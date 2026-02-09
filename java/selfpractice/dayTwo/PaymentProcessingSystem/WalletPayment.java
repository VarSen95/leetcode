package selfpractice.dayTwo.PaymentProcessingSystem;

public class WalletPayment implements PaymentMethod {
    private final String type = "Wallet";
    private double balance;
    private String id;

    public WalletPayment(){}
    @Override
    public synchronized boolean processPayment(double amount) {
        if (amount <= 0) {
            return false;
        }
        this.balance -= amount;
        return true;
    }

    @Override
    public synchronized boolean refund(double amount) {
        if (amount <= 0) {
            return false;
        }
        this.balance += amount;
        return true;
    }

    @Override
    public String getType() {
        return this.type;
    }

    public synchronized double getBalance() {
        return this.balance;
    }
    @Override
    public String getId() {
        return this.id;
    }
    
}
