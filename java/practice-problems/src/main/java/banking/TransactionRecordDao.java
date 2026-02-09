package banking;

import java.util.List;

public interface TransactionRecordDao {
    void save(TransactionRecordDTO record);
    List<TransactionRecordDTO> findByAccount(Long accountNumber);
    List<TransactionRecordDTO> findAll();
}
