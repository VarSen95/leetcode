# Adyen Round 2 — Complete HackerRank Review Prep

## What the Recruiter Said

> "The upcoming interview will involve talking through your solutions in more depth, your approach and if they can be optimized in any way (when it comes to thread safetiness, concurrency etc). If they have time left, they will go into some system design work with you."

## Your HackerRank Submissions

1. **VelocityProvider** — Fraud detection velocity checker (S1)
2. **Banking System** — OOP banking with accounts & transactions (S2)
3. **SQL Queries** — Job execution status queries
4. **Valid Parentheses** — Stack-based bracket matching
5. **Possibly more sub-questions** (S1: 1-2, S2: 3-5 from screenshot)

---

# SECTION A: VelocityProvider

## 3-Minute Walkthrough

```
""The problem asks for a system that tracks how many times a card 
was used in a time window — for fraud detection. Adyen processes 
millions of payments daily, so performance and accuracy were 
my top priorities.

I separated the system into three concerns:

1. VelocityProviderImpl — the orchestrator
   Validates input, delegates to storage and cleanup.
   Doesn't know HOW data is stored or cleaned.

2. TreeMapTimestampStorage — the data layer
   ConcurrentHashMap of cardHash → TreeMap<epochMillis, count>.
   I chose TreeMap because I needed efficient range queries —
   'count all timestamps between (now - 10min) and now.'
   subMap() does this in O(log n). I used TreeMap<Long, Integer> 
   instead of TreeSet<Instant> to handle duplicate timestamps — 
   two payments at the same millisecond count as 2, not 1.

3. ProbabilisticCleanupStrategy — memory management
   Without cleanup, memory grows forever. I trigger cleanup 
   on 0.1% of registrations — spreads the cost so no single 
   payment request bears all the cleanup overhead.

I designed around interfaces — TimestampStorage and 
CleanupStrategy are both swappable. Today it's in-memory 
for the assessment. For production at Adyen's scale, I'd put 
PostgreSQL as the system of record — strongly consistent, 
durable, indexed on (card_hash, timestamp) for range queries. 
Redis sorted sets in front as a real-time query layer for 
sub-millisecond velocity checks. Write to both, read from 
Redis, fall back to Postgres. Zero changes to the orchestrator — 
just a new TimestampStorage implementation.

There are concurrency weaknesses I'd fix — I can walk through 
those if you'd like."
"
```

## Thread Safety Questions

### "Is your VelocityProvider thread-safe?"

```
"Mostly. Known weaknesses:

✅ ConcurrentHashMap — different cards never block each other
✅ synchronized(timestamps) — per-card TreeMap operations are safe
❌ volatile oldestTimestampMillis — race condition on read-then-write
   Fix: AtomicLong.updateAndGet(current -> Math.min(current, epochMilli))
   example: 
        AtomicLong counter = new AtomicLong(5);
        thread A: get() --> reads 5
        thread B: get() --> reads 5
        thread A: compareAndSet(5, 6) --> value is 5, SUCCESS, now 6
        thread B: compareAndSet(5, 6) --> value is 6, FAILURE
        thread B: retries --> get() --> reads 6
        thread B: compareAndSet(6, 7) --> value is 6, SUCCESS, now 7
   Fix: ConcurrentSkipListMap — drop-in replacement, lock-free
    For hot keys, the biggest win isn't even about writes — 
    it's that reads are completely lock-free. In a velocity 
    system, most operations are checks, not registrations. 
    With synchronized TreeMap, even readers block. With 
    ConcurrentSkipListMap, all readers run in parallel, 
    and writers only do cheap CAS retries instead of 
    expensive thread sleeps. For a hot stolen card getting 
    hammered, that's the difference between 999 threads 
    sleeping and 900 threads running freely.

    I'd use a ReadWriteLock per card. Payment processing is read-heavy — we're doing many fraud checks per card compared to actual payment registrations. With ReadWriteLock, all those concurrent fraud checks can read in parallel without blocking each other. Only when a new payment is registered do we take the write lock, which briefly blocks reads for just that one card. This gives us strong consistency because writes are exclusive, good concurrency because reads don't block each other, and fine-grained locking because cards don't interfere with each other.
❌ Cleanup blocks request path
   Fix: CompletableFuture.runAsync()"
```

### "Two threads, same card — what happens?"

```
Both call computeIfAbsent → ConcurrentHashMap creates ONE TreeMap
Both try merge() → synchronized(timestamps) serializes them
Thread A: acquires lock → merge → releases
Thread B: was waiting → acquires → merge → releases
Data consistent ✓ but serialized for same card
```

### "How would you optimize?"

```
1. ConcurrentSkipListMap — eliminates all synchronized blocks
2. ReadWriteLock — multiple concurrent reads, exclusive writes
3. Async cleanup — CompletableFuture.runAsync()
4. Validate duration vs cleanup threshold — prevents silent wrong results
```

### Bugs to Own

```
1. volatile race condition → AtomicLong
2. removeOlderThan synchronized on entire method → too broad
3. No validation: duration > cleanup threshold → silent wrong results
4. 7-day default too high for in-memory → should be 24 hours
5. Probabilistic cleanup has no guarantee it runs → add size-based fallback
```

---

# SECTION B: Banking System

## 2-Minute Walkthrough

```
"Classic OOP banking system with:
- Abstract AccountHolder → Person and Company extend it
- Abstract Account → ConsumerAccount and CommercialAccount extend it
- Bank manages accounts in a ConcurrentHashMap
- Transaction authenticates once at construction, then delegates

Key design decisions:
- ConcurrentHashMap for thread-safe account storage
- AtomicLong for unique account number generation
- synchronized on balance operations (credit/debit/getBalance)
- Interfaces (BankInterface, TransactionInterface, AccountInterface) 
  for clean contracts"
```

## OOP Questions They'll Ask

### "Why abstract class for Account instead of interface?"

```
"Account has STATE — balance, pin, accountNumber, accountHolder.
Interfaces can't have state (pre-Java 8) and even with default 
methods, shared mutable state belongs in abstract classes.

AccountHolder is also abstract because Person and Company share 
the idNumber field.

The INTERFACES (BankInterface, AccountInterface) define the 
public contract. The ABSTRACT CLASSES hold shared state and 
partial implementation. Different purposes."
```

### "Why abstract class for AccountHolder instead of interface?"

```
"AccountHolder has a private idNumber field and getIdNumber() 
implementation shared by both Person and Company. An interface 
can't hold instance state.

If AccountHolder had no shared state — just method signatures — 
I'd use an interface instead."
```

### "Walk me through the inheritance hierarchy."

```
AccountHolder (abstract)
  ├── Person (firstName, lastName)
  └── Company (companyName)

Account (abstract) — has AccountHolder, balance, pin
  ├── ConsumerAccount (simple, no extras)
  └── CommercialAccount (has authorizedUsers list)

Bank implements BankInterface
Transaction implements TransactionInterface
```

### "Why does CommercialAccount have synchronized on addAuthorizedUser and isAuthorizedUser?"

```
"The authorizedUsers list is an ArrayList — not thread-safe.
If two threads add users simultaneously, the internal array 
could corrupt. synchronized ensures only one thread modifies 
the list at a time.

Improvement: Use CopyOnWriteArrayList instead — thread-safe 
for read-heavy workloads. Most operations are isAuthorizedUser 
(reads), rare addAuthorizedUser (writes). Perfect fit."
```

## Thread Safety Questions on Banking

### "Is the Banking system thread-safe?"

```
"Partially:

✅ ConcurrentHashMap for accounts map — safe concurrent access
✅ AtomicLong for account numbers — atomic increment
✅ synchronized on getBalance/creditAccount/debitAccount — 
   per-account locking via 'this'
✅ synchronized on CommercialAccount authorized users

❌ Bank.credit/debit has a TOCTOU race:
   Account account = getAccount(accountNumber);  // could return non-null
   // another thread could theoretically remove the account here
   account.creditAccount(amount);                 // NPE if removed
   In practice, accounts aren't removed, so it's fine.
   But strictly speaking, it's a race condition.

❌ Transfer is NOT atomic:
   sourceAccount.withdraw(amount);  // succeeds
   targetAccount.deposit(amount);   // if this fails, money vanishes
   
   Fix: Lock both accounts, or use a transaction log"
```

### "The transfer method — what if it fails halfway?"

**This is a critical question. Be honest:**

```
"My transfer in the selfpractice Bank.java does:
  sourceAccount.withdraw(amount);
  targetAccount.deposit(amount);

If withdraw succeeds but deposit fails (shouldn't with valid 
amount, but in theory), money disappears. Not atomic.

Fix options:
1. Lock both accounts in consistent order (by account number)
   to prevent deadlock, do both operations, rollback on failure

2. Use a transaction log — write intent first, execute, 
   mark complete. On failure, compensating transaction.

3. In the HackerRank Banking system, the Transaction class 
   delegates to Bank — same issue. For a real banking system, 
   you'd use database transactions with ACID guarantees."
```

### "Why AtomicLong instead of a simple counter?"

```
"Multiple threads could call openConsumerAccount simultaneously.
A regular long++ is NOT atomic — two threads read the same value, 
both increment, both write — you get duplicate account numbers.

AtomicLong.getAndIncrement() is atomic — uses CAS internally.
Guaranteed unique account numbers within a single JVM.

For multi-JVM: UUID, Snowflake ID, or database sequence."
```

### "Account.getBalance is synchronized. Is that necessary?"

```
"For a double field — yes. In Java, double reads/writes are 
NOT guaranteed atomic on 32-bit JVMs (doubles are 64 bits, 
could be read in two 32-bit operations).

On 64-bit JVMs it's practically atomic, but the JLS doesn't 
guarantee it. synchronized ensures:
1. Atomicity of the read
2. Visibility — you see the latest balance after a credit/debit

Alternative: Make balance a volatile field. Reads become 
visible without synchronized. But compound operations like 
debitAccount (read balance, check, subtract) still need 
synchronized for atomicity."
```

### "What design patterns do you see in the Banking system?"

```
1. Template Method (implicit)
   — Account is abstract, subclasses provide specific behavior
   — CommercialAccount adds authorizedUsers

2. Factory-ish
   — Bank.openConsumerAccount / openCommercialAccount
   — Creates the right account type based on method called

3. Delegation
   — Transaction delegates all operations to Bank
   — Bank delegates to Account

4. Interface Segregation
   — BankInterface, AccountInterface, TransactionInterface
   — Each defines a specific contract
```

---

# SECTION C: SQL Queries

## 2-Minute Walkthrough

```
"Three queries of increasing complexity on job execution data:

Query 1: Latest execution ID per job — simple GROUP BY + MAX
Query 2: Latest execution with last status timestamp — 
  subquery to find max execution_id, then JOIN to status history
Query 3: Job name with the actual last status — 
  DISTINCT ON to get the most recent status per execution

I also added indexes for performance:
  idx_job_executions_name_id — covers the GROUP BY
  idx_job_status_execution_timestamp — covers the ORDER BY in DISTINCT ON"
```

## SQL Questions They'll Ask

### "Walk me through Query 3 — the hardest one."

```
"I need: for each job, its latest execution's last status.

Step 1: Subquery finds latest execution_id per job
  SELECT job_name, MAX(execution_id) FROM job_executions GROUP BY job_name

Step 2: Join back to get the execution row
  INNER JOIN on job_name AND execution_id = max_execution_id

Step 3: Subquery finds last status per execution
  DISTINCT ON (execution_id) ordered by timestamp DESC
  This gives me the most recent status row for each execution

Step 4: Join execution to its last status

Result: job_name + its latest execution's most recent status"
```

### "DISTINCT ON is PostgreSQL-specific. How would you do this in standard SQL?"

```
"Window function:

SELECT execution_id, status
FROM (
    SELECT execution_id, status, timestamp,
           ROW_NUMBER() OVER (
               PARTITION BY execution_id 
               ORDER BY timestamp DESC
           ) AS rn
    FROM job_status_history
) ranked
WHERE rn = 1

ROW_NUMBER + PARTITION BY is the standard SQL equivalent 
of PostgreSQL's DISTINCT ON."
```

### "Why those specific indexes?"

```
"idx_job_executions_name_id ON (job_name, execution_id):
  Covers GROUP BY job_name + MAX(execution_id)
  The DB can scan the index to find max per group — no table scan

idx_job_status_execution_timestamp ON (execution_id, timestamp DESC):
  Covers the DISTINCT ON / ORDER BY execution_id, timestamp DESC
  For each execution_id, the DB reads timestamps in order from the index
  First row is the most recent — no sorting needed"
```

### "What if job_executions had millions of rows?"

```
"The indexes handle it. But additionally:

1. Partitioning — partition job_executions by date range
   Old jobs rarely queried, keep them in cold partitions

2. Materialized view — if this query runs frequently,
   precompute latest execution per job

3. Covering index — include status in the index so the DB 
   doesn't need to hit the table at all (index-only scan)"
```

---

# SECTION D: Valid Parentheses

## 1-Minute Walkthrough

```
"Classic stack problem. Map closing brackets to their opening 
bracket. Iterate through the string:
- Opening bracket → push to stack
- Closing bracket → check if stack top matches the expected 
  opening bracket. If not, return false.
At the end, stack must be empty (no unmatched opens).

Used ArrayDeque as the stack — faster than java.util.Stack 
(which is synchronized and legacy). HashMap for O(1) bracket 
lookup."
```

## Questions They'll Ask

### "Why ArrayDeque instead of Stack?"

```
"java.util.Stack extends Vector — every method is synchronized.
Unnecessary overhead for single-threaded code.
ArrayDeque is unsynchronized, faster, and the recommended 
replacement per Java docs.

Stack is also legacy — shouldn't be used in new code."
```

### "Why HashMap for bracket matching?"

```
"O(1) lookup for the matching opening bracket.
Alternative: if-else chain works too, but map is cleaner 
and easily extensible if you add new bracket types.

Trade-off: HashMap has overhead for just 3 entries. 
For production code, a switch statement might be faster. 
For readability, the map wins."
```

### "What's the time and space complexity?"

```
"Time: O(n) — single pass through the string
Space: O(n) — worst case all opening brackets: '(((((('"
```

### "What if the string is null or empty?"

```
"Empty string: for loop doesn't execute, stack.isEmpty() 
returns true → returns true. Correct — empty is valid.

Null string: My code would throw NullPointerException on 
s.length(). I should add a null check:
  if (s == null || s.isEmpty()) return true;"
```

---

# SECTION E: Cross-Cutting Thread Safety Summary

## Every Concurrent/Thread-Safe Decision Across All Code

### VelocityProvider
```
ConcurrentHashMap    — outer map, different cards don't block
TreeMap + synchronized — per-card locking (should be ConcurrentSkipListMap)
volatile             — oldestTimestamp (has race condition → AtomicLong)
Random               — not thread-safe (should be ThreadLocalRandom)
```

### Banking System
```
ConcurrentHashMap    — accounts map in Bank
AtomicLong           — account number generation
synchronized methods — Account.getBalance/credit/debit
synchronized methods — CommercialAccount.addAuthorizedUser/isAuthorizedUser
ArrayList            — authorizedUsers (protected by synchronized, 
                       could be CopyOnWriteArrayList)
```

### If They Ask "synchronized vs ConcurrentHashMap vs AtomicLong"
```
synchronized:
  Mutual exclusion + visibility
  Heavyweight — thread blocking
  Use for: compound operations on multiple fields

ConcurrentHashMap:
  Fine-grained per-bin locking
  Lock-free reads
  Use for: concurrent map access

AtomicLong:
  CAS-based, lock-free
  Use for: single counters/IDs

volatile:
  Visibility only, no atomicity
  Use for: flags (boolean running = true)
  NOT for: read-then-write
```

---

# SECTION F: System Design (If Time Remains)

## Documentation Platform — 5-Minute Version

```
SOURCES              PIPELINE              STORAGE           API

Headless CMS ──┐
               ├─ Kafka ─→ Transformer ─→ Neo4j (graph)
Git Repos ─────┤           Service         ElasticSearch     GraphQL
               │                           PostgreSQL        + REST
API Specs ─────┘
```

**Why three databases:**
```
Neo4j: "Show me docs related to Payment API" — graph traversals
ElasticSearch: "Search payment authentication" — relevance ranking
PostgreSQL: "Who edited this doc when?" — ACID, audit trail
```

**Connect to your code:**
```
VelocityProvider patterns    →  Documentation Platform
Strategy pattern (storage)   →  Pluggable content sources
ConcurrentHashMap + TreeMap  →  High-performance data retrieval APIs
Interface-based design       →  Multiple storage backends
Builder pattern for config   →  Configurable ingestion pipelines
```

---

# SECTION G: How to Handle Being Grilled

```
They find a flaw:
  "You're right. Here's how I'd fix that..."

They suggest something better:
  "That's a better approach because [reason]."

They ask about something you don't know:
  "I haven't used that in production, but my understanding is..."

They challenge your OOP:
  "I chose abstract class over interface because of shared state..."

They challenge your SQL:
  "DISTINCT ON is Postgres-specific, the portable version uses 
   ROW_NUMBER() OVER (PARTITION BY ... ORDER BY ...)"

Dead silence — you're stuck:
  "Let me think about that for a moment..."
  (10 seconds of thinking is fine)
```

---

# Day-Of Checklist

- [ ] Re-read ALL your code — VelocityProvider, Banking, SQL, Valid Parentheses
- [ ] Practice 3-min VelocityProvider walkthrough
- [ ] Practice 2-min Banking walkthrough
- [ ] Practice "two threads same card" explanation
- [ ] Know your 3 biggest weaknesses per solution
- [ ] Know your fixes: ConcurrentSkipListMap, AtomicLong, async cleanup
- [ ] Practice transfer atomicity question (Banking)
- [ ] Practice SQL Query 3 explanation
- [ ] Practice DISTINCT ON vs ROW_NUMBER alternative
- [ ] Have 3-5 questions ready for them
- [ ] Sleep well — you wrote this code, you know it