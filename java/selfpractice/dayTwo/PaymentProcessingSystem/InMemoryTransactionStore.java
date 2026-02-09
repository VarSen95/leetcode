package selfpractice.dayTwo.PaymentProcessingSystem;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryTransactionStore implements TransactionStore {

    private Map<String, Transaction> transactionsById = new ConcurrentHashMap<>();
    private Map<String, List<Transaction>> transactionByMerchantId = new ConcurrentHashMap<>();
    private Map<String, List<Transaction>> transactionsByPaymentId = new ConcurrentHashMap<>();

    @Override
    public void save(Transaction transaction) {
        transactionsById.put(transaction.getId(), transaction);
        String id = transaction.getPaymentMethod().getId();
        if (id != null) {
            transactionsByPaymentId.computeIfAbsent(id, k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(transaction);

            transactionByMerchantId
                    .computeIfAbsent(transaction.getMerchant().getId(),
                            k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(transaction);
        }

    }

    @Override
    public Transaction findById(String id) {
        Transaction transaction = transactionsById.get(id);
        if (transaction == null) {
            throw new RuntimeException("Transaction not found");
        }

        return transaction;
    }

    @Override
    public List<Transaction> findByMerchant(String merchantId) {
        return new ArrayList<>(transactionByMerchantId.getOrDefault(merchantId, Collections.emptyList()));
    }

    @Override
    public List<Transaction> findByCard(String cardId, Duration window) {
        List<Transaction> transactions = new ArrayList<>(
                transactionsByPaymentId.getOrDefault(cardId, Collections.emptyList()));
        if (window == null) {
            return transactions;
        }

        long cutoff = Instant.now().toEpochMilli() - window.toMillis();
        return transactions.stream()
                .filter(t -> t.getTimestamp() > cutoff)
                .collect(Collectors.toList());
    }

}
