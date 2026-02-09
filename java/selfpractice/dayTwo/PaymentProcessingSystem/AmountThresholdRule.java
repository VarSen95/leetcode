package selfpractice.dayTwo.PaymentProcessingSystem;

public class AmountThresholdRule implements FraudRule {
    private final int threshold;
    private final String ruleName;
    public AmountThresholdRule(int threshold) {
        this.threshold = threshold;
        this.ruleName = "AmountThresholdRule";
    }

    @Override
    public boolean isFraudulent(Transaction transaction) {
        return transaction.getAmount() > this.threshold;
    }

    @Override
    public String getRuleName() {
        return this.ruleName;
    }

}
