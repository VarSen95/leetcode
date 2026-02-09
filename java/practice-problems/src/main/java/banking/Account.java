package banking;

/**
 * Abstract bank account class.<br>
 * <br>
 * <p>
 * Private Variables:<br>
 * {@link #accountHolder}: AccountHolder<br>
 * {@link #accountNumber}: Long<br>
 * {@link #pin}: int<br>
 * {@link #balance}: double<br><br>
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
    private Currency currency;

    protected Account(AccountHolder accountHolder, Long accountNumber, int pin, double startingDeposit, Currency currency) {
        this.accountHolder = accountHolder;
        this.accountNumber = accountNumber;
        this.pin = pin;
        this.balance = startingDeposit;
        this.currency = currency;
    }

    public AccountHolder getAccountHolder() {
        return accountHolder;
    }

    public boolean validatePin(int attemptedPin) {
        return this.pin == attemptedPin;
    }

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

    public synchronized boolean debitAccount(double amount) {
        if (amount > 0 && balance >= amount) {
            balance -= amount;
            return true;
        }
        return false;
    }

    public Currency getCurrency() {
        return this.currency;
    }
}
