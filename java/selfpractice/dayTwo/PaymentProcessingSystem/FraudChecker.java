package selfpractice.dayTwo.PaymentProcessingSystem;

public interface FraudChecker {
    public FraudCheckResult check(Transaction transaction);
}
