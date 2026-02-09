# Day 2 Afternoon: Java Design Exercises
## Time: ~2.5 hours | Think before you code. Draw diagrams on paper first.

---

## Exercise 1: Design Patterns Warm-up (30 min)

### Part A: Builder Pattern
Build an `Order` class for a payment system (like Adyen):

```java
// Should be used like:
Order order = Order.builder()
    .orderId("ORD-123")
    .merchantId("MERCH-456")
    .amount(99.99)
    .currency("EUR")
    .paymentMethod("CARD")
    .customerEmail("alice@email.com")
    .build();

// Order should be IMMUTABLE after build
// Validate: orderId, amount, currency are required — throw if missing
```

Implement the full Builder pattern. Make Order immutable (all fields final, no setters).

### Part B: Strategy Pattern
Create a fraud detection system with pluggable rules:

```java
// Interface
public interface FraudRule {
    boolean isFraudulent(Transaction transaction);
    String getRuleName();
}

// Implement these rules:
// 1. AmountThresholdRule — flag if amount > threshold
// 2. VelocityRule — flag if same card used > N times in last M minutes
// 3. BlockedCountryRule — flag if country is in blocked list

// FraudEngine runs ALL rules and returns results
public class FraudEngine {
    private List<FraudRule> rules;

    public void addRule(FraudRule rule) { }
    public void removeRule(FraudRule rule) { }
    public FraudCheckResult check(Transaction transaction) { }
}

// Usage:
FraudEngine engine = new FraudEngine();
engine.addRule(new AmountThresholdRule(10000));
engine.addRule(new BlockedCountryRule(Arrays.asList("XX", "YY")));
engine.addRule(new VelocityRule(5, Duration.ofMinutes(10)));

FraudCheckResult result = engine.check(transaction);
// result.isPassed(), result.getFailedRules()
```

### Part C: Factory Pattern
Create a payment method factory:

```java
public interface PaymentMethod {
    boolean processPayment(double amount);
    boolean refund(double amount);
    String getType();
}

// Implement: CardPayment, BankTransferPayment, WalletPayment

public class PaymentMethodFactory {
    public static PaymentMethod create(String type) {
        // return appropriate implementation based on type
    }
}

// Usage:
PaymentMethod method = PaymentMethodFactory.create("CARD");
method.processPayment(99.99);
```

Think about: How to add a new payment method WITHOUT modifying the factory? (Open-Closed Principle)

---

## Exercise 2: Design a Payment Processing System (45 min)

This is an Adyen-relevant design problem. Build it step by step.

### Requirements:
- Process payments from merchants
- Support multiple payment methods (Card, Bank Transfer, Wallet)
- Apply fraud rules before processing
- Track transaction history
- Support refunds

### Step 1: Define your models

```java
public enum TransactionStatus {
    PENDING, APPROVED, DECLINED, REFUNDED, FRAUD_BLOCKED
}

public class Transaction {
    // What fields does a transaction need?
    // Think: id, merchant, amount, currency, status, timestamp,
    //        paymentMethod, cardId, etc.
}

public class Merchant {
    // What fields? id, name, active fraud rules, transaction history?
}
```

### Step 2: Define your interfaces

```java
public interface PaymentProcessor {
    TransactionResult process(Transaction transaction);
}

public interface FraudChecker {
    FraudCheckResult check(Transaction transaction);
}

public interface TransactionStore {
    void save(Transaction transaction);
    Transaction findById(String id);
    List<Transaction> findByMerchant(String merchantId);
    List<Transaction> findByCard(String cardId, Duration window);
}
```

### Step 3: Implement the PaymentService

```java
public class PaymentService {
    private FraudChecker fraudChecker;
    private PaymentProcessor paymentProcessor;
    private TransactionStore transactionStore;

    // Constructor injection — dependencies passed in
    public PaymentService(FraudChecker fraudChecker,
                          PaymentProcessor paymentProcessor,
                          TransactionStore transactionStore) { }

    public TransactionResult processPayment(Transaction transaction) {
        // 1. Validate input
        // 2. Check fraud rules
        // 3. If passed, process payment
        // 4. Save transaction
        // 5. Return result
    }

    public TransactionResult refund(String transactionId) {
        // 1. Find original transaction
        // 2. Validate refund is possible
        // 3. Process refund
        // 4. Update transaction status
        // 5. Return result
    }
}
```

### Step 4: Implement InMemoryTransactionStore

```java
public class InMemoryTransactionStore implements TransactionStore {
    // What data structures do you need?
    // Think: Map<String, Transaction> for by-id lookup
    //        Map<String, List<Transaction>> for by-merchant
    //        Map<String, List<Transaction>> for by-card
    // Or: just one list and filter? What are the trade-offs?
}
```

### Step 5: Wire it all together and test

```java
public class Main {
    public static void main(String[] args) {
        // Create components
        TransactionStore store = new InMemoryTransactionStore();
        FraudChecker fraudChecker = new FraudEngine();
        PaymentProcessor processor = new CardPaymentProcessor();

        // Build the service
        PaymentService service = new PaymentService(fraudChecker, processor, store);

        // Process a payment
        Transaction txn = Transaction.builder()
            .merchantId("MERCH-1")
            .amount(150.00)
            .currency("EUR")
            .cardId("CARD-123")
            .build();

        TransactionResult result = service.processPayment(txn);
    }
}
```

### Design Questions to Think About:
- How would you add a new payment method?
- How would you add a new fraud rule?
- How would you make this thread-safe?
- How would you add logging/metrics?
- What if fraud rules need access to transaction history?
- How would you handle partial refunds?

---

## Exercise 3: Design a Rate Limiter (45 min)

This directly prepares you for the Velocity problem discussion.

### Requirements:
- Limit requests per client within a time window
- Support different limits for different clients
- Must be fast (O(1) or O(log n) per check)
- Must clean up old data

### Step 1: Define the interface

```java
public interface RateLimiter {
    // Returns true if request is allowed, false if rate limited
    boolean allowRequest(String clientId);

    // Get remaining requests for a client
    int getRemainingRequests(String clientId);
}
```

### Step 2: Implement Sliding Window approach

```java
public class SlidingWindowRateLimiter implements RateLimiter {
    private final int maxRequests;
    private final Duration window;
    private final Map<String, Deque<Long>> requestTimestamps;

    public SlidingWindowRateLimiter(int maxRequests, Duration window) { }

    @Override
    public boolean allowRequest(String clientId) {
        // 1. Get current time
        // 2. Get or create the deque for this client
        // 3. Remove expired timestamps (older than window)
        // 4. Check if under limit
        // 5. If yes, add current timestamp and return true
        // 6. If no, return false
    }

    // Add cleanup to prevent memory leak
    private void cleanup(Deque<Long> timestamps, long now) { }
}
```

### Step 3: Implement Token Bucket approach (alternative)

```java
public class TokenBucketRateLimiter implements RateLimiter {
    private final int maxTokens;
    private final double refillRate;  // tokens per second

    private static class Bucket {
        double tokens;
        long lastRefillTime;
    }

    private final Map<String, Bucket> buckets;

    public TokenBucketRateLimiter(int maxTokens, double refillRate) { }

    @Override
    public boolean allowRequest(String clientId) {
        // 1. Get or create bucket
        // 2. Refill tokens based on elapsed time
        // 3. If tokens >= 1, consume one and return true
        // 4. Else return false
    }
}
```

### Step 4: Add configurability per client

```java
public class ConfigurableRateLimiter implements RateLimiter {
    private final Map<String, RateLimiterConfig> clientConfigs;
    private final RateLimiterConfig defaultConfig;

    public void setClientConfig(String clientId, int maxRequests, Duration window) { }

    // Different clients get different limits
    // e.g., premium merchants get 1000/min, free tier gets 100/min
}
```

### Step 5: Write tests

```java
// Test basic limiting
RateLimiter limiter = new SlidingWindowRateLimiter(3, Duration.ofSeconds(10));
assertTrue(limiter.allowRequest("client1"));   // 1st — allowed
assertTrue(limiter.allowRequest("client1"));   // 2nd — allowed
assertTrue(limiter.allowRequest("client1"));   // 3rd — allowed
assertFalse(limiter.allowRequest("client1"));  // 4th — blocked!

// Test window expiry
// Wait for window to pass...
assertTrue(limiter.allowRequest("client1"));   // allowed again

// Test different clients don't interfere
assertTrue(limiter.allowRequest("client2"));   // different client — allowed
```

### Design Questions to Think About:
- Sliding window vs Token bucket — when to use which?
- How would you make this thread-safe?
- How would you distribute this across multiple servers?
- How do you handle clock skew in distributed systems?
- Memory cleanup — when do you remove inactive clients?
- How does this relate to your Velocity problem?

---

## Exercise 4: Design a Cache with TTL (30 min)

### Requirements:
- Key-value store with time-to-live per entry
- Configurable max size
- Eviction when full (LRU)
- Expired entries should be cleaned up

```java
public class TTLCache<K, V> {
    private final int maxSize;
    private final Duration defaultTTL;

    // What data structures do you need?
    // Think: LinkedHashMap for LRU ordering
    //        Map for storing expiry times

    public TTLCache(int maxSize, Duration defaultTTL) { }

    // Put with default TTL
    public void put(K key, V value) { }

    // Put with custom TTL
    public void put(K key, V value, Duration ttl) { }

    // Get — returns null if expired or not found
    public V get(K key) { }

    // Remove
    public V remove(K key) { }

    // Current size (excluding expired entries)
    public int size() { }

    // Cleanup expired entries
    private void evictExpired() { }

    // Evict oldest entry if at max size
    private void evictOldest() { }
}
```

Test it:
```java
TTLCache<String, String> cache = new TTLCache<>(3, Duration.ofSeconds(5));
cache.put("a", "1");
cache.put("b", "2");
cache.put("c", "3");
cache.get("a");          // "1"
cache.put("d", "4");     // evicts "b" (LRU — "a" was recently accessed)
cache.get("b");          // null (evicted)

// After 5 seconds...
cache.get("a");          // null (expired)
```

### Design Questions:
- Why LinkedHashMap for LRU?
- How does this relate to your Velocity cleanup mechanism?
- Thread safety — how would you handle concurrent access?
- Lazy vs eager cleanup — trade-offs?

---

## SOLID Principles Cheat Sheet

### S — Single Responsibility
```
Each class does ONE thing.
PaymentService processes payments.
FraudEngine checks fraud.
TransactionStore stores data.
NOT one God class that does everything.
```

### O — Open/Closed
```
Open for extension, closed for modification.
Add new FraudRule by creating a new class.
Don't modify FraudEngine to add rules.
Add new PaymentMethod by creating a new class.
Don't modify PaymentService.
```

### L — Liskov Substitution
```
Subclass can replace parent anywhere.
ConsumerAccount works wherever Account is expected.
CardPayment works wherever PaymentMethod is expected.
```

### I — Interface Segregation
```
Don't force classes to implement methods they don't use.
Split fat interfaces into smaller ones.
PaymentProcessor doesn't need fraud checking methods.
```

### D — Dependency Inversion
```
Depend on abstractions, not concretions.
PaymentService takes FraudChecker interface, not FraudEngine class.
Easy to swap implementations. Easy to test with mocks.
```

---

## Design Interview Tips for Senior Role

1. **Always start with interfaces** — show you think about contracts before implementation
2. **Constructor injection** — pass dependencies in, don't create them inside
3. **Explain trade-offs** — "I chose HashMap for O(1) but TreeMap would give us sorted access"
4. **Think about extensibility** — "Adding a new payment method only requires implementing the interface"
5. **Mention testing** — "This design is testable because I can mock the FraudChecker"
6. **Consider concurrency** — "In production, this Map would need to be ConcurrentHashMap"
7. **Name things well** — good naming shows clear thinking
8. **Draw before you code** — sketch the class diagram, show the relationships

---

*Exercise 2 (Payment Processing) is the most important — it's directly relevant to Adyen's domain.
Exercise 3 (Rate Limiter) is basically your Velocity problem reframed.
If you can design and implement both of these cleanly, you're in great shape for the senior role.*