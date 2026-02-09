package selfpractice.dayTwo.PaymentProcessingSystem;

public interface FraudRule {
    public boolean isFraudulent(Transaction transaction);
    public String getRuleName();
}
