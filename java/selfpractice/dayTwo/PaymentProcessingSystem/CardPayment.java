package selfpractice.dayTwo.PaymentProcessingSystem;

import java.time.LocalDate;
import java.time.ZoneId;

public class CardPayment implements PaymentMethod {
    private final String type = "Card";
    private double balance;
    private String cardNumber;
    private String cvv;
    private LocalDate expiryDate;

    private CardPayment(double initialBalance, String cardNumber, String cvv, LocalDate expiryDate) {
        this.balance = initialBalance;
        this.cardNumber = cardNumber;
        this.cvv = cvv;
        this.expiryDate = expiryDate;
    }

    public boolean isExpired(long transactionTimestamp) {
        long startOfDay = this.expiryDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();

        return startOfDay < transactionTimestamp;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private double initialBalance;
        private String cardNumber;
        private String cvv;
        private LocalDate expiryDate;

        public Builder initialBalance(double balance) {
            this.initialBalance = balance;
            return this;
        }

        public Builder cardNumber(String cardNumber) {
            this.cardNumber = cardNumber;
            return this;
        }

        public Builder cvv(String cvv) {
            this.cvv = cvv;
            return this;
        }

        public Builder expiryDate(String expiryDate) {
            LocalDate date = LocalDate.parse(expiryDate);
            this.expiryDate = date;
            return this;
        }

        public CardPayment build() {
            if (this.cardNumber == null || this.cvv == null || this.expiryDate == null || this.initialBalance <= 0) {
                throw new IllegalArgumentException("Invalid Card Details");
            }
            return new CardPayment(this.initialBalance, this.cardNumber, this.cvv, this.expiryDate);
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

    @Override
    public synchronized double getBalance() {
        return this.balance;
    }

    @Override
    public String getId() {
        return this.cardNumber;
    }

}
