package selfpractice.dayTwo.factory;

public interface PaymentMethod {
    boolean processPayment(double amount);
    boolean refund(double amount);
    String getType();
    
}
