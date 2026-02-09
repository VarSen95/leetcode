package selfpractice.dayTwo;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FraudEngine {
    private final List<FraudRule> rules = new ArrayList<>();

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

    public static void main(String[] args) {
        FraudEngine engine = new FraudEngine();
        engine.addRule(new AmountThresholdRule(10003));
        engine.addRule(new BlockedCountryRule(Arrays.asList("XX", "YY")));
        engine.addRule(new VelocityRule(5, Duration.ofMinutes(10)));

        for (int i = 1; i <= 9; i++) {
            Transaction tx = new Transaction(100, "US", "Card123");
            FraudCheckResult result = engine.check(tx);
            System.out.println(String.format("ATTEMPT: %s, FRAUD CHECK RESULT %s", i, result.toString()));

        }
    }

}
