package bankingPractice;

/**
 * Simple in-memory transaction service backed by AccountDao. Synchronization is
 * coarse-grained and only safe within a single JVM.
 */
public class InMemoryTransactionService implements TransactionService {

    private final AccountDao accountDao;

    public InMemoryTransactionService(AccountDao accountDao) {
        this.accountDao = accountDao;
    }

    @Override
    public synchronized void credit(Long accountNumber, double amount) {
        AccountDTO account = accountDao.findById(accountNumber);
        if (account == null || amount <= 0) {
            return;
        }
        accountDao.save(new AccountDTO(accountNumber, account.getHolderName(), account.getPin(),
                account.getBalance() + amount));
    }

    @Override
    public synchronized boolean debit(Long accountNumber, double amount) {
        AccountDTO account = accountDao.findById(accountNumber);
        if (account == null || amount <= 0) {
            return false;
        }
        if (account.getBalance() < amount) {
            return false;
        }
        accountDao.save(new AccountDTO(accountNumber, account.getHolderName(), account.getPin(),
                account.getBalance() - amount));
        return true;
    }

    @Override
    public synchronized boolean transfer(Long sourceAccount, Long destinationAccount, double amount) {
        if (sourceAccount == null || destinationAccount == null || amount <= 0) {
            return false;
        }

        AccountDTO source = accountDao.findById(sourceAccount);
        AccountDTO dest = accountDao.findById(destinationAccount);

        if (source == null || dest == null || source.getBalance() < amount) {
            return false;
        }

        accountDao.save(new AccountDTO(sourceAccount, source.getHolderName(), source.getPin(),
                source.getBalance() - amount));
        accountDao.save(new AccountDTO(destinationAccount, dest.getHolderName(), dest.getPin(),
                dest.getBalance() + amount));
        return true;
    }
}
