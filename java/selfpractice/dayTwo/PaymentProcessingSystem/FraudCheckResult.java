package selfpractice.dayTwo.PaymentProcessingSystem;

import java.util.List;

public class FraudCheckResult {
    private boolean isPassed;
    private List<FraudRule> failedRules;

    public FraudCheckResult(boolean isPassed, List<FraudRule> failedRules) {
        this.isPassed = isPassed;
        this.failedRules = failedRules;
    }

    public boolean isPassed() {
        return isPassed;
    }

    public List<FraudRule> getFailedRules() {
        return failedRules;
    }

    @Override
    public String toString() {
        return String.format("FraudCheckResult[isPassed=%s, failedRules=%s]", isPassed, failedRules);

    }

}
