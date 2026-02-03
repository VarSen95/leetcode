# VelocityProvider Architecture Documentation

## Overview

The VelocityProvider is a fraud detection system designed to track card usage velocity (frequency) within configurable time windows. This document explains the architectural decisions, design patterns, and extensibility points.

## Design Principles

### 1. Maintainability
- **Separation of Concerns**: Each class has a single, well-defined responsibility
- **Clear Naming**: Descriptive class and method names that reflect intent
- **Comprehensive Documentation**: Every public interface and class is documented
- **Fail-Fast Validation**: Input validation catches errors early

### 2. Readability
- **Logical Structure**: Code flows naturally from abstract to concrete
- **Consistent Patterns**: Builder pattern for configuration, Strategy pattern for algorithms
- **Clear Interfaces**: Simple, intuitive method signatures
- **Explanatory Comments**: Complex logic is explained inline

### 3. Extensibility
- **Strategy Pattern**: Pluggable storage and cleanup strategies
- **Interface-Based Design**: Easy to swap implementations
- **Configuration System**: Tunable parameters without code changes
- **Open/Closed Principle**: Open for extension, closed for modification

### 4. Scalability
- **Efficient Algorithms**: O(log n) operations for time-window queries
- **Thread-Safety**: Proper synchronization for concurrent access
- **Memory Management**: Automatic cleanup prevents unbounded growth
- **Distributed-Ready**: Interfaces support future distributed implementations

## Architecture Components

```
VelocityProvider (Interface)
    ↓
VelocityProviderImpl
    ├── TimestampStorage (Interface)
    │   └── TreeSetTimestampStorage (Implementation)
    ├── CleanupStrategy (Interface)
    │   └── ProbabilisticCleanupStrategy (Implementation)
    └── VelocityProviderConfig
        └── Builder (Inner Class)
```

### Core Components

#### 1. VelocityProvider (Interface)
**Purpose**: Public API for card velocity checking

**Key Methods**:
- `getCardUsageCount(Payment, Duration)`: Query card usage in time window
- `registerPayment(Payment)`: Record a new payment
- `getProvider()`: Factory method for creating instances

**Why Interface**: Allows for multiple implementations (in-memory, distributed, mock for testing)

#### 2. VelocityProviderImpl
**Purpose**: Coordinates storage and cleanup strategies

**Responsibilities**:
- Input validation
- Delegation to storage and cleanup components
- Configuration management

**Why This Design**: Single Responsibility - orchestrates but doesn't implement storage/cleanup logic

#### 3. TimestampStorage (Interface)
**Purpose**: Abstract storage mechanism for payment timestamps

**Key Methods**:
- `addTimestamp(String, Instant)`: Store a timestamp
- `countInWindow(String, Instant, Duration)`: Count timestamps in window
- `removeOlderThan(Instant)`: Cleanup old data
- `getOldestTimestamp()`: Track oldest data point

**Why Interface**: Allows different storage backends without changing core logic

#### 4. TreeSetTimestampStorage
**Purpose**: In-memory storage using TreeSet for efficient range queries

**Data Structure**:
- `ConcurrentHashMap<String, TreeSet<Instant>>`: Thread-safe card → timestamps mapping
- TreeSet provides O(log n) insertion and range queries

**Key Features**:
- Thread-safe with synchronized blocks
- Efficient subSet operations for time windows
- Tracks oldest timestamp for cleanup optimization

**Performance Characteristics**:
- Insert: O(log n) per card
- Query: O(log n + k) where k = results in window
- Cleanup: O(m * n) where m = cards, n = timestamps per card

#### 5. CleanupStrategy (Interface)
**Purpose**: Abstract cleanup decision and execution

**Key Methods**:
- `shouldCleanup()`: Decide if cleanup should run
- `cleanup(TimestampStorage, Instant)`: Execute cleanup

**Why Interface**: Different cleanup policies for different scenarios

#### 6. ProbabilisticCleanupStrategy
**Purpose**: Distribute cleanup overhead across multiple operations

**Algorithm**:
- Random selection: Cleanup on ~0.1% of registrations (configurable)
- Removes data older than threshold (default: 24 hours)

**Benefits**:
- No single request bears all cleanup cost
- Amortized O(1) cleanup overhead
- Configurable frequency and retention

#### 7. VelocityProviderConfig
**Purpose**: Centralized configuration with Builder pattern

**Parameters**:
- `cleanupThreshold`: How old data must be before cleanup
- `cleanupProbability`: Frequency of cleanup attempts

**Builder Pattern Benefits**:
- Flexible, readable configuration
- Immutable config objects
- Clear defaults
- Validation at build time

## Performance Characteristics

### Time Complexity
| Operation | Complexity | Notes |
|-----------|------------|-------|
| registerPayment | O(log n) | TreeSet insertion per card |
| getCardUsageCount | O(log n + k) | Binary search + k results |
| cleanup | O(m * n) amortized | Probabilistic distribution |

Where:
- n = timestamps per card
- k = results in time window
- m = number of unique cards

### Space Complexity
- O(m * n) where m = unique cards, n = avg timestamps per card
- Bounded by cleanup threshold (typically 24 hours of data)

### Scalability Considerations

**Current Limits**:
- Single JVM memory bound
- ~10-100 million timestamps feasible with 16GB heap
- Thread-safe for multi-threaded payment processing

**Future Scaling**:
- Implement distributed storage (Redis, Cassandra)
- Shard by card hash for horizontal scaling
- Use time-series databases for better compression

## Extension Points

### 1. Custom Storage Implementation

Implement `TimestampStorage` for different backends:

```java
// Example: Redis-based storage
class RedisTimestampStorage implements TimestampStorage {
    private final JedisPool jedisPool;
    
    @Override
    public void addTimestamp(String cardHash, Instant timestamp) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.zadd("card:" + cardHash, 
                       timestamp.toEpochMilli(), 
                       timestamp.toString());
        }
    }
    
    @Override
    public int countInWindow(String cardHash, Instant queryTime, Duration duration) {
        try (Jedis jedis = jedisPool.getResource()) {
            long min = queryTime.minus(duration).toEpochMilli();
            long max = queryTime.toEpochMilli();
            return jedis.zcount("card:" + cardHash, min, max).intValue();
        }
    }
    
    // Implement other methods...
}
```

### 2. Custom Cleanup Strategy

Implement `CleanupStrategy` for different policies:

```java
// Example: Time-based cleanup (every N minutes)
class ScheduledCleanupStrategy implements CleanupStrategy {
    private Instant lastCleanup;
    private final Duration cleanupInterval;
    
    public ScheduledCleanupStrategy(Duration interval) {
        this.cleanupInterval = interval;
        this.lastCleanup = Instant.now();
    }
    
    @Override
    public boolean shouldCleanup() {
        Instant now = Instant.now();
        if (Duration.between(lastCleanup, now).compareTo(cleanupInterval) >= 0) {
            lastCleanup = now;
            return true;
        }
        return false;
    }
    
    @Override
    public void cleanup(TimestampStorage storage, Instant currentTime) {
        storage.removeOlderThan(currentTime.minus(Duration.ofHours(24)));
    }
}
```

### 3. Custom Configuration

Create specialized configurations for different use cases:

```java
// High-frequency trading scenario - aggressive cleanup
VelocityProviderConfig highFrequencyConfig = new VelocityProviderConfig.Builder()
    .cleanupThreshold(Duration.ofMinutes(30))  // Keep only 30 minutes
    .cleanupProbability(0.01)  // Clean more frequently (1%)
    .build();

// Low-frequency scenario - less aggressive
VelocityProviderConfig lowFrequencyConfig = new VelocityProviderConfig.Builder()
    .cleanupThreshold(Duration.ofDays(7))  // Keep 7 days
    .cleanupProbability(0.0001)  // Clean rarely (0.01%)
    .build();

VelocityProvider provider = new VelocityProviderImpl(highFrequencyConfig);
```

## Testing Strategy

### Unit Tests
- Test each component in isolation
- Mock dependencies (storage, cleanup strategies)
- Test edge cases (null inputs, boundary conditions)
- Verify thread-safety with concurrent tests

### Integration Tests
- Test full system with real storage
- Verify time window calculations
- Test cleanup mechanisms
- Performance benchmarks

### Example Test Scenarios
1. **Basic Functionality**: Register payment, verify count
2. **Time Windows**: Payments inside/outside window
3. **Multiple Cards**: Isolation between cards
4. **Boundary Conditions**: Exact window edges
5. **Thread Safety**: Concurrent registrations and queries
6. **Cleanup**: Verify old data removal
7. **Configuration**: Custom settings work correctly

## Monitoring and Observability

### Key Metrics to Track
1. **Query Latency**: p50, p95, p99 for getCardUsageCount
2. **Registration Latency**: p50, p95, p99 for registerPayment
3. **Memory Usage**: Heap size, timestamp count per card
4. **Cleanup Duration**: Time spent in cleanup operations
5. **False Positive Rate**: Cards incorrectly flagged as fraud

### Logging Strategy
```java
// Add structured logging
private static final Logger logger = LoggerFactory.getLogger(VelocityProviderImpl.class);

@Override
public void registerPayment(Payment payment) {
    long startTime = System.nanoTime();
    try {
        // ... registration logic ...
        
        long duration = System.nanoTime() - startTime;
        if (duration > SLOW_THRESHOLD_NANOS) {
            logger.warn("Slow registration: {}ns for card hash starting with {}", 
                       duration, 
                       payment.getHashedCardNumber().substring(0, 4));
        }
    } catch (Exception e) {
        logger.error("Registration failed for payment {}", payment.getPaymentId(), e);
        throw e;
    }
}
```

## Production Considerations

### Configuration Tuning
- **High Traffic** (>1M payments/day):
  - cleanupProbability: 0.01 (1%)
  - cleanupThreshold: 6-12 hours
  - Consider distributed storage

- **Medium Traffic** (100K-1M payments/day):
  - cleanupProbability: 0.001 (0.1%)
  - cleanupThreshold: 24 hours
  - In-memory storage sufficient

- **Low Traffic** (<100K payments/day):
  - cleanupProbability: 0.0001 (0.01%)
  - cleanupThreshold: 7 days
  - In-memory storage sufficient

### Deployment Checklist
- [ ] Configure appropriate cleanup parameters for traffic volume
- [ ] Set up monitoring and alerting
- [ ] Configure JVM heap size based on expected data volume
- [ ] Test failover scenarios (if using distributed storage)
- [ ] Document operational procedures
- [ ] Set up performance baselines

## Future Enhancements

1. **Distributed Storage**: Redis/Cassandra backend for horizontal scaling
2. **Async Processing**: Non-blocking registration with CompletableFuture
3. **Metrics Export**: Prometheus/Grafana integration
4. **Advanced Cleanup**: LRU-based cleanup for memory pressure
5. **Query Optimization**: Bloom filters for non-existent cards
6. **Compression**: Time-series compression for historical data
7. **Batch Operations**: Bulk registration and queries

## Conclusion

This architecture prioritizes production readiness through clear separation of concerns, extensible design, and operational considerations. The system can handle high throughput while remaining maintainable and adaptable to changing requirements.
