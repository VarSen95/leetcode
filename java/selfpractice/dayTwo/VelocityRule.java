package selfpractice.dayTwo;

import java.time.Duration;
import java.util.Deque;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayDeque;

public class VelocityRule implements FraudRule {
    private int maxAttempts;
    private Map<String, Deque<Long>> cardToTimestampsMap;
    private Duration window;
    private String ruleName;

    public VelocityRule(int maxAttempts, Duration window) {
        this.maxAttempts = maxAttempts;
        this.window = window;
        this.cardToTimestampsMap = new HashMap<>();
        this.ruleName = "VelocityRule";
    }

    @Override
    public boolean isFraudulent(Transaction transaction) {
        Deque<Long> timestamps = cardToTimestampsMap.computeIfAbsent(transaction.getCardNumber(),
                k -> new ArrayDeque<>());
        long now = transaction.getTimestamp();
        long cutoff = now - window.toMillis();
        while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) {
            timestamps.pollFirst();
        }

        timestamps.addLast(now);

        return timestamps.size() > maxAttempts;

    }

    @Override
    public String getRuleName() {
        return this.ruleName;
    }

}
