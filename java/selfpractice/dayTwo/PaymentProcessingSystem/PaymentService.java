package selfpractice.dayTwo.PaymentProcessingSystem;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class PaymentService {
    private FraudChecker fraudChecker;
    private PaymentProcesser paymentProcessor;
    private TransactionStore transactionStore;
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public PaymentService(FraudChecker fraudChecker, PaymentProcesser paymentProcessor,
            TransactionStore transactionStore) {
        this.fraudChecker = fraudChecker;
        this.paymentProcessor = paymentProcessor;
        this.transactionStore = transactionStore;
    }

    public TransactionResult processPayment(Transaction transaction) {
        PaymentMethod paymentMethod = transaction.getPaymentMethod();
        String key = paymentMethod != null ? paymentMethod.getId() : null;
        if (key == null || key.isEmpty()) {
            key = transaction.getId();
        }

        ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        try {
            FraudCheckResult result = this.fraudChecker.check(transaction);

            if (result.isPassed()) {
                TransactionResult transactionResult = this.paymentProcessor.process(transaction);
                this.transactionStore.save(transaction);
                return transactionResult;
            } else {
                return TransactionResult.fraud("fraud detected");
            }
        } finally {
            lock.unlock();
        }

    }

}
