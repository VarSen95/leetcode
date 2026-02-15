package velocityProviderPractice.velocityProviderFinalPractice;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.time.*;

public class Solution {

    static class Payment {
        /* The payment ID. */
        private final String paymentId;
        /* The timestamp of the payment processing start. */
        private final Instant timestamp;
        /* The hashed card number used for the payment. */
        private final String hashedCardNumber;

        /*
         * EXTENSIBILITY NOTE - Merchant-Specific Configuration:
         * To support per-merchant retention/limits, add:
         * private final String merchantId;
         * Then use Map<String, VelocityProviderConfig> or MerchantConfigService
         * to apply merchant-specific cleanup thresholds while keeping rule
         * durations external (passed as the duration parameter).
         */

        public Payment(String paymentId, Instant timestamp, String hashedCardNumber) {
            this.paymentId = paymentId;
            this.timestamp = timestamp;
            this.hashedCardNumber = hashedCardNumber;
        }

        public String getPaymentId() {
            return paymentId;
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public String getHashedCardNumber() {
            return hashedCardNumber;
        }
    }

    interface VelocityProvider {

        /**
         * This method is called during the payment risk assessment.
         * 
         * It returns how many times the card in the Payment has been seen in the last
         * minutes/seconds/hours as
         * defined in the {@code duration} parameter at the time of the payment
         * processing start.
         *
         * @param payment  The payment being processed
         * @param duration The interval to count
         * @return The number of times the card was used in the interval defined in
         *         duration.
         */
        int getCardUsageCount(Payment payment, Duration duration);

        /**
         * After the payment is processed this method is called.
         *
         * @param payment The payment that has been processed.
         */
        void registerPayment(Payment payment);

        /**
         * @return Instance of a Velocity provider
         */
        static VelocityProvider getProvider() {
            return new VelocityProviderImpl();
        }
    }

    /**
     * Production-ready implementation of VelocityProvider with focus on:
     * - Maintainability: Clear separation of concerns, well-documented code
     * - Readability: Descriptive names, logical structure, clear intent
     * - Extensibility: Pluggable strategies for storage and cleanup
     * - Scalability: Thread-safe, efficient algorithms, configurable memory
     * management
     * 
     * MERCHANT-SPECIFIC CONFIGURATION PATTERN (when needed):
     * To support per-merchant retention policies:
     * 
     * 1. Add merchantId to Payment class
     * 2. Create MerchantConfigService:
     * interface MerchantConfigService {
     * VelocityProviderConfig getConfig(String merchantId);
     * }
     * 3. Store merchant configs:
     * Map<String, VelocityProviderConfig> merchantConfigs = new
     * ConcurrentHashMap<>();
     * 4. At query/registration time:
     * VelocityProviderConfig config = merchantConfigs.get(payment.getMerchantId());
     * 
     * This allows different merchants to have:
     * - Different cleanup thresholds (e.g., high-risk merchants keep 30 days,
     * others 7 days)
     * - Different cleanup frequencies
     * - While keeping rule durations (velocity windows) external as query
     * parameters
     * 
     * SCALABILITY SUMMARY:
     * - Per-request cost: O(log n + k) per card query
     * - Memory: Bounded by cleanup threshold × payment volume
     * - Thread-safe: Safe for concurrent payment processing
     * - Sharding ready: Easy to distribute by card hash or merchantId
     * - 20x scale: partition by card hash, keep per-card state local, and bound
     * history with cleanup
     */
    static class VelocityProviderImpl implements VelocityProvider {

        // Storage strategy for payment timestamps
        private final TimestampStorage timestampStorage;

        // Cleanup strategy for old data
        private final CleanupStrategy cleanupStrategy;

        // Configuration for the provider
        private final VelocityProviderConfig config;

        /**
         * Default constructor with standard configuration.
         */
        public VelocityProviderImpl() {
            this(VelocityProviderConfig.defaultConfig());
        }

        /**
         * Constructor with custom configuration for flexibility.
         * Allows tuning behavior for different use cases.
         */
        public VelocityProviderImpl(VelocityProviderConfig config) {
            this.config = config;
            this.timestampStorage = new TreeMapTimestampStorage();
            this.cleanupStrategy = new ProbabilisticCleanupStrategy(
                    config.getCleanupThreshold(),
                    config.getCleanupProbability());
        }

        @Override
        public int getCardUsageCount(Payment payment, Duration duration) {
            validatePayment(payment);
            validateDuration(duration);

            return timestampStorage.countInWindow(
                    payment.getHashedCardNumber(),
                    payment.getTimestamp(),
                    duration);
        }

        @Override
        public void registerPayment(Payment payment) {
            validatePayment(payment);

            timestampStorage.addTimestamp(
                    payment.getHashedCardNumber(),
                    payment.getTimestamp());

            // Trigger cleanup if strategy decides it's time
            if (cleanupStrategy.shouldCleanup()) {
                cleanupStrategy.cleanup(timestampStorage, payment.getTimestamp());
            }
        }

        /**
         * Validates payment input to fail fast on invalid data.
         */
        private void validatePayment(Payment payment) {
            if (payment == null) {
                throw new IllegalArgumentException("Payment cannot be null");
            }
            if (payment.getHashedCardNumber() == null || payment.getHashedCardNumber().isEmpty()) {
                throw new IllegalArgumentException("Card number hash cannot be null or empty");
            }
            if (payment.getTimestamp() == null) {
                throw new IllegalArgumentException("Payment timestamp cannot be null");
            }
        }

        /**
         * Validates duration to ensure valid time windows.
         */
        private void validateDuration(Duration duration) {
            if (duration == null || duration.isNegative() || duration.isZero()) {
                throw new IllegalArgumentException("Duration must be positive");
            }
        }
    }

    /**
     * Configuration class for VelocityProvider.
     * Centralizes all tunable parameters for easy maintenance and testing.
     */
    static class VelocityProviderConfig {
        private final Duration cleanupThreshold;
        private final double cleanupProbability;

        private VelocityProviderConfig(Builder builder) {
            this.cleanupThreshold = builder.cleanupThreshold;
            this.cleanupProbability = builder.cleanupProbability;
        }

        public static VelocityProviderConfig defaultConfig() {
            return new Builder().build();
        }

        public Duration getCleanupThreshold() {
            return cleanupThreshold;
        }

        public double getCleanupProbability() {
            return cleanupProbability;
        }

        /**
         * Builder pattern for flexible configuration.
         */
        public static class Builder {
            // Conservative default: keep 7 days of data
            // This ensures cleanup won't interfere with reasonable query durations
            private Duration cleanupThreshold = Duration.ofDays(7);
            private double cleanupProbability = 0.001; // 0.1% chance per registration

            public Builder cleanupThreshold(Duration threshold) {
                this.cleanupThreshold = threshold;
                return this;
            }

            public Builder cleanupProbability(double probability) {
                if (probability < 0 || probability > 1) {
                    throw new IllegalArgumentException("Probability must be between 0 and 1");
                }
                this.cleanupProbability = probability;
                return this;
            }

            public VelocityProviderConfig build() {
                return new VelocityProviderConfig(this);
            }
        }
    }

    /**
     * Interface for timestamp storage strategies.
     * Allows different implementations for different use cases (in-memory,
     * distributed, etc.)
     */
    interface TimestampStorage {
        /**
         * Adds a timestamp for a specific card.
         */
        void addTimestamp(String cardHash, Instant timestamp);

        /**
         * Counts timestamps within a time window for a card.
         */
        int countInWindow(String cardHash, Instant queryTime, Duration duration);

        /**
         * Removes all timestamps older than the cutoff time.
         */
        void removeOlderThan(Instant cutoffTime);

        /**
         * Gets the oldest timestamp across all cards.
         */
        Instant getOldestTimestamp();
    }

    /**
     * TreeMap-based implementation of TimestampStorage.
     * Uses TreeMap<Long, Integer> to track counts per timestamp, properly handling
     * duplicates.
     * Optimized for time-range queries with O(log n) complexity.
     * 
     * SCALABILITY NOTES:
     * - Per-request cost: O(log n + k) where n = timestamps per card, k = results
     * in window
     * - Memory bounded by cleanup threshold (default 7 days)
     * - Thread-safe: ConcurrentHashMap + per-card TreeMap synchronization
     * 
     * SHARDING STRATEGY:
     * - Current: Keyed by hashedCardNumber for natural distribution
     * - Easy to shard: Hash(cardNumber) % numShards → shard assignment
     * - For merchant-specific: Use composite key (merchantId + cardHash)
     * - Horizontal scaling: Replace with Redis/Cassandra via TimestampStorage
     * interface
     * 
     * CLEANUP THRESHOLD vs QUERY DURATION:
     * - cleanupThreshold MUST be >= max expected query duration
     * - Default 7 days ensures most reasonable velocity rules work
     * - If querying "card seen in last 30 days", set threshold >= 30 days
     * - Too aggressive cleanup = lost data = incorrect counts
     */
    static class TreeMapTimestampStorage implements TimestampStorage {
        // Thread-safe map of card hash to timestamp counts
        // TreeMap<epochMilli, count> allows duplicate timestamps with proper counting
        private final Map<String, TreeMap<Long, Integer>> cardTimestamps;
        private AtomicLong oldestTimestampMillis = new AtomicLong(Long.MAX_VALUE);

        public TreeMapTimestampStorage() {
            this.cardTimestamps = new java.util.concurrent.ConcurrentHashMap<>();
        }

        @Override
        public void addTimestamp(String cardHash, Instant timestamp) {
            TreeMap<Long, Integer> timestamps = cardTimestamps.computeIfAbsent(
                    cardHash,
                    k -> new TreeMap<>());

            long epochMilli = timestamp.toEpochMilli();

            // Synchronize on the same timestamps object as countInWindow
            synchronized (timestamps) {
                // Increment count for this timestamp (handles duplicates)
                timestamps.merge(epochMilli, 1, Integer::sum);
            }

            this.oldestTimestampMillis.accumulateAndGet(epochMilli, Long::min);
        }

        @Override
        public int countInWindow(String cardHash, Instant queryTime, Duration duration) {
            // CAVEAT: Treemap is not thread-safe that why we need to use snchornized
            // blocks. If we wanna do the locking aoutomagtically we can use
            // ConcurrentSkipListMap. With this data struture, the reads can happen
            // concurrently, there;s ginan be no waiting..however writes will block.

            // One downside is its weakly consistent. So we can use ReadWriteLock for strong
            // consistency..we can lock while writeing as well as querying. This helps with
            // paralel reads. But for write heavy scenarios where muktiple transactions are
            // happenig at the same instant, the wriet would have to tobe processed
            // sequentially. One way to fix it is to make the write faster. So if the query
            // oattenr is just th elast x seconds an the system ensures sorted timestamps,
            // then we can go for Deque.Deque we would add to tail, and evict from the
            // head. This eviction is done based on whether the head timestamp < cutoff
            // timestamp. Deque append and evict are O(1) so in those scenarios this is best
            // option.

            // For production I would use low latency shared database like redis because in
            // distributed systems the counts can ebe fragmented. We will use operations
            // like ZADD, zcount (card hash, startTime, endTime) and ZREMRANGEBYSCORE
            // For hot cards, we will accept serialisation of the requests. But apply some
            // rate limiting

            // In a multi-instance deployment behind a load balancer, my current design
            // would break because each JVM maintains its own in-memory state, so velocity
            // counts would be fragmented across servers. A query hitting one instance would
            // only see the subset of payments processed by that instance, leading to
            // incorrect results. To fix this, I would externalize the state into a shared
            // distributed store such as Redis. Each card could be modeled as a sorted set
            // keyed by card hash, where registerPayment performs a ZADD, queries use ZCOUNT
            // over the time window, and cleanup uses ZREMRANGEBYSCORE. The tradeoffs
            // include added network latency and dependency on Redis availability, but in
            // return we gain correctness across instances, true horizontal scalability, and
            // simplified concurrency guarantees since Redis operations are atomic per key.

            // Okay another note reqriet lock s
            TreeMap<Long, Integer> timestamps = cardTimestamps.get(cardHash);

            if (timestamps == null || timestamps.isEmpty()) {
                return 0;
            }

            long windowStartMillis = queryTime.minus(duration).toEpochMilli();
            long queryTimeMillis = queryTime.toEpochMilli();

            // OPTIMISE: Since we are holding a lock, we wanna minimise the time for
            // operation and a simple for loop avoids overhead and extra allocation provided
            // by streams. So its faster
            synchronized (timestamps) {
                int sum = 0;
                for (Integer count : timestamps.subMap(windowStartMillis, true, queryTimeMillis, true).values()) {
                    sum += count;
                }
                return sum;
            }

        }

        @Override
        public synchronized void removeOlderThan(Instant cutoffTime) {
            long cutoffMillis = cutoffTime.toEpochMilli();

            // If late-arriving events are possible, I would introduce an allowed lateness
            // buffer — meaning I retain data slightly longer than the business retention
            // window to ensure delayed events can still be processed correctly

            if (oldestTimestampMillis.get() > cutoffMillis) {
                return; // No data old enough to clean
            }

            Long newOldest = Long.MAX_VALUE;

            // Clean old timestamps from each card's TreeMap
            // Cleanup is risky under load because it performs a global scan over all cards
            // and acquires many per-card locks, which can create latency spikes and
            // contention—especially if the map is large. Even though each lock is per-card,
            // the sweep itself is expensive and can overlap with live traffic, causing
            // register/query operations on many cards to block. To improve this, I’d
            // decouple cleanup from the hot path: run cleanup asynchronously on a
            // background schedule, or make it incremental by cleaning only a limited number
            // of cards per trigger. Another common approach is to track expirations
            // efficiently (e.g., a global min-heap/queue of oldest timestamps per card, or
            // per-card TTL/eviction) so we don’t have to scan every card each time.
            for (TreeMap<Long, Integer> timestamps : cardTimestamps.values()) {
                synchronized (timestamps) {
                    // Remove old timestamps (headMap returns entries < cutoff)
                    timestamps.headMap(cutoffMillis, false).clear();

                    // Track new oldest timestamp if card has remaining data
                    if (!timestamps.isEmpty()) {
                        Long first = timestamps.firstKey();
                        if (first < newOldest) {
                            newOldest = first;
                        }
                    }
                }
            }

            // Remove empty card entries using removeIf (ConcurrentHashMap supports this)
            cardTimestamps.entrySet().removeIf(entry -> entry.getValue().isEmpty());

            oldestTimestampMillis.set(newOldest);
        }

        @Override
        public Instant getOldestTimestamp() {
            return Instant.ofEpochMilli(oldestTimestampMillis.get());
        }
    }

    // At very high volume, the bottleneck becomes per-card serialization. For a hot
    // card, all threads contend on the same per-card lock (or in Redis, the same
    // hot key), so latency increases and throughput caps out for that card. Memory
    // can also grow if retention is large and cleanup isn’t aggressive enough,
    // especially for cards that stop being queried. To improve it, I would (1) keep
    // the write path O(1) and lightweight (deque helps), (2) add rate limiting /
    // circuit breaking for abusive hot cards since that traffic is often
    // suspicious, (3) make cleanup predictable via a scheduled job or probabilistic
    // cleanup, and (4) for multi-instance scaling, move state to a shared store
    // like Redis ZSET so counts aren’t fragmented. Tradeoff-wise, you can’t fully
    // parallelize writes for the same card if you want strong per-card
    // consistency—so the pragmatic approach is to keep the critical section small
    // and control hot-key behavior.
    static class DequeBasedTimestampStorage implements TimestampStorage {
        private final Map<String, Deque<Long>> cardTimestamps;
        private AtomicLong oldestTimestampMillis = new AtomicLong(Long.MAX_VALUE);

        public DequeBasedTimestampStorage() {
            this.cardTimestamps = new java.util.concurrent.ConcurrentHashMap<>();
        }

        @Override
        public void addTimestamp(String cardHash, Instant timestamp) {
            Deque<Long> timestamps = cardTimestamps.computeIfAbsent(
                    cardHash,
                    k -> new ArrayDeque<>());

            long epochMilli = timestamp.toEpochMilli();

            synchronized (timestamps) {
                timestamps.addLast(epochMilli);

            }

            this.oldestTimestampMillis.accumulateAndGet(epochMilli, Long::min);
        }

        @Override
        public int countInWindow(String cardHash, Instant queryTime, Duration duration) {

            Deque<Long> timestamps = cardTimestamps.get(cardHash);

            if (timestamps == null || timestamps.isEmpty()) {
                return 0;
            }

            long windowStartMillis = queryTime.minus(duration).toEpochMilli();
            long queryTimeMillis = queryTime.toEpochMilli();
            // With a deque sliding window, insert is O(1). Counting is amortized O(1)
            // because each timestamp is added once and removed once. We avoid the O(log n +
            // k) range scan entirely, which makes it ideal for high write workloads with
            // real-time sliding window queries.
            synchronized (timestamps) {
                while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStartMillis) {
                    timestamps.pollFirst();
                }

                return timestamps.size();
            }

        }

        @Override
        public synchronized void removeOlderThan(Instant cutoffTime) {
            long cutoffMillis = cutoffTime.toEpochMilli();

            // If late-arriving events are possible, I would introduce an allowed lateness
            // buffer — meaning I retain data slightly longer than the business retention
            // window to ensure delayed events can still be processed correctly

            if (oldestTimestampMillis.get() > cutoffMillis) {
                return; // No data old enough to clean
            }

            Long newOldest = Long.MAX_VALUE;

            // Clean old timestamps from each card's TreeMap
            for (Deque<Long> timestamps : cardTimestamps.values()) {
                synchronized (timestamps) {
                    // Remove old timestamps (headMap returns entries < cutoff)

                    while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoffMillis) {
                        timestamps.pollFirst();
                    }

                }
            }

            // Remove empty card entries using removeIf (ConcurrentHashMap supports this)
            cardTimestamps.entrySet().removeIf(entry -> entry.getValue().isEmpty());

            oldestTimestampMillis.set(newOldest);
        }

        @Override
        public Instant getOldestTimestamp() {
            return Instant.ofEpochMilli(oldestTimestampMillis.get());
        }
    }

    /**
     * Interface for cleanup strategies.
     * Allows different approaches to memory management.
     */
    interface CleanupStrategy {
        /**
         * Determines if cleanup should occur.
         */
        boolean shouldCleanup();

        /**
         * Performs cleanup on the storage.
         */
        void cleanup(TimestampStorage storage, Instant currentTime);
    }

    /**
     * Probabilistic cleanup strategy to distribute cleanup overhead.
     * Prevents cleanup from blocking a single payment operation.
     * 
     * Note: For testing, use DeterministicCleanupStrategy or inject a seeded Random
     * via the constructor that accepts a Random instance.
     */
    static class ProbabilisticCleanupStrategy implements CleanupStrategy {
        private final Duration cleanupThreshold;
        private final double cleanupProbability;
        private final Random random;

        /**
         * Constructor with default Random (non-deterministic).
         */
        public ProbabilisticCleanupStrategy(Duration threshold, double probability) {
            this(threshold, probability, new Random());
        }

        /**
         * Constructor with injectable Random for deterministic testing.
         * 
         * @param threshold   Duration after which data should be cleaned
         * @param probability Probability of cleanup per registration (0.0 to 1.0)
         * @param random      Random instance (can be seeded for tests)
         */
        public ProbabilisticCleanupStrategy(Duration threshold, double probability, Random random) {
            this.cleanupThreshold = threshold;
            this.cleanupProbability = probability;
            this.random = random;
        }

        @Override
        public boolean shouldCleanup() {
            return random.nextDouble() < cleanupProbability;
        }

        @Override
        public void cleanup(TimestampStorage storage, Instant currentTime) {
            Instant cutoffTime = currentTime.minus(cleanupThreshold);
            storage.removeOlderThan(cutoffTime);
        }
    }

    /**
     * Deterministic cleanup strategy for testing.
     * Cleans up every N registrations for predictable behavior in tests.
     */
    static class DeterministicCleanupStrategy implements CleanupStrategy {
        private final Duration cleanupThreshold;
        private final int cleanupInterval;
        private int registrationCount;

        /**
         * @param threshold       Duration after which data should be cleaned
         * @param cleanupInterval Clean up every N registrations
         */
        public DeterministicCleanupStrategy(Duration threshold, int cleanupInterval) {
            this.cleanupThreshold = threshold;
            this.cleanupInterval = cleanupInterval;
            this.registrationCount = 0;
        }

        @Override
        public synchronized boolean shouldCleanup() {
            registrationCount++;
            return registrationCount % cleanupInterval == 0;
        }

        @Override
        public void cleanup(TimestampStorage storage, Instant currentTime) {
            Instant cutoffTime = currentTime.minus(cleanupThreshold);
            storage.removeOlderThan(cutoffTime);
        }
    }

    public static void main(String args[]) throws Exception {
        final VelocityProvider velocityProvider = VelocityProvider.getProvider();

        try (final Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                final String assoc = scanner.next();
                final String[] split = assoc.split(":");

                final String operation = split[0];

                if (split.length == 3 && "register".equals(operation)) {
                    final long timestamp = Long.parseLong(split[1]);
                    final String hashedCardNumber = split[2];
                    final Payment payment = new Payment(UUID.randomUUID().toString(), Instant.ofEpochMilli(timestamp),
                            hashedCardNumber);

                    velocityProvider.registerPayment(payment);
                } else if (split.length == 4 && "get".equals(operation)) {
                    final long queryTime = Long.parseLong(split[1]);
                    final String hashedCardNumber = split[2];
                    final long durationInSeconds = Long.parseLong(split[3]);
                    System.out.println(velocityProvider.getCardUsageCount(new Payment(UUID.randomUUID().toString(),
                            Instant.ofEpochMilli(queryTime), hashedCardNumber), Duration.ofSeconds(durationInSeconds)));
                } else {
                    throw new RuntimeException("Invalid test input");
                }
            }
        }
    }
}