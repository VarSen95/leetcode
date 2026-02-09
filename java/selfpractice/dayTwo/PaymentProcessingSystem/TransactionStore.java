package selfpractice.dayTwo.PaymentProcessingSystem;

import java.time.Duration;
import java.util.List;

public interface TransactionStore {
    void save(Transaction transaction);

    Transaction findById(String id);

    List<Transaction> findByMerchant(String merchantId);

    List<Transaction> findByCard(String cardId, Duration window);
}
