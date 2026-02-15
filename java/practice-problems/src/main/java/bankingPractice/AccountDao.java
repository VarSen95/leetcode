package bankingPractice;

import java.util.List;

public interface AccountDao {
    void save(AccountDTO account);

    AccountDTO findById(Long accountNumber);

    List<AccountDTO> findAll();
}
