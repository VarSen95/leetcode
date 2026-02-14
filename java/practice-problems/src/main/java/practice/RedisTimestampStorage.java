package practice;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Redis-backed implementation of {@link Solution.TimestampStorage}.
 *
 * Uses Redis Sorted Sets — essentially a TreeMap in the cloud.
 * One sorted set per card: key = "velocity:{cardHash}"
 * Each entry: score = epoch millis (timestamp), member = unique payment ID
 *
 * Maps to in-memory implementation:
 * TreeMap.merge() → ZADD (insert with score)
 * TreeMap.subMap().sum() → ZCOUNT (count entries in score range)
 * headMap().clear() → TTL auto-expiry (no manual cleanup needed)
 *
 * Why Redis Sorted Sets:
 * - O(log n) for ZADD and ZCOUNT — same as TreeMap
 * - Single-threaded — no synchronized blocks, no race conditions
 * - Distributed — multiple JVMs share the same data
 * - Built-in TTL — replaces entire ProbabilisticCleanupStrategy
 *
 * Why UUID as member:
 * Redis sorted sets require unique members. If two payments arrive
 * at the same millisecond, using epochMillis alone would overwrite
 * the first entry. UUID ensures both are stored. ZCOUNT counts both.
 *
 * Trade-off vs in-memory:
 * In-memory: ~1μs per operation, single JVM, no durability
 * Redis: ~500μs per operation (network), distributed, durable
 * For fraud detection, 500μs is well within budget.
 *
 * Production pairing:
 * Redis = real-time velocity queries (fast reads)
 * PostgreSQL = system of record (durable, ACID, audit trail)
 * Write to both. Read from Redis. Fall back to Postgres if Redis is down.
 */
public class RedisTimestampStorage implements Solution.TimestampStorage {

    // All keys prefixed with "velocity:" for namespace isolation
    // e.g., "velocity:abc123" for card hash "abc123"
    private static final String KEY_PREFIX = "velocity:";

    // JedisPool manages a pool of Redis connections
    // try-with-resources auto-returns connections to the pool
    private final JedisPool pool;

    public RedisTimestampStorage(JedisPool pool) {
        this.pool = pool;
    }

    @Override
    public void addTimestamp(String cardHash, Instant timestamp) {
        String key = KEY_PREFIX + cardHash;
        long epochMilli = timestamp.toEpochMilli();

        // UUID ensures uniqueness — two payments at same millisecond
        // become two separate members with the same score.
        // Without UUID: second ZADD overwrites the first → wrong count
        String member = epochMilli + ":" + UUID.randomUUID().toString();

        try (Jedis jedis = pool.getResource()) {
            // ZADD velocity:card123 1707000200 "1707000200:uuid-abc"
            // Adds member with timestamp as score
            // Sorted set stays ordered by score automatically
            jedis.zadd(key, epochMilli, member);

            // TODO: Add jedis.expire(key, ttlSeconds) here
            // This would replace the entire cleanup strategy —
            // Redis auto-deletes the key after ttlSeconds.
            // Requires adding ttlSeconds as a constructor parameter.
        }
    }

    @Override
    public int countInWindow(String cardHash, Instant queryTime, Duration duration) {
        String key = KEY_PREFIX + cardHash;
        long start = queryTime.minus(duration).toEpochMilli();
        long end = queryTime.toEpochMilli();

        try (Jedis jedis = pool.getResource()) {
            // ZCOUNT velocity:card123 1707000000 1707000600
            // Counts all members with score between start and end (inclusive)
            // Uses skip list internally — O(log n), does NOT iterate all entries
            // Equivalent to: TreeMap.subMap(start, end).values().stream().sum()
            return jedis.zcount(key, start, end).intValue();
        }
    }

    @Override
    public void removeOlderThan(Instant cutoffTime) {
        // No-op: TTL on each key handles cleanup automatically.
        // Redis deletes the entire sorted set when TTL expires.
        //
        // Without TTL, we'd need:
        // SCAN to find all velocity:* keys
        // ZREMRANGEBYSCORE per key to remove old entries
        // That's expensive at scale — TTL is simpler and free.
    }

    @Override
    public Instant getOldestTimestamp() {
        // Not meaningful with TTL-based cleanup.
        // This method exists in the interface because the in-memory
        // implementation uses it as an optimization to skip cleanup
        // when no data is old enough.
        //
        // With Redis TTL, there's no cleanup to skip.
        // Return EPOCH so if cleanup is called, it runs —
        // but removeOlderThan is a no-op anyway.
        //
        // Ideally this method should be removed from the interface —
        // it's a cleanup optimization that leaked into the storage contract.
        return Instant.EPOCH;
    }
}