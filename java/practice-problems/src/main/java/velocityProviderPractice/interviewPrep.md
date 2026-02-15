# VelocityProvider Code Review — Interview Prep

## What They'll Do

They'll open your code, walk through it with you, and:
- Challenge your design decisions
- Point out flaws and see how you respond
- Ask "what if" scenarios to test your thinking
- See if you can acknowledge mistakes and improve on the spot

**Golden rule: Don't be defensive. Acknowledge issues. Propose fixes.**

---

## Section 1: Where They'll Grill You — Honest Weaknesses

### Q: "Your synchronized blocks — isn't this a performance bottleneck?"

**They're right. Acknowledge it:**
```
"Yes, you're right. The synchronized blocks on per-card TreeMaps 
mean that concurrent queries and writes for the SAME card are 
serialized. For a card being checked hundreds of times per second 
during a fraud attack, this becomes a bottleneck.

I could improve this with:
1. ReadWriteLock per card — multiple concurrent reads, 
   exclusive writes
2. ConcurrentSkipListMap instead of TreeMap — lock-free, 
   supports subMap
3. Move to Redis sorted sets — offload concurrency to Redis

ConcurrentSkipListMap is probably the best fix — drop-in 
replacement, no synchronized blocks needed."
```

### Q: "Your oldestTimestampMillis is volatile but you have a race condition. Two threads could both update it."

**They're right. Acknowledge it:**
```
"You're correct. Two threads could both read oldestTimestampMillis, 
both see their value is smaller, and both write — last one wins. 
We could lose track of the true oldest.

I should use AtomicLong with a CAS loop:

AtomicLong oldestTimestamp = new AtomicLong(Long.MAX_VALUE);

// In addTimestamp:
oldestTimestamp.updateAndGet(current -> 
    Math.min(current, epochMilli));

This is atomic — no race condition. I took a shortcut with 
volatile because the worst case is an unnecessary cleanup pass, 
which is harmless but not ideal."
```

### Q: "Your cleanup iterates ALL cards. That's O(m * k). Won't that cause latency spikes?"

**They're right. Acknowledge it:**
```
"Yes. Even with probabilistic triggering, when cleanup DOES run, 
it touches every card. With millions of cards, that's expensive 
and it happens on a real request — that user sees high latency.

Better approaches:
1. Background thread — cleanup runs async, never blocks requests
2. Per-card TTL — each card tracks its own expiry, cleanup is local
3. Sharded cleanup — only clean a subset of cards each time
4. Move to Redis with EXPIRE — Redis handles TTL natively

I should have used a CompletableFuture.runAsync() for cleanup 
so it never blocks the request path."
```

### Q: "Why is removeOlderThan synchronized on the entire method? That locks ALL cards."

**They're right. Acknowledge it:**
```
"That's a mistake. The method-level synchronized means no other 
cleanup can run simultaneously, which is fine. But it also means 
the method holds the monitor on 'this' (the storage instance) 
while iterating all cards.

The per-card synchronized blocks inside are what actually protect 
the data. The outer synchronized is overly broad — it was 
unnecessary caution on my part.

I should remove the method-level synchronized and rely only on 
the per-card locks. Or better, use ConcurrentSkipListMap and 
remove all synchronized blocks."
```

### Q: "computeIfAbsent with ConcurrentHashMap — the returned TreeMap isn't thread-safe. Isn't that fragile?"

**Acknowledge it:**
```
"Yes. computeIfAbsent on ConcurrentHashMap IS atomic — only one 
thread creates the TreeMap. But the returned TreeMap is a regular 
TreeMap, not thread-safe.

My design relies on every caller remembering to wrap access in 
synchronized(timestamps). If someone adds a new method and forgets 
the sync block, we get a race condition. That's fragile.

ConcurrentSkipListMap would eliminate this entire class of bugs — 
thread-safe by default, no manual synchronization needed."
```

### Q: "Your probabilistic cleanup — what guarantees do you have that it actually runs?"

**Be honest:**
```
"No guarantees. With 0.1% probability, statistically it runs 
once every ~1000 registrations. But it's random — it could 
theoretically not run for 10,000 registrations. Memory grows 
unchecked during that period.

I should add a fallback:
- Track time since last cleanup
- If it's been more than X minutes, force cleanup
- Or add a size-based trigger — if entries exceed threshold, 
  force cleanup

The probabilistic approach avoids latency spikes, but it trades 
off guaranteed memory bounds. I should have added a safety net."
```

### Q: "You're storing Long keys and Integer values in TreeMap. That's a lot of object overhead. Why not a simpler structure?"

**Think honestly:**
```
"Fair point. A sorted ArrayList<Long> with binary search gives 
O(log n) lookup too, with better cache locality and less memory 
overhead than TreeMap nodes.

I chose TreeMap because:
- subMap() is built-in
- headMap().clear() makes cleanup easy
- merge() handles duplicates

But if duplicate timestamps are rare, a sorted list with 
binary search would use less memory and be faster due to 
cache locality. At extreme scale, that could matter.

Honestly, I optimized for code readability over raw performance 
here."
```

### Q: "Your validation throws RuntimeExceptions. Is that appropriate for a payment system?"

**Acknowledge the nuance:**
```
"Crashing might be worse than degraded behavior in a payment 
system. IllegalArgumentException is unchecked — the caller 
might not catch it.

For registerPayment, throwing on null makes sense — you can't 
register nothing. But for getCardUsageCount, maybe returning 0 
with a warning log is safer than crashing the entire risk 
assessment.

I should differentiate:
- Null payment → throw (programming error, fix the caller)
- Invalid duration → return 0 + log warning (graceful degradation)

Or use a Result<T> object instead of exceptions."
```

### Q: "This is all in one file. Why?"

**Be honest:**
```
"HackerRank constraint — everything had to be in one class. 
In production I'd split into proper packages:

velocityProvider/
├── VelocityProvider.java (interface)
├── VelocityProviderImpl.java
├── config/
│   └── VelocityProviderConfig.java
├── storage/
│   ├── TimestampStorage.java (interface)
│   └── TreeMapTimestampStorage.java
├── cleanup/
│   ├── CleanupStrategy.java (interface)
│   └── ProbabilisticCleanupStrategy.java
└── model/
    └── Payment.java"
```

### Q: "The cleanup threshold is 7 days but this is in-memory. Isn't 7 days of data too much for memory?"

**They're right:**
```
"Yes. 7 days is too aggressive for in-memory storage. 
At 1M payments/day, that's 7M entries in memory.

I set 7 days as a safe default so cleanup wouldn't interfere 
with reasonable query durations. But I should have:
1. Defaulted to 24 hours for in-memory
2. Made 7 days the default only for distributed backends
3. Added a warning if estimated memory exceeds a threshold

The config is tunable via Builder, but the default should have 
been more conservative."
```

### Q: "What if query duration exceeds cleanup threshold? You'll return wrong results silently."

**This is a bug. Own it:**
```
"That's a bug. I should validate:

if (duration.compareTo(config.getCleanupThreshold()) > 0) {
    throw new IllegalArgumentException(
        'Duration exceeds cleanup threshold');
}

Or log a warning and return the best result we have.
Either way, silently returning wrong data is the worst outcome 
for a fraud detection system. Good catch — I missed this."
```

---

## Section 2: "What If" Scenarios

### Q: "What if two payments arrive at the exact same millisecond?"

```
Handled by merge():
  timestamps.merge(epochMilli, 1, Integer::sum);

First payment at ms 1000:  {1000: 1}
Second payment at ms 1000: {1000: 2}
countInWindow sums values → returns 2 ✓
```

### Q: "What if 10 million payments per day?"

```
7-day threshold: 70M entries × ~64 bytes = ~4.5 GB → too much

Fix:
1. Reduce threshold to 24 hours → ~640 MB manageable
2. Move to Redis sorted sets for distributed storage
3. Shard by card hash across multiple instances

The default 7-day threshold was too generous for in-memory.
```

### Q: "What if payments arrive out of order?"

```
TreeMap handles this naturally — sorted by key, not insertion order.
Insert 1000, then insert 900 → TreeMap = {900:1, 1000:1}
subMap(800, 1000) returns both ✓
```

### Q: "What if a card has never been seen?"

```
cardTimestamps.get(cardHash) returns null.
My code checks:

if (timestamps == null || timestamps.isEmpty()) {
    return 0;
}

Returns 0 — safe. ✓
```

### Q: "What if the system crashes?"

```
All data lost — it's in-memory. On restart:
- Velocity data rebuilds from incoming payments
- Brief window where fraud detection is weaker
- Acceptable trade-off for in-memory speed
- For production, Redis or write-ahead log for persistence
```

### Q: "What if we need multiple velocity rules?"

```
Already supported. Caller calls getCardUsageCount multiple times:

int perMin = provider.getCardUsageCount(payment, Duration.ofMinutes(1));
int perHour = provider.getCardUsageCount(payment, Duration.ofHours(1));

VelocityProvider stores raw timestamps — rules are the 
caller's responsibility. Separation of concerns.
```

### Q: "What if we need this across multiple servers?"

```
Swap storage to Redis — same interface:

ZADD velocity:cardHash timestamp timestamp  → register
ZCOUNT velocity:cardHash min max            → count
ZREMRANGEBYSCORE → cleanup

Zero changes to VelocityProviderImpl. Strategy pattern pays off.
```

### Q: "What if a merchant wants different rules than another?"

```
Add merchantId to Payment. 
Composite key: "merchantId:cardHash" for storage.
Per-merchant config via Map<String, VelocityProviderConfig>.
```

---

## Section 3: Code Walkthrough — Key Lines

### computeIfAbsent
```java
cardTimestamps.computeIfAbsent(cardHash, k -> new TreeMap<>());
// Atomic get-or-create. Only one thread creates TreeMap for new card.
// Weakness: returned TreeMap isn't thread-safe — need sync blocks.
```

### merge
```java
timestamps.merge(epochMilli, 1, Integer::sum);
// Absent → insert 1. Exists → add 1 to count.
// Handles duplicate timestamps correctly.
```

### subMap
```java
timestamps.subMap(windowStartMillis, true, queryTimeMillis, true)
    .values().stream().mapToInt(Integer::intValue).sum();
// VIEW of entries in range — no copying. O(log n) boundaries.
```

### volatile
```java
private volatile Long oldestTimestampMillis;
// Visibility across threads without full sync.
// Has race condition — should be AtomicLong. Acknowledged.
```

### headMap.clear()
```java
timestamps.headMap(cutoffMillis, false).clear();
// View of entries less than cutoff. Removes from original.
// Efficient bulk delete.
```

---

## Section 4: Complexity

```
registerPayment:     O(log n) per card
getCardUsageCount:   O(log n + k) per card, k = results in window
cleanup (triggered): O(m * k) where m = cards, k = old entries per card
                     Amortized O(1) per registration due to probability

Space: O(m * n)
  m = unique cards, n = avg timestamps per card
  Bounded by cleanup threshold
```

---

## Section 5: Patterns in Your Code

```
Strategy Pattern     → TimestampStorage, CleanupStrategy
Builder Pattern      → VelocityProviderConfig.Builder
Factory Method       → VelocityProvider.getProvider()
```

**If they ask about SOLID:**
```
S — VelocityProviderImpl orchestrates, doesn't implement storage/cleanup
O — New storage? Implement interface. No existing code changes.
L — Any TimestampStorage works in VelocityProviderImpl
I — Small focused interfaces, not one giant interface
D — Depends on interfaces, not concrete classes
```

---

## Section 6: What You'd Improve — Say This Proactively

```
"Looking at this code again, three things I'd change:

1. ConcurrentSkipListMap instead of TreeMap + synchronized
   → Eliminates all manual synchronization bugs

2. Async cleanup with CompletableFuture.runAsync()
   → Cleanup never blocks the request path

3. Validate duration vs cleanup threshold
   → Prevents silently wrong results

And honestly, the 7-day default cleanup threshold is too high 
for in-memory. I'd change it to 24 hours."
```

---

## Section 7: How to Handle Being Grilled

```
WHEN THEY FIND A FLAW:
  ✅ "You're right, that is a problem. Here's how I'd fix it..."
  ✅ "Good catch. I missed that. I'd add..."
  ✅ "That was a trade-off I made for time. In production I'd..."
  ❌ "No, that's fine because..." (defensive)
  ❌ "That wouldn't happen in practice" (dismissive)
  ❌ Long silence (frozen)

WHEN YOU DON'T KNOW:
  ✅ "I'm not sure. Let me think through it..."
  ✅ "I'd need to research that, but my instinct is..."
  ❌ Making something up

WHEN THEY SUGGEST A BETTER APPROACH:
  ✅ "That's better. The trade-off is... and I think yours wins."
  ✅ "I hadn't considered that. It solves the X problem I had."
  ❌ "My way works too" (defensive)
```

---

## Section 8: Questions to Ask THEM

```
1. "What does the current documentation infrastructure look like?"
2. "What's the biggest technical challenge the team faces right now?"
3. "How does the team handle on-call and production incidents?"
4. "What does success look like in the first 90 days?"
5. "What's the team size and how are decisions made?"
```

---

## Final Checklist

- [ ] Can acknowledge every weakness honestly
- [ ] Can propose fixes on the spot when challenged
- [ ] Can explain every line if asked
- [ ] Can discuss scaling honestly (what breaks and when)
- [ ] Can name patterns and WHY
- [ ] Practiced saying "you're right, here's how I'd fix it"
- [ ] Have 5 questions ready for them