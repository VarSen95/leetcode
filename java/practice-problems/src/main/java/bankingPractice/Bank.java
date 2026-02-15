package bankingPractice;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Private Variables:<br>
 * {@link #accounts}: List&lt;Long, Account&gt;<br>
 * <br>
 *
 * Scalability/Extensibility notes:
 * - In-memory storage is fast but not durable; replace with a repository if
 * persistence is required.
 * - AtomicLong is safe within a single JVM; use a distributed ID generator for
 * multi-node deployments.
 */
public class Bank implements BankInterface {
    // DISTRIBUTED ENVIRONMENT — DOUBLE SPEND PROBLEM:
    // In a distributed environment, two servers could both read balance = $500
    // and both approve a $400 debit — that's a double-spend.
    // My synchronized blocks only protect within one JVM, not across servers.
    //
    // FIX 1: Create an AccountDao interface with save, findById, findAll methods,
    // and implement it with PostgreSQL instead of the in-memory ConcurrentHashMap.
    // This follows the Strategy pattern — the Bank class depends on an interface,
    // so the storage is swappable.
    //
    // FIX 2: Instead of checking balance in Java, use a single atomic SQL:
    // UPDATE accounts SET balance = balance - 400 WHERE balance >= 400
    // The database handles concurrency — no Java-level locking needed.
    private final AccountDao accountDao;
    private final AuthorisationService authorisationService;
    private final TransactionService transactionService;
    private static final AtomicLong accountNumberGenerator = new AtomicLong();

    public Bank() {
        this(new InMemoryAccountDao(), null);
    }

    public Bank(AccountDao accountDao) {
        this(accountDao, null);
    }

    public Bank(AccountDao accountDao, TransactionService transactionService) {
        this.accountDao = accountDao;
        this.authorisationService = new AuthorisationService(accountDao);
        this.transactionService = transactionService == null
                ? new InMemoryTransactionService(accountDao)
                : transactionService;
    }

    public AccountDTO getAccount(Long accountNumber) {
        return accountDao.findById(accountNumber);
    }

    public Long openCommercialAccount(Company company, int pin, double startingDeposit) {
        Long accountNumber = accountNumberGenerator.getAndIncrement();
        String holderName = company.getCompanyName();
        accountDao.save(new AccountDTO(accountNumber, holderName, pin, startingDeposit));
        return accountNumber;
    }

    public Long openConsumerAccount(Person person, int pin, double startingDeposit) {
        Long accountNumber = accountNumberGenerator.getAndIncrement();
        String holderName = person.getFirstName() + " " + person.getLastName();
        accountDao.save(new AccountDTO(accountNumber, holderName, pin, startingDeposit));
        return accountNumber;
    }

    public boolean authenticateUser(Long accountNumber, int pin) {
        return authorisationService.authenticate(accountNumber, pin);
    }

    public synchronized double getBalance(Long accountNumber) {
        AccountDTO account = getAccount(accountNumber);
        return account == null ? -1 : account.getBalance();
    }

    public synchronized void credit(Long accountNumber, double amount) {
        transactionService.credit(accountNumber, amount);
    }

    public synchronized boolean debit(Long accountNumber, double amount) {
        return transactionService.debit(accountNumber, amount);
    }

    public synchronized boolean transfer(Long sourceAccount, Long destinationAccount, double amount) {
        return transactionService.transfer(sourceAccount, destinationAccount, amount);
    }
}
