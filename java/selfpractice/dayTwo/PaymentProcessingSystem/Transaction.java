package selfpractice.dayTwo.PaymentProcessingSystem;

public class Transaction {
    private final String id;
    private double amount;
    private final String currency;
    private Long timestamp;
    private PaymentMethod paymentMethod;
    private final Merchant merchant;

    private Transaction(String id, Merchant merchant, double amount, String currency, PaymentMethod paymentMethod) {
        this.id = id;
        this.merchant = merchant;
        this.amount = amount;
        this.currency = currency;
        this.timestamp = System.currentTimeMillis();
        this.paymentMethod = paymentMethod;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public double getAmount() {
        return this.amount;
    }

    public PaymentMethod getPaymentMethod() {
        return this.paymentMethod;
    }

    public String getId() {
        return this.id;
    }

    public Merchant getMerchant() {
        return this.merchant;
    }

    public static Builder builder() {
        return new Builder();
    }

    static class Builder {
        private String id;
        private Merchant merchant;
        private double amount;
        private String currency;
        private TransactionResult result;
        private Long timestamp;
        private PaymentMethod paymentMethod;
        private PaymentDetails paymentDetails;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder merchant(Merchant merchant) {
            this.merchant = merchant;
            return this;
        }

        public Builder amount(double amount) {
            this.amount = amount;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder paymentMethod(PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
            return this;
        }

        public Transaction build() {
            if (this.id == null || this.merchant == null || this.amount <= 0) {
                throw new IllegalArgumentException("Invalid transaction parameters");
            }
            return new Transaction(this.id, this.merchant, this.amount, this.currency, this.paymentMethod);
        }
    }

}