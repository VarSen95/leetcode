import java.util.*;
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
         *   private final String merchantId;
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
         * It returns how many times the card in the Payment has been seen in the last minutes/seconds/hours as
         * defined in the {@code duration} parameter at the time of the payment processing start.
         *
         * @param payment  The payment being processed
         * @param duration The interval to count
         * @return The number of times the card was used in the interval defined in duration.
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
     * - Scalability: Thread-safe, efficient algorithms, configurable memory management
     * 
     * MERCHANT-SPECIFIC CONFIGURATION PATTERN (when needed):
     * To support per-merchant retention policies:
     * 
     * 1. Add merchantId to Payment class
     * 2. Create MerchantConfigService:
     *    interface MerchantConfigService {
     *        VelocityProviderConfig getConfig(String merchantId);
     *    }
     * 3. Store merchant configs:
     *    Map<String, VelocityProviderConfig> merchantConfigs = new ConcurrentHashMap<>();
     * 4. At query/registration time:
     *    VelocityProviderConfig config = merchantConfigs.get(payment.getMerchantId());
     *    
     * This allows different merchants to have:
     * - Different cleanup thresholds (e.g., high-risk merchants keep 30 days, others 7 days)
     * - Different cleanup frequencies
     * - While keeping rule durations (velocity windows) external as query parameters
     * 
     * SCALABILITY SUMMARY:
     * - Per-request cost: O(log n + k) per card query
     * - Memory: Bounded by cleanup threshold × payment volume
     * - Thread-safe: Safe for concurrent payment processing
     * - Sharding ready: Easy to distribute by card hash or merchantId
     * - 20x scale: partition by card hash, keep per-card state local, and bound history with cleanup
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
                config.getCleanupProbability()
            );
        }

        @Override
        public int getCardUsageCount(Payment payment, Duration duration) {
            validatePayment(payment);
            validateDuration(duration);
            
            return timestampStorage.countInWindow(
                payment.getHashedCardNumber(),
                payment.getTimestamp(),
                duration
            );
        }

        @Override
        public void registerPayment(Payment payment) {
            validatePayment(payment);
            
            timestampStorage.addTimestamp(
                payment.getHashedCardNumber(),
                payment.getTimestamp()
            );
            
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
            if (duration == null || duration.isNegative()) {
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
     * Allows different implementations for different use cases (in-memory, distributed, etc.)
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
     * Uses TreeMap<Long, Integer> to track counts per timestamp, properly handling duplicates.
     * Optimized for time-range queries with O(log n) complexity.
     * 
     * SCALABILITY NOTES:
     * - Per-request cost: O(log n + k) where n = timestamps per card, k = results in window
     * - Memory bounded by cleanup threshold (default 7 days)
     * - Thread-safe: ConcurrentHashMap + per-card TreeMap synchronization
     * 
     * SHARDING STRATEGY:
     * - Current: Keyed by hashedCardNumber for natural distribution
     * - Easy to shard: Hash(cardNumber) % numShards → shard assignment
     * - For merchant-specific: Use composite key (merchantId + cardHash)
     * - Horizontal scaling: Replace with Redis/Cassandra via TimestampStorage interface
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
        private volatile Long oldestTimestampMillis;
        
        public TreeMapTimestampStorage() {
            this.cardTimestamps = new java.util.concurrent.ConcurrentHashMap<>();
            this.oldestTimestampMillis = System.currentTimeMillis();
        }
        
        @Override
        public void addTimestamp(String cardHash, Instant timestamp) {
            TreeMap<Long, Integer> timestamps = cardTimestamps.computeIfAbsent(
                cardHash,
                k -> new TreeMap<>()
            );
            
            long epochMilli = timestamp.toEpochMilli();
            
            // Synchronize on the same timestamps object as countInWindow
            synchronized (timestamps) {
                // Increment count for this timestamp (handles duplicates)
                timestamps.merge(epochMilli, 1, Integer::sum); // FOLLOW UP: O(logn)
            }
            
            // Update oldest timestamp tracking (atomic operation on volatile field)
            if (epochMilli < oldestTimestampMillis) {
                oldestTimestampMillis = epochMilli;
            }
        }
        
        @Override
        public int countInWindow(String cardHash, Instant queryTime, Duration duration) {
            TreeMap<Long, Integer> timestamps = cardTimestamps.get(cardHash);
            
            if (timestamps == null || timestamps.isEmpty()) {
                return 0;
            }
            
            long windowStartMillis = queryTime.minus(duration).toEpochMilli();
            long queryTimeMillis = queryTime.toEpochMilli();
            
            // Use TreeMap's subMap for efficient range query: O(log n + k)
            // where k is the number of unique timestamps in the result
            // Sum all counts within the time window
            synchronized (timestamps) {
                return timestamps.subMap(windowStartMillis, true, queryTimeMillis, true)
                    .values()
                    .stream()
                    .mapToInt(Integer::intValue)
                    .sum();
            }
        }
        
        @Override
        public synchronized void removeOlderThan(Instant cutoffTime) {
            long cutoffMillis = cutoffTime.toEpochMilli();
            
            if (oldestTimestampMillis > cutoffMillis) {
                return; // No data old enough to clean
            }
            
            Long newOldest = System.currentTimeMillis();
            
            // Clean old timestamps from each card's TreeMap
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
            
            oldestTimestampMillis = newOldest;
        }
        
        @Override
        public Instant getOldestTimestamp() {
            return Instant.ofEpochMilli(oldestTimestampMillis);
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
         * @param threshold Duration after which data should be cleaned
         * @param probability Probability of cleanup per registration (0.0 to 1.0)
         * @param random Random instance (can be seeded for tests)
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
         * @param threshold Duration after which data should be cleaned
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
                    final Payment payment = new Payment(UUID.randomUUID().toString(), Instant.ofEpochMilli(timestamp), hashedCardNumber);

                    velocityProvider.registerPayment(payment);
                } else if (split.length == 4 &&  "get".equals(operation)) {
                    final long queryTime = Long.parseLong(split[1]);
                    final String hashedCardNumber = split[2];
                    final long durationInSeconds = Long.parseLong(split[3]);
                    System.out.println(velocityProvider.getCardUsageCount(new Payment(UUID.randomUUID().toString(), Instant.ofEpochMilli(queryTime), hashedCardNumber), Duration.ofSeconds(durationInSeconds)));
                } else {
                    throw new RuntimeException("Invalid test input");
                }
            }
        }
    }
}
