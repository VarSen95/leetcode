package selfpractice.dayTwo.PaymentProcessingSystem;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class FraudEngine implements FraudChecker{
    // to make it thread safe
    private final List<FraudRule> rules = new CopyOnWriteArrayList<>();

    public FraudEngine() {
    }

    public void addRule(FraudRule rule) {
        rules.add(rule);
    }

    public void removeRule(FraudRule rule) {
        rules.remove(rule);
    }

    public FraudCheckResult check(Transaction transaction) {
        boolean isPassed = true;
        List<FraudRule> failedRules = new ArrayList<>();
        for (FraudRule rule : rules) {
            if (rule.isFraudulent(transaction)) {
                isPassed = false;
                failedRules.add(rule);
            }
        }
        return new FraudCheckResult(isPassed, failedRules);
    }

}
