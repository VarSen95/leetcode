package bankingPractice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory AccountDao implementation used for tests/demo.
 */
public class InMemoryAccountDao implements AccountDao {

    private final Map<Long, AccountDTO> accounts = new ConcurrentHashMap<>();

    @Override
    public void save(AccountDTO account) {
        accounts.put(account.getAccountNumber(), account);
    }

    @Override
    public AccountDTO findById(Long accountNumber) {
        return accounts.get(accountNumber);
    }

    @Override
    public List<AccountDTO> findAll() {
        return new ArrayList<>(accounts.values());
    }
}
