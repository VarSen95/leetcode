package banking;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    private final Map<Long, Account> accounts;
    private static final AtomicLong accountNumberGenerator = new AtomicLong(1000000L);
    // FOLLOW UP: Currency service
    private CurrencyService currencyService;

    public Bank(ExchangeRateService exchangeRateService) {
        this.accounts = new ConcurrentHashMap<>();
        this.currencyService = new CurrencyService(exchangeRateService);
    }

    public Account getAccount(Long accountNumber) {
        return accounts.get(accountNumber);
    }

    public Long openCommercialAccount(Company company, int pin, double startingDeposit, Currency currency) {
        Long accountNumber = accountNumberGenerator.getAndIncrement();
        Account account = new CommercialAccount(company, accountNumber, pin, startingDeposit, currency);
        accounts.put(accountNumber, account);
        return accountNumber;
    }

    public Long openConsumerAccount(Person person, int pin, double startingDeposit, Currency currency) {
        Long accountNumber = accountNumberGenerator.getAndIncrement();
        Account account = new ConsumerAccount(person, accountNumber, pin, startingDeposit, currency);
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
            throw new AccountNotFoundException("Account not found"); // FOLLOW UP: Error handling
        }
        return account.getBalance();
    }

    public void credit(Long accountNumber, double amount) {
        Account account = getAccount(accountNumber);
        if (account == null) {
            throw new AccountNotFoundException(
                    "Account not found while crediting. Please entre a valid account number"); // FOLLOW UP: Error
                                                                                               // handling
        }
        account.creditAccount(amount);
    }

    public boolean debit(Long accountNumber, double amount) {
        Account account = getAccount(accountNumber);
        if (account == null) {
            throw new AccountNotFoundException(
                    "Account not found while debiting. Please entre a valid account number"); // FOLLOW UP: Error
                                                                                              // handling
        }
        return account.debitAccount(amount);
    }

    public boolean transfer(Long sourceAccountNumber, Long destinationAccountNumber, double amount) {
        if (amount <= 0 || sourceAccountNumber == null || destinationAccountNumber == null) {
            return false;
        }
        if (sourceAccountNumber.equals(destinationAccountNumber)) {
            return true;
        }

        Account sourceAccount = getAccount(sourceAccountNumber);
        Account destinationAccount = getAccount(destinationAccountNumber);
        if (sourceAccount == null || destinationAccount == null) {
            return false;
        }

        Currency sourceCurrency = sourceAccount.getCurrency();
        Currency targetCurrency = destinationAccount.getCurrency();

        double destinationAmountConvertedToCurrency = this.currencyService.convert(amount, sourceCurrency, targetCurrency);

        Account firstLock = sourceAccountNumber < destinationAccountNumber ? sourceAccount : destinationAccount;
        Account secondLock = sourceAccountNumber < destinationAccountNumber ? destinationAccount : sourceAccount;

        // FOLLOW UP: lock both accounts in a fixed order to avoid deadlocks during
        // transfer.
        synchronized (firstLock) {
            synchronized (secondLock) {
                if (!sourceAccount.debitAccount(amount)) {
                    return false;
                }
                try {
                    destinationAccount.creditAccount(destinationAmountConvertedToCurrency);
                } catch (Exception e) {
                    // FOLLOW UP: Handle exception during credit operation and rollback.
                    // Rollback
                    sourceAccount.creditAccount(amount);
                    throw new IllegalArgumentException("Transfer failed: . Rolling back" + e.getMessage());
                }
                return true;
            }
        }
    }
}
