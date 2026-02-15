# Adyen Interview Prep — Banking System Walkthrough

**February 16, 2026**

---

## 1. What You Submitted

The HackerRank asked you to implement a UML diagram for a banking system, assuming a distributed environment. Your submission included:

- `Account` (abstract) — `synchronized` balance operations
- `AccountHolder` (abstract) → `Person`, `Company`
- `ConsumerAccount`, `CommercialAccount` (with authorized users)
- `Bank` — `ConcurrentHashMap` + `AtomicLong`
- `Transaction` — PIN auth in constructor
- Interfaces: `AccountInterface`, `BankInterface`, `TransactionInterface`

### Your Elevator Pitch

> "I implemented a banking system with OOP principles — abstract classes for Account and AccountHolder, with concrete implementations for consumer and commercial accounts. I used ConcurrentHashMap for thread-safe account storage, AtomicLong for unique ID generation, and synchronized balance operations. The interfaces make each component swappable for production implementations."

---

## 2. OOP Questions

### Abstraction

**Q: Why is Account abstract?**

> "Account defines common behavior — balance, PIN validation, credit, debit — but shouldn't be instantiated directly. You're always opening either a ConsumerAccount or CommercialAccount. The abstract class holds shared logic so I don't duplicate it in both subclasses."

**Q: Why is AccountHolder abstract?**

> "Same reason — you're never just an 'account holder'. You're either a Person or a Company. They share an `idNumber` (SSN or tax ID), but have different additional fields. Abstract class captures the common part."

### Inheritance vs Interfaces

**Q: Why does Account both extend an abstract class AND implement AccountInterface?**

> "The interface defines the contract — what operations an Account supports. The abstract class provides shared implementation. This way, if someone needs a completely different Account implementation (e.g., a mock for testing), they can implement AccountInterface without extending Account."

### Encapsulation

**Q: Why are Account fields private instead of protected?**

> "Even subclasses like ConsumerAccount shouldn't directly modify balance — that bypasses the synchronized protection. By keeping fields private and exposing synchronized methods, I ensure all balance changes go through the thread-safe path. Subclasses use the public API, not direct field access."

### Polymorphism

**Q: Where does polymorphism show up in your design?**

> "Bank stores `Map<Long, Account>` — it doesn't know or care whether it's a ConsumerAccount or CommercialAccount. When it calls `account.debitAccount(amount)`, the right implementation runs based on the actual type. Also, AccountHolder — Bank doesn't care if it's a Person or Company."

### Design Patterns in the Banking System

| Pattern | Where | Why |
|---------|-------|-----|
| **Template Method** | Account (abstract) with shared credit/debit logic | Subclasses inherit common behavior |
| **Strategy** (proposed extension) | AccountDao interface | Swap InMemory for PostgreSQL |
| **Decorator** (proposed extension) | AuditingBank wraps Bank | Add logging without modifying Bank |
| **Factory** (proposed extension) | Account creation in Bank | Bank decides which Account subclass to create |

---

## 3. Concurrency Questions

### Why synchronized on balance operations?

**Q: Why did you synchronize getBalance, creditAccount, and debitAccount?**

> "Two reasons. First, **mutual exclusion** — only one thread can modify balance at a time, preventing race conditions on credit/debit. Second, **memory visibility** — without synchronized, a thread could read a stale cached value of balance due to CPU caching. The synchronized keyword establishes a happens-before relationship, guaranteeing the reading thread sees the latest write."

### Why synchronized on getBalance? It's just a read.

**Q: getBalance only reads — why lock it?**

> "Java Memory Model — each thread can have its own CPU cache. Without synchronized, Thread A could update balance to 500 but Thread B still sees the old cached value of 400. synchronized forces the read from main memory, not the CPU cache."

**Follow-up: Could you use volatile instead?**

> "For getBalance alone, yes — volatile guarantees visibility. But debitAccount does a check-then-act (read balance, compare, then subtract). volatile doesn't make that atomic. So I kept all three synchronized for consistency and correctness."

### ConcurrentHashMap + AtomicLong

**Q: Why ConcurrentHashMap instead of HashMap?**

> "HashMap is not thread-safe. If two threads open accounts simultaneously, HashMap could corrupt internally — lost entries, infinite loops in older Java versions. ConcurrentHashMap allows concurrent reads and writes safely without locking the whole map."

**Q: Why AtomicLong instead of synchronized long?**

> "AtomicLong uses Compare-And-Swap at the CPU level — no locking, no thread sleeping. For a simple counter increment, it's much faster than synchronized because there's no context switch overhead. synchronized would work correctly but it's heavier — it involves OS-level thread scheduling. For a single variable update like account number generation, AtomicLong is the right tool."

### The shared lock problem

**Q: CommercialAccount has synchronized addAuthorizedUser. Account has synchronized debitAccount. Do they block each other?**

> "Yes — synchronized methods all lock on `this`. So adding an authorized user blocks debit/credit even though they're unrelated operations. I'd fix this with separate lock objects:"

```java
class CommercialAccount extends Account {
    private final Object userLock = new Object();
    
    void addAuthorizedUser(Person p) {
        synchronized (userLock) {  // doesn't block balance operations
            authorizedUsers.add(p);
        }
    }
}
```

### Transaction constructor authentication

**Q: Authentication happens in the constructor and throws an exception. What's wrong with this?**

> "Three problems: (1) Single Responsibility violation — constructor should create the object, not do business logic. (2) Throwing in constructors leaves a half-constructed object. (3) Can't reuse — 5 transactions means authenticating 5 times. I'd extract an AuthenticationService that validates the PIN separately. Transaction only gets created after successful auth."

---

## 4. Distributed Environment Questions

### The Double-Spend Problem

**Q: Your debitAccount checks balance >= amount then subtracts. What goes wrong with two servers?**

> "Two servers could both read balance = $500 and both approve a $400 debit — that's a double-spend, customer gets $800 from a $500 account. My synchronized blocks only protect within one JVM, not across servers."

### The Fix

> "I'd make two changes. First, create an AccountDao interface with save, findById, findAll methods, and implement it with PostgreSQL instead of ConcurrentHashMap. Strategy pattern — Bank depends on an interface, storage is swappable.

> Second, instead of checking balance in Java, use a single atomic SQL: `UPDATE accounts SET balance = balance - 400 WHERE balance >= 400`. The database handles concurrency — no Java-level locking needed."

### Mapping Java concepts to database

| Java Code (Single JVM) | Database (Distributed) |
|------------------------|----------------------|
| `ConcurrentHashMap` | PostgreSQL table |
| `AtomicLong` | DB sequence / Snowflake ID |
| `synchronized` on Account | `SELECT ... FOR UPDATE` or atomic UPDATE |
| `InMemoryAccountDao` | `PostgresAccountDao` |
| Manual rollback in transfer | `BEGIN` / `COMMIT` / `ROLLBACK` |

### Transfers in a distributed environment

**Q: How would you handle transfers across two accounts in a database?**

> "Wrap both operations in a database transaction. BEGIN, debit the source with `UPDATE ... WHERE balance >= amount`, credit the destination, then COMMIT. If anything fails, ROLLBACK — both succeed or neither does. The database handles deadlock detection automatically — no need for ordered locking in Java."

### AtomicLong in distributed

**Q: Your AtomicLong for account numbers — what happens with multiple servers?**

> "AtomicLong only guarantees uniqueness within one JVM. Two servers could generate the same account number. I'd replace it with database sequences (auto-increment), UUIDs, or Snowflake IDs for global uniqueness."

---

## 5. Extensions You'd Propose

When they ask "what would you change or add":

### 1. AccountDao Interface (Storage)

> "Decouple storage from Bank. Create an interface so I can swap InMemoryAccountDao for PostgresAccountDao without changing Bank."

### 2. AuthenticationService

> "Pull auth out of Transaction constructor. Separate service validates PIN, returns result. Transaction only created after successful auth. Single Responsibility."

### 3. TransactionService

> "Extract debit, credit, transfer into a dedicated service. In distributed environment, this service handles database transactions — BEGIN, UPDATE, COMMIT/ROLLBACK. Bank becomes a thin orchestrator."

### 4. AuditingBank (Decorator Pattern)

> "Wrap Bank with an AuditingBank that logs all credits/debits without modifying Bank code. Open/Closed Principle — add behavior without changing existing classes."

### 5. Currency Support

> "Add Currency enum to Account, create ExchangeRateService interface. CurrencyService converts amounts during transfers. Interface makes it swappable between in-memory rates and a live API."

---

## 6. General Java OOP Concepts

### SOLID Principles in Your Code

| Principle | Example |
|-----------|---------|
| **Single Responsibility** | Bank manages accounts, Account manages balance, Transaction handles auth |
| **Open/Closed** | Add CommercialAccount without modifying Account. Add AccountDao without modifying Bank |
| **Liskov Substitution** | ConsumerAccount and CommercialAccount both work wherever Account is expected |
| **Interface Segregation** | AccountInterface, BankInterface, TransactionInterface — separate contracts |
| **Dependency Inversion** | Bank depends on AccountInterface, not concrete Account classes |

### Abstract Class vs Interface

| Feature | Abstract Class | Interface |
|---------|---------------|-----------|
| Fields | Can have instance fields | Only constants (static final) |
| Constructors | Yes | No |
| Method implementation | Can have concrete methods | Default methods only (Java 8+) |
| Inheritance | Single (extends one) | Multiple (implements many) |
| Use when | Shared state + behavior (Account, AccountHolder) | Contract only (BankInterface, AccountInterface) |

### Key Java Concurrency Concepts

| Concept | What It Does | Used In Your Code |
|---------|-------------|-------------------|
| `synchronized` | Mutual exclusion + memory visibility | Account balance ops |
| `volatile` | Memory visibility only (no atomicity) | Would work for getBalance alone |
| `AtomicLong` | Lock-free atomic operations via CAS | Account number generation |
| `ConcurrentHashMap` | Thread-safe map, fine-grained locking | Bank account storage |
| `ThreadLocal` | Per-thread isolated storage | Request ID tracking (extension) |
| `ReadWriteLock` | Concurrent reads, exclusive writes | VelocityProvider TreeMap storage |

---

## 7. Quick Comparison: Banking vs VelocityProvider

They may ask how your two solutions compare:

| Aspect | VelocityProvider | Banking System |
|--------|-----------------|----------------|
| **Core data structure** | TreeMap (sorted timestamps) | ConcurrentHashMap (account lookup) |
| **Concurrency** | ReadWriteLock per card | synchronized per account |
| **Design pattern** | Strategy (storage + cleanup) | Template Method (Account hierarchy) |
| **Cleanup** | Probabilistic / scheduled | N/A (but cleanup threshold in extension) |
| **Distributed scaling** | Redis sorted sets | PostgreSQL with atomic SQL |
| **Key tradeoff** | Consistency vs throughput | Safety vs performance |