# Adyen Interview Prep — VelocityProvider Solution Walkthrough

**February 16, 2026**

---

## 1. Architecture Walkthrough

### Your Elevator Pitch

> "I built a fraud detection velocity checker using the Strategy pattern. The core is a TreeMap-based storage that gives O(log n + k) time complexity for range queries. I separated storage, cleanup, and configuration into pluggable interfaces so each can be swapped independently. The system is thread-safe using ReadWriteLock per card for fine-grained concurrency."

### Why TreeMap?

**Q: Why did you choose TreeMap over ArrayList or HashMap?**

TreeMap is backed by a red-black tree, which is self-balancing. This means timestamps are always ordered on insert at O(log n) cost. I leverage the `subMap()` operation for range queries, which gives O(log n) to find the start of the range plus O(k) to iterate results.

- **ArrayList**: O(log n) search via binary search but **O(n) insert** due to element shifting. The shift cost dominates.
- **HashMap**: No ordering at all, so range queries require scanning everything at O(n).

### Why TreeMap over TreeSet?

**Q: Your architecture doc mentions TreeSet but your code uses TreeMap. Why?**

TreeSet cannot handle duplicate timestamps. If two payments for the same card arrive at the exact same millisecond, TreeSet would only store one. `TreeMap<Long, Integer>` maps each timestamp to a count, so duplicates are handled correctly via `merge(epochMilli, 1, Integer::sum)`.

---

## 2. Thread Safety & Concurrency

*This is the area they will probe deepest. Know these cold.*

### Your Concurrency Approach

**Q: Walk me through your concurrency approach. Why ConcurrentHashMap + ReadWriteLock?**

ConcurrentHashMap handles thread-safe access to the outer map (card hash → TreeMap). For the inner TreeMap per card, I use a ReadWriteLock because payment processing is **read-heavy** — many fraud checks (reads) happen per card vs. fewer payment registrations (writes). ReadWriteLock allows multiple concurrent reads while ensuring writes are exclusive. I lock at the **per-card level**, not globally, so different cards never block each other.

### ReadWriteLock vs synchronized vs ConcurrentSkipListMap

| Approach | Pros | Cons | Best For |
|----------|------|------|----------|
| `synchronized` (original) | Simple, strongly consistent | All access serialized, even reads block reads | Simple cases, low contention |
| **ReadWriteLock** (current) | Concurrent reads, exclusive writes, strongly consistent | Slightly more complex, write starvation possible | **Read-heavy fraud detection** |
| ConcurrentSkipListMap | Lock-free, great concurrency | Weakly consistent reads (subMap may miss concurrent writes) | Analytics, approximate counts |
| StampedLock (optimistic) | Fastest reads (no locking on optimistic path) | Complex to implement correctly | Extreme read-heavy workloads |

### ReadWriteLock Implementation

```java
private final ConcurrentHashMap<String, ReadWriteLock> cardLocks = new ConcurrentHashMap<>();

private ReadWriteLock getLock(String cardHash) {
    return cardLocks.computeIfAbsent(cardHash, k -> new ReentrantReadWriteLock());
}

// WRITE — exclusive access
public void addTimestamp(String cardHash, Instant timestamp) {
    TreeMap<Long, Integer> timestamps = cardTimestamps.computeIfAbsent(cardHash, k -> new TreeMap<>());
    ReadWriteLock lock = getLock(cardHash);
    lock.writeLock().lock();
    try {
        timestamps.merge(timestamp.toEpochMilli(), 1, Integer::sum);
    } finally {
        lock.writeLock().unlock();
    }
}

// READ — multiple threads can hold this simultaneously
public int countInWindow(String cardHash, Instant queryTime, Duration duration) {
    // ...
    ReadWriteLock lock = getLock(cardHash);
    lock.readLock().lock();
    try {
        return timestamps.subMap(start, true, end, true).values().stream()
            .mapToInt(Integer::intValue).sum();
    } finally {
        lock.readLock().unlock();
    }
}
```

### The Hot Key Problem

**Q: What happens when multiple threads access the same card simultaneously?**

With ReadWriteLock, concurrent fraud checks (reads) proceed in parallel. Only when a payment is registered (write) do reads block — and only for that specific card. This is much better than `synchronized`, where even two reads for the same card would block each other.

### Why NOT ConcurrentSkipListMap for Fraud Detection?

**Q: You have a SkipList implementation. Why not use it for fraud detection?**

ConcurrentSkipListMap's `subMap` returns a **weakly consistent** view. If a payment is registered while we're counting, we might miss it. In fraud detection, if we return 4 instead of 5 and the rule triggers at 5, **we let a fraudulent payment through**. Correctness matters more than throughput here.

**Use SkipList for**: analytics, reporting, dashboards — where being off by one momentarily doesn't cause real harm.

### Race Condition: oldestTimestampMillis

**Q: Your oldestTimestampMillis update isn't atomic. What's the race condition?**

Two threads could both read the current value, both pass the `<` check, and one write gets lost. However, this is a **benign race** — `oldestTimestampMillis` is only an optimization hint for cleanup to skip early. The worst case is an unnecessary cleanup scan, not incorrect data. To fix it properly: use `AtomicLong` with `accumulateAndGet(epochMilli, Math::min)` — which is exactly what I do in the SkipList implementation.

---

## 3. Cleanup Strategy

### Why Probabilistic Cleanup?

**Q: Why probabilistic? What if cleanup doesn't trigger for a long time?**

Probabilistic cleanup **amortizes the cost** across many requests. Instead of one request bearing the full cleanup cost or needing a background scheduler, each registration has a 0.1% chance of triggering cleanup.

- On **average**, cleanup runs once every 1000 registrations
- Probability of going 2000 without cleanup: 0.999^2000 ≈ 13% — very unlikely
- When it does trigger, it clears everything older than the threshold — **self-correcting**

*"I used probabilistic cleanup in the HackerRank because it was self-contained with no external dependencies. In production, I'd use a scheduled background service for predictability and zero impact on payment latency."*

### Production Alternative: Background Service

In production, a scheduled background service is better because:

- **Predictable timing** — runs on a fixed schedule (e.g., every 5 minutes)
- **No impact on payment latency** — runs on a separate thread
- **Easy to monitor** and alert on
- Can run during **low-traffic periods**

```java
ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
scheduler.scheduleAtFixedRate(
    () -> storage.removeOlderThan(Instant.now().minus(threshold)),
    0, 5, TimeUnit.MINUTES
);
```

### Cleanup Threshold vs Query Duration

**Q: What happens if the cleanup threshold is smaller than the query duration?**

Data gets deleted before it can be queried — you'd get incorrect (lower) counts. The cleanup threshold **MUST be >= the maximum expected query duration**. Default of 7 days ensures most reasonable velocity rules work. If someone needs "card seen in last 30 days", threshold must be ≥ 30 days.

---

## 4. Design Patterns Used

| Pattern | Where Used | Why |
|---------|-----------|-----|
| **Strategy** | TimestampStorage, CleanupStrategy interfaces | Swap implementations without changing core logic |
| **Builder** | VelocityProviderConfig | Flexible, readable config with validation and defaults |
| **Factory Method** | VelocityProvider.getProvider() | Hides implementation details from callers |
| **Interface Segregation** | Separate Storage and Cleanup interfaces | Each interface has a single, focused responsibility |

---

## 5. API Design Improvement

**Q: getCardUsageCount takes a full Payment object but only needs the card hash and timestamp. What would you change?**

The current API forces callers to create a dummy Payment with a fake paymentId just to query a count (visible in the `main` method with `UUID.randomUUID()`). That's a code smell. Fix: add an overloaded method keeping the original for backward compatibility:

```java
int getCardUsageCount(String hashedCardNumber, Instant queryTime, Duration duration);
```

---

## 6. System Design: Scaling Beyond One JVM

### Approach 1: Shard by Card Hash (Simplest)

Load balancer routes based on `hash(cardNumber) % numServers`. All payments for the same card always hit the same server. Each server keeps its own in-memory TreeMap.

- **Pro**: Simple, fast, no shared state
- **Con**: Hot cards still hit one server; server failure loses data

### Approach 2: Redis Sorted Sets (Medium Scale)

All servers read/write to a central Redis instance. Redis sorted sets = your TreeMap hosted externally:

- `ZADD card:ABC123 <timestamp> "txn1"` → same as treeMap.put()
- `ZCOUNT card:ABC123 <start> <end>` → gives count directly
- `ZRANGEBYSCORE` for range queries

Redis handles **100K+ ops/sec** on a single key. App servers become stateless. Tradeoff: sub-millisecond network latency vs nanosecond in-memory.

### Approach 3: Redis Cluster (Large Scale)

- **Redis Cluster** — auto-shards across multiple Redis nodes
- **Read replicas** — writes to primary, reads to replicas (fraud checking is read-heavy)

### Scaling Hierarchy Summary

| Scale | Solution | Tradeoff |
|-------|----------|----------|
| Single server | In-memory TreeMap + ReadWriteLock | Fast but limited by JVM memory |
| Medium (10s of servers) | Redis sorted sets | Sub-ms latency, 100K+ ops/sec |
| Large (100s of servers) | Redis Cluster + read replicas | More infra complexity |
| Global (multi-region) | Aurora/DynamoDB with strong reads | Higher latency, highest consistency |

---

## 7. Production System Questions

### Monitoring & Observability

**Q: What metrics would you track in production?**

- **p50, p95, p99 latency** for both `getCardUsageCount` and `registerPayment` — if p99 spikes, it could mean lock contention on hot cards or cleanup running too long
- **Cleanup duration and frequency** — how long each cleanup takes, when it last ran. If cleanup takes too long, it holds write locks and blocks payments
- **Memory usage** — heap size, number of cards in the map, timestamps per card. If memory grows unexpectedly, cleanup might not be triggering
- **Card count per window** — track how many cards are hitting high velocity thresholds. A sudden spike could mean a fraud attack or a bug
- **False positive rate** — cards incorrectly flagged as fraud. Business metric but directly tied to the system's correctness

```java
// Example: structured logging for slow operations
long startNanos = System.nanoTime();
int count = timestampStorage.countInWindow(cardHash, queryTime, duration);
long durationNanos = System.nanoTime() - startNanos;

if (durationNanos > SLOW_THRESHOLD_NANOS) {
    logger.warn("Slow query: {}ms for card prefix {}",
        durationNanos / 1_000_000, cardHash.substring(0, 4));
}
```

### Failover & Resilience

**Q: What happens when a server crashes? How do you recover state?**

With in-memory storage, all data is lost on crash. Mitigation strategies:

- **Write-ahead log (WAL)**: Before adding to the TreeMap, append the payment to a local log file. On restart, replay the log to rebuild state. This is how databases like Postgres handle it.
- **Redis as primary store**: If using Redis sorted sets, state survives app server crashes. Redis itself has persistence (RDB snapshots + AOF log).
- **Graceful degradation**: If state is lost, the velocity checker returns 0 for all cards until enough data accumulates again. This means temporarily reduced fraud detection — not a system outage. You can mitigate by having a "warm-up period" flag that triggers stricter rules.
- **Replication**: In a multi-server setup, replicate payment registrations to a standby. On failover, the standby takes over with minimal data loss.

**Q: What about network partitions in a distributed setup?**

This is a CAP theorem question. For fraud detection, we choose **CP (consistency over availability)** — it's better to temporarily block a payment than to let a fraudulent one through. If we can't verify the velocity count, we should err on the side of caution and either reject or queue the payment for manual review.

### Out-of-Order Payments

**Q: What if payments arrive with timestamps out of order?**

This is actually handled naturally by TreeMap — it sorts by key (timestamp), so insertion order doesn't matter. `subMap()` always returns the correct range regardless of insertion order.

However, there are edge cases:

- **Late-arriving payment**: A payment from 5 minutes ago arrives now. It gets inserted correctly into the TreeMap, but any fraud checks that already ran in the last 5 minutes missed it. This is a **fundamental limitation** of any real-time system.
- **Cleanup race**: If a very old payment arrives after cleanup has already removed that time window, it gets inserted but is immediately eligible for cleanup. Not harmful, just wastes a tiny bit of work.
- **Clock skew across servers**: In a distributed setup, different servers may have slightly different clocks. Solution: use a centralized timestamp source (e.g., the payment gateway's timestamp) rather than `System.currentTimeMillis()`.

### Memory Pressure

**Q: What if the heap runs out before cleanup triggers?**

With probabilistic cleanup, this is theoretically possible under extreme load:

- **Prevention**: Set cleanup probability higher for high-traffic systems. At 1M payments/day, use 1% (triggers ~every 100 payments) instead of 0.1%.
- **Backpressure**: Monitor heap usage. If it exceeds a threshold (e.g., 80%), force an immediate cleanup regardless of probability.
- **Bounded data structures**: Set a hard cap on timestamps per card. If a card exceeds N timestamps, remove the oldest before inserting. This bounds memory per card.
- **JVM tuning**: Size the heap based on expected data volume. Formula: `heap >= numCards × avgTimestampsPerCard × ~50 bytes per entry × safety margin`.

```java
// Example: forced cleanup on memory pressure
Runtime runtime = Runtime.getRuntime();
double usedRatio = (double)(runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory();
if (usedRatio > 0.8) {
    cleanupStrategy.cleanup(timestampStorage, Instant.now());
}
```

### Testing Strategy

**Q: How would you test thread safety?**

- **DeterministicCleanupStrategy**: Already in the code — cleans every N registrations instead of randomly. Makes tests predictable and repeatable.
- **Injectable Random for cleanup**: The `ProbabilisticCleanupStrategy` accepts a `Random` in its constructor. In tests, pass a seeded `Random` for deterministic behavior.
- **Concurrent test with CountDownLatch**: Spawn N threads, have them all wait on a latch, release simultaneously to hammer the same card, then verify the count is exactly N.

```java
@Test
void testConcurrentRegistrations() throws Exception {
    int threadCount = 100;
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(threadCount);
    Instant now = Instant.now();
    String card = "CARD_ABC";

    for (int i = 0; i < threadCount; i++) {
        new Thread(() -> {
            startLatch.await();
            provider.registerPayment(new Payment(UUID.randomUUID().toString(), now, card));
            doneLatch.countDown();
        }).start();
    }

    startLatch.countDown(); // Release all threads
    doneLatch.await();      // Wait for all to finish

    assertEquals(threadCount, provider.getCardUsageCount(
        new Payment("q", now, card), Duration.ofMinutes(1)));
}
```

- **Stress tests**: Run millions of operations and verify no `ConcurrentModificationException`, no deadlocks, and counts are always accurate.

### Deployment & Configuration

**Q: How would you configure this for different traffic levels?**

| Traffic Level | Cleanup Probability | Cleanup Threshold | Storage | Notes |
|--------------|-------------------|------------------|---------|-------|
| High (>1M payments/day) | 1% (0.01) | 6–12 hours | Redis sorted sets | Or use background scheduler |
| Medium (100K–1M/day) | 0.1% (0.001) | 24 hours | In-memory TreeMap | Default config works |
| Low (<100K/day) | 0.01% (0.0001) | 7 days | In-memory TreeMap | Cleanup rarely needed |

**Q: How would you do a zero-downtime deployment?**

- **Rolling deployment**: Update one server at a time. Other servers continue serving. If using sharding by card hash, temporarily re-route the updating server's cards to others.
- **Blue-green**: Run two identical environments. Switch traffic from blue to green after green is verified healthy. State in Redis persists across deployments.
- **Feature flags**: Toggle between TreeMap and SkipList storage via config (already in your code with `useSkipListStorage`). Roll out gradually to a percentage of traffic.

---

## 8. Quick Reference: Key Complexity

| Operation | Time Complexity | Notes |
|-----------|----------------|-------|
| registerPayment | O(log n) | TreeMap insertion per card |
| getCardUsageCount | O(log n + k) | Binary search + k results in window |
| cleanup (probabilistic) | Amortized O(1) per registration | Full scan O(m × n) when triggered |
| ArrayList insert (comparison) | O(n) | Binary search O(log n) + shift O(n) |
| HashMap range query (comparison) | O(n) | Must scan all entries |

*n = timestamps per card, k = results in window, m = unique cards*

---

## 9. Interview Delivery Tips

- **Think out loud** — they want to see your reasoning process, not just the answer
- **Start broad, then go deep** — give the high-level answer first, then details
- **Acknowledge tradeoffs** — every design choice has pros and cons; name them
- **Tie to real impact** — "missing one count could let fraud through" is stronger than "it's not consistent"
- **If you don't know, reason through it** — never say "you tell me"
- **For concurrency questions**, always describe: what's shared, what's the lock scope, what's the race condition
