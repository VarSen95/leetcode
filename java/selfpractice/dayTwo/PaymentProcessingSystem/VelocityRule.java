package selfpractice.dayTwo.PaymentProcessingSystem;

import java.time.Duration;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.ArrayDeque;

public class VelocityRule implements FraudRule {
    private int maxAttempts;
    private final Map<String, Deque<Long>> cardToTimestampsMap = new ConcurrentHashMap<>();
    private Duration window;
    private String ruleName;

    public VelocityRule(int maxAttempts, Duration window) {
        this.maxAttempts = maxAttempts;
        this.window = window;
        this.ruleName = "VelocityRule";
    }

    @Override
    public boolean isFraudulent(Transaction transaction) {
        String key = null;
        if (transaction.getPaymentMethod() != null) {
            key = transaction.getPaymentMethod().getId();
        }
        if (key == null) {
            key = transaction.getId();
        }

        Deque<Long> timestamps = cardToTimestampsMap.computeIfAbsent(key,
                k -> new ArrayDeque<>());
        // thread safe
        synchronized (timestamps) {
            long now = transaction.getTimestamp();
            long cutoff = now - window.toMillis();
            while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) {
                timestamps.pollFirst();
            }

            timestamps.addLast(now);

            return timestamps.size() > maxAttempts;
        }

    }

    @Override
    public String getRuleName() {
        return this.ruleName;
    }

}
