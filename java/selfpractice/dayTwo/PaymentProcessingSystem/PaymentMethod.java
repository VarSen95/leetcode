package selfpractice.dayTwo.PaymentProcessingSystem;

public interface PaymentMethod {
    boolean processPayment(double amount);
    boolean refund(double amount);
    String getId();
    String getType();
    double getBalance();
    
}
