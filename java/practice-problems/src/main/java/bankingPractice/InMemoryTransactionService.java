package bankingPractice;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Simple in-memory transaction service backed by AccountDao. Synchronization is
 * coarse-grained and only safe within a single JVM.
 */
public class InMemoryTransactionService implements TransactionService {

    private final AccountDao accountDao;
    private final Map<Long, Lock> accountLocks = new ConcurrentHashMap<>();

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
    public boolean transfer(Long sourceAccount, Long destinationAccount, double amount) {
        if (sourceAccount == null || destinationAccount == null || amount <= 0) {
            return false;
        }

        // Acquire per-account locks in deterministic order to avoid deadlock
        Long first = sourceAccount < destinationAccount ? sourceAccount : destinationAccount;
        Long second = sourceAccount < destinationAccount ? destinationAccount : sourceAccount;

        Lock firstLock = accountLocks.computeIfAbsent(first, k -> new ReentrantLock());
        Lock secondLock = accountLocks.computeIfAbsent(second, k -> new ReentrantLock());

        firstLock.lock();
        secondLock.lock();

        try {
            AccountDTO source = accountDao.findById(sourceAccount);
            AccountDTO dest = accountDao.findById(destinationAccount);

            if (source == null || dest == null || source.getBalance() < amount) {
                return false;
            }

            // Save originals for rollback in case second write fails
            AccountDTO originalSource = source;
            AccountDTO originalDest = dest;

            try {
                accountDao.save(new AccountDTO(sourceAccount, source.getHolderName(), source.getPin(),
                        source.getBalance() - amount));
                accountDao.save(new AccountDTO(destinationAccount, dest.getHolderName(), dest.getPin(),
                        dest.getBalance() + amount));
                return true;
            } catch (RuntimeException e) {
                // Roll back to original state to maintain atomicity in-memory
                accountDao.save(originalSource);
                accountDao.save(originalDest);
                return false;
            }
        } finally {
            secondLock.unlock();
            firstLock.unlock();
        }
    }
}
