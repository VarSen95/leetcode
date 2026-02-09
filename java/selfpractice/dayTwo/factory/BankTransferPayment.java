package selfpractice.dayTwo.factory;

public class BankTransferPayment implements PaymentMethod {
    private final String type = "BankTransfer";
    private double balance;

    public BankTransferPayment(){}
    @Override
    public boolean processPayment(double amount) {
        if (amount <= 0) {
            return false;
        }
        this.balance -= amount;
        return true;
    }

    @Override
    public boolean refund(double amount) {
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

    public double getBalance() {
        return this.balance;
    }
    
}
