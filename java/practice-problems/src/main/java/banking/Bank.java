package banking;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Private Variables:<br>
 * {@link #accounts}: List&lt;Long, Account&gt;<br><br>
 *
 * Scalability/Extensibility notes:
 * - In-memory storage is fast but not durable; replace with a repository if persistence is required.
 * - AtomicLong is safe within a single JVM; use a distributed ID generator for multi-node deployments.
 */
public class Bank implements BankInterface {
    private final Map<Long, Account> accounts;
    private static final AtomicLong accountNumberGenerator = new AtomicLong(1000000L);

    public Bank() {
        this.accounts = new ConcurrentHashMap<>();
    }

    public Account getAccount(Long accountNumber) {
        return accounts.get(accountNumber);
    }

    public Long openCommercialAccount(Company company, int pin, double startingDeposit) {
        Long accountNumber = accountNumberGenerator.getAndIncrement();
        Account account = new CommercialAccount(company, accountNumber, pin, startingDeposit);
        accounts.put(accountNumber, account);
        return accountNumber;
    }

    public Long openConsumerAccount(Person person, int pin, double startingDeposit) {
        Long accountNumber = accountNumberGenerator.getAndIncrement();
        Account account = new ConsumerAccount(person, accountNumber, pin, startingDeposit);
        accounts.put(accountNumber, account);
        return accountNumber;
    }

    public boolean authenticateUser(Long accountNumber, int pin) {
        Account account = getAccount(accountNumber);
        if (account == null) {
            return false;
        }
        return account.validatePin(pin);
    }

    public double getBalance(Long accountNumber) {
        Account account = getAccount(accountNumber);
        if (account == null) {
            return -1;
        }
        return account.getBalance();
    }

    public void credit(Long accountNumber, double amount) {
        Account account = getAccount(accountNumber);
        if (account == null) {
            return;
        }
        account.creditAccount(amount);
    }

    public boolean debit(Long accountNumber, double amount) {
        Account account = getAccount(accountNumber);
        if (account == null) {
            return false;
        }
        return account.debitAccount(amount);
    }
}
