package selfpractice.dayTwo.PaymentProcessingSystem;

public class BankTransferPayment implements PaymentMethod {
    private final String type = "BankTransfer";
    private double balance;
    private String accountNumber;
    private String bankCode;

    private BankTransferPayment(double initialBalance, String accountNumber, String bankCode) {
        this.balance = initialBalance;
        this.accountNumber = accountNumber;
        this.bankCode = bankCode;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private double initialBalance;
        private String accountNumber;
        private String bankCode;

        public Builder initialBalance(double balance) {
            this.initialBalance = balance;
            return this;
        }

        public Builder accountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }

        public Builder bankCode(String bankCode) {
            this.bankCode = bankCode;
            return this;
        }

        public BankTransferPayment build() {
            if (this.accountNumber == null || this.bankCode == null || this.initialBalance <= 0) {
                throw new IllegalArgumentException("Invalid bank transfer details");
            }
            return new BankTransferPayment(this.initialBalance, this.accountNumber, this.bankCode);
        }
    }

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
        return this.accountNumber;
    }
    
}
