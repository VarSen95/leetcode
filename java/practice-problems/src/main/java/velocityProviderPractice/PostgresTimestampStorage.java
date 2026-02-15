package practice;

import java.time.Duration;
import java.time.Instant;
import java.sql.*;
import javax.sql.DataSource;

/**
 * PostgreSQL-backed implementation of {@link Solution.TimestampStorage}.
 *
 * Role in the architecture:
 *   PostgreSQL = system of record (durable, strongly consistent, ACID)
 *   Redis      = real-time query layer (fast, ephemeral)
 *   Write to both. Read from Redis first. Fall back to Postgres if Redis is down.
 *
 * Schema:
 * ┌─────────────────────────────────────────────────────────────┐
 * │ card_payments                                               │
 * ├─────────────────────────────────────────────────────────────┤
 * │ id           BIGSERIAL PRIMARY KEY                          │
 * │ card_hash    VARCHAR(64) NOT NULL     -- hashed card number │
 * │ payment_id   VARCHAR(36) NOT NULL     -- UUID from Payment  │
 * │ timestamp    TIMESTAMPTZ NOT NULL     -- payment time       │
 * │ created_at   TIMESTAMPTZ DEFAULT NOW()-- when row inserted  │
 * ├─────────────────────────────────────────────────────────────┤
 * │ INDEXES:                                                    │
 * │ idx_card_timestamp ON (card_hash, timestamp DESC)           │
 * │ idx_payment_id ON (payment_id) UNIQUE                      │
 * └─────────────────────────────────────────────────────────────┘
 *
 * Why this schema:
 *   - card_hash: NOT the raw card number — hashed for security/PCI compliance
 *   - payment_id: unique constraint prevents duplicate registrations
 *     (idempotent — same payment registered twice doesn't double-count)
 *   - timestamp: TIMESTAMPTZ not TIMESTAMP — timezone-aware, critical for
 *     a global payment system where payments come from different timezones
 *   - created_at: audit trail — when did WE record this, vs when payment happened
 *   - BIGSERIAL id: auto-incrementing, good for range scans and partitioning
 *
 * Why the index on (card_hash, timestamp DESC):
 *   The velocity query is:
 *     SELECT COUNT(*) WHERE card_hash = ? AND timestamp BETWEEN ? AND ?
 *
 *   With this composite index, Postgres:
 *     1. Finds the card_hash in the B-tree → O(log n)
 *     2. Walks timestamps in DESC order within that card
 *     3. Stops when it exits the time window
 *     → Index-only scan. Never touches the table. Fast.
 *
 *   Without the index: full table scan → O(n) over millions of rows. Disaster.
 *
 * Partitioning strategy (for scale):
 *   PARTITION BY RANGE (timestamp)
 *   One partition per month: card_payments_2025_01, card_payments_2025_02, etc.
 *
 *   Benefits:
 *     - Velocity queries only scan current partition (last 24 hours of data)
 *     - Old partitions: DROP TABLE instead of DELETE → instant, no vacuum
 *     - Archive old months to cold storage for compliance
 *
 *   CREATE TABLE card_payments (
 *       id           BIGSERIAL,
 *       card_hash    VARCHAR(64) NOT NULL,
 *       payment_id   VARCHAR(36) NOT NULL,
 *       timestamp    TIMESTAMPTZ NOT NULL,
 *       created_at   TIMESTAMPTZ DEFAULT NOW(),
 *       PRIMARY KEY (id, timestamp)  -- timestamp needed in PK for partitioning
 *   ) PARTITION BY RANGE (timestamp);
 *
 *   CREATE TABLE card_payments_2025_02
 *       PARTITION OF card_payments
 *       FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');
 *
 * Performance characteristics:
 *   INSERT:        ~1-2ms (network + write-ahead log)
 *   COUNT query:   ~1-5ms (index-only scan)
 *   vs Redis:      ~0.5ms
 *   vs in-memory:  ~0.001ms
 *
 *   Postgres is slower than Redis but provides:
 *     - Strong consistency (reads always reflect latest writes)
 *     - ACID guarantees (no phantom reads during velocity checks)
 *     - Durability (survives crashes, restarts, Redis failures)
 *     - Audit trail (compliance, forensics)
 *     - Complex queries (joins, aggregations for analytics)
 *
 * Thread safety:
 *   Postgres handles concurrency internally — no synchronized blocks needed.
 *   Connection pool (DataSource) manages concurrent access.
 *   Each thread gets its own connection from the pool.
 */
public class PostgresTimestampStorage implements Solution.TimestampStorage {

    private final DataSource dataSource;

    public PostgresTimestampStorage(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Schema creation — run once at startup or via migration tool (Flyway/Liquibase).
     *
     * In production, you'd never create tables in application code.
     * This is here for documentation / interview discussion only.
     */
    public void createSchema() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS card_payments (
                    id           BIGSERIAL PRIMARY KEY,
                    card_hash    VARCHAR(64) NOT NULL,
                    payment_id   VARCHAR(36) NOT NULL UNIQUE,
                    timestamp    TIMESTAMPTZ NOT NULL,
                    created_at   TIMESTAMPTZ DEFAULT NOW()
                )
            """);

            // THE critical index for velocity queries
            // Composite: (card_hash, timestamp DESC)
            //
            // Why DESC on timestamp?
            //   Velocity queries ask "count in the LAST 10 minutes"
            //   Most recent data is accessed most often
            //   DESC ordering means Postgres reads the hot data first
            //
            // This enables index-only scans:
            //   Postgres answers COUNT(*) directly from the index
            //   without touching the table (heap) at all
            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_card_timestamp 
                ON card_payments(card_hash, timestamp DESC)
            """);

            // Unique index on payment_id for idempotency
            // If the same payment is registered twice (e.g., retry after timeout),
            // the second INSERT fails instead of double-counting
            // Already covered by UNIQUE constraint above, but explicit for clarity
        }
    }

    @Override
    public void addTimestamp(String cardHash, Instant timestamp) {
        // INSERT INTO card_payments (card_hash, payment_id, timestamp)
        // VALUES ('abc123', 'uuid-xxx', '2025-02-11T10:30:00Z')
        //
        // ON CONFLICT (payment_id) DO NOTHING:
        //   Idempotent — if this payment was already registered
        //   (e.g., retry after network timeout), don't insert again.
        //   Without this: duplicate rows → inflated velocity count → 
        //   legitimate cards blocked as fraud. Bad.
        //
        // Note: We don't have payment_id in the TimestampStorage interface.
        // In a real implementation, I'd extend the interface or pass Payment directly.
        // Using cardHash + timestamp as a workaround here.
        String sql = """
            INSERT INTO card_payments (card_hash, payment_id, timestamp) 
            VALUES (?, ?, ?)
            ON CONFLICT (payment_id) DO NOTHING
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cardHash);
            ps.setString(2, cardHash + ":" + timestamp.toEpochMilli()); // workaround — ideally payment_id
            ps.setTimestamp(3, Timestamp.from(timestamp));
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to register payment", e);
        }
    }

    @Override
    public int countInWindow(String cardHash, Instant queryTime, Duration duration) {
        // SELECT COUNT(*) FROM card_payments
        // WHERE card_hash = 'abc123'
        //   AND timestamp BETWEEN '2025-02-11T10:20:00Z' AND '2025-02-11T10:30:00Z'
        //
        // How Postgres executes this with our index:
        //   1. B-tree lookup on card_hash = 'abc123' → O(log n)
        //   2. Walk index entries where timestamp is in range
        //   3. Count them → no table access needed (index-only scan)
        //   4. Return count
        //
        // BETWEEN is inclusive on both ends — same as TreeMap.subMap(start, true, end, true)
        //
        // Performance: ~1-5ms depending on how many entries are in the window
        // vs Redis ZCOUNT: ~0.5ms
        // vs TreeMap.subMap: ~0.001ms
        String sql = """
            SELECT COUNT(*) 
            FROM card_payments 
            WHERE card_hash = ? 
              AND timestamp BETWEEN ? AND ?
        """;

        Instant windowStart = queryTime.minus(duration);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cardHash);
            ps.setTimestamp(2, Timestamp.from(windowStart));
            ps.setTimestamp(3, Timestamp.from(queryTime));

            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to count payments", e);
        }
    }

    @Override
    public void removeOlderThan(Instant cutoffTime) {
        // DELETE FROM card_payments WHERE timestamp < '2025-02-04T10:30:00Z'
        //
        // With partitioning, this becomes:
        //   DROP TABLE card_payments_2025_01;
        //   Much faster — instant vs scanning and deleting millions of rows
        //   No vacuum needed after DROP
        //
        // Without partitioning: DELETE is O(n) and creates dead tuples
        //   that need VACUUM. At scale, this is a problem.
        //
        // In production: scheduled job runs this nightly, not probabilistic.
        //   The ProbabilisticCleanupStrategy is an in-memory optimization.
        //   For Postgres, a cron job or pg_cron is more appropriate.
        String sql = "DELETE FROM card_payments WHERE timestamp < ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.from(cutoffTime));
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to cleanup old payments", e);
        }
    }

    @Override
    public Instant getOldestTimestamp() {
        // SELECT MIN(timestamp) FROM card_payments
        //
        // With the index on (card_hash, timestamp DESC), this isn't optimal.
        // A separate index on (timestamp ASC) would make MIN() instant.
        // Or: just check the oldest partition's boundary.
        //
        // In practice, this is only called to decide whether cleanup is needed.
        // With scheduled cleanup (cron), this method isn't necessary.
        String sql = "SELECT MIN(timestamp) FROM card_payments";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getTimestamp(1) != null) {
                return rs.getTimestamp(1).toInstant();
            }
            return Instant.EPOCH;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to get oldest timestamp", e);
        }
    }
}