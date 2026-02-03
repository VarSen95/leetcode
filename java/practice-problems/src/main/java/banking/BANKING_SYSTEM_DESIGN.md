# Banking System Design Notes

## Goals
- Keep core operations fast and predictable under higher load.
- Allow safe concurrent access to accounts.
- Make storage and ID generation replaceable for multi-node deployments.

## Current Design (Single JVM)
- `Bank` stores accounts in a `ConcurrentHashMap<Long, Account>`.
- Account numbers are generated with an `AtomicLong`.
- `Account` synchronizes balance updates (`creditAccount`, `debitAccount`).
- `Transaction` authenticates once at construction and then delegates to `Bank`.

## Scaling to 20x Volume
- **Partitioning:** Key by account number (or hash) to distribute accounts across shards.
- **Isolation:** Per-account synchronization avoids global locks, so throughput grows with number of accounts.
- **Memory bounds:** Keep only required account state in memory; move historical data to a durable store.

## Trade-offs
- **In-memory map** is fast but not durable. A crash loses state.
- **AtomicLong** is safe in one JVM but not unique across nodes.
- **Synchronized balance** ensures correctness but can limit throughput on hot accounts.

## Extension Points
- Replace map with a repository interface (SQL/NoSQL).
- Replace `AtomicLong` with a distributed ID service or database sequence.
- Add auditing/logging for all credits/debits.
- Add account-level policies (overdraft limits, daily withdrawal caps).

## Notes for Distributed Environment
- Use a durable store for account state.
- Use a distributed lock or transactional DB for balance updates.
- Ensure ID uniqueness across nodes (UUID, Snowflake, DB sequence).
