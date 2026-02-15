package bankingPractice;


/**
 * Abstract bank account class.<br>
 * <br>
 * <p>
 * Private Variables:<br>
 * {@link #accountHolder}: AccountHolder<br>
 * {@link #accountNumber}: Long<br>
 * {@link #pin}: int<br>
 * {@link #balance}: double<br>
 * <br>
 *
 * Scalability/Extensibility notes:
 * - Balance operations are synchronized for thread safety within a single JVM.
 * - For higher throughput, consider an external ledger or transactional store.
 */
public abstract class Account implements AccountInterface {
    private AccountHolder accountHolder;
    private Long accountNumber;
    private int pin;
    private double balance;

    protected Account(AccountHolder accountHolder, Long accountNumber, int pin, double startingDeposit) {
        this.accountHolder = accountHolder;
        this.accountNumber = accountNumber;
        this.pin = pin;
        this.balance = startingDeposit;
    }

    public AccountHolder getAccountHolder() {
        return accountHolder;
    }

    public boolean validatePin(int attemptedPin) {
        return this.pin == attemptedPin;
    }

    // I synchronized getBalance for memory visibility, not just mutual exclusion.
    // Without it, a thread could read a stale cached value of balance due to CPU
    // caching. The synchronized keyword establishes a happens-before relationship,
    // guaranteeing the reading thread sees the latest write.

    // For getBalance alone, yes — volatile guarantees visibility. But since
    // debitAccount does a read-then-write (check balance, then subtract), volatile
    // isn't enough there — you still need synchronized for atomicity. So I kept all
    // three synchronized for consistency
    public synchronized double getBalance() {
        return balance;
    }

    public Long getAccountNumber() {
        return accountNumber;
    }

    public synchronized void creditAccount(double amount) {
        if (amount > 0) {
            balance += amount;
        }
    }

    // In a distributed environment, I'd move the balance to PostgreSQL and let the
    // database handle concurrency. A single atomic UPDATE statement — UPDATE
    // accounts SET balance = balance - 400 WHERE balance >= 400 — eliminates the
    // double-spend problem entirely. No Java-level locking needed. The database's
    // ACID guarantees do all the work. My synchronized blocks were the right
    // approach for a single JVM, but in production, the database is the source of
    // truth and the enforcer of consistency."
    public synchronized boolean debitAccount(double amount) {
        if (amount > 0 && balance >= amount) {
            balance -= amount;
            return true;
        }
        return false;
    }
}