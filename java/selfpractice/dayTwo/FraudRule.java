package selfpractice.dayTwo;

public interface FraudRule {
    public boolean isFraudulent(Transaction transaction);
    public String getRuleName();
}
