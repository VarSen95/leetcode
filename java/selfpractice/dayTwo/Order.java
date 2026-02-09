package selfpractice.dayTwo;

public class Order {
    private final String orderId;
    private final String merchantId;
    private final double amount;
    private final String currency;
    private final PaymentMethod paymentMethod;
    private final String customerEmail;

    private Order(Builder builder) {
        this.orderId = builder.orderId;
        this.merchantId = builder.merchantId;
        this.amount = builder.amount;
        this.currency = builder.currency;
        this.paymentMethod = builder.paymentMethod;
        this.customerEmail = builder.customerEmail;
    }

    public String getOrderId() {
        return this.orderId;
    }

    public String getMerchantId(){
        return this.merchantId;
    }

    public double getAmount(){
        return this.amount;
    }

    public String getCurrency(){
        return this.currency;
    }

    public PaymentMethod getPaymentMethod(){
        return this.paymentMethod;
    }
    public String getCustomerEmail(){
        return this.customerEmail;
    }
    
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String orderId;
        private String merchantId;
        private double amount;
        private String currency;
        private PaymentMethod paymentMethod;
        private String customerEmail;

        public Builder orderId(String orderId) {
            this.orderId = orderId;
            return this;
        }

        public Builder merchantId(String merchantId) {
            this.merchantId = merchantId;
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

        public Builder customerEmail(String customerEmail) {
            this.customerEmail = customerEmail;
            return this;
        }

        public Order build() {
            if (this.orderId == null || this.orderId.isEmpty()) {
                throw new IllegalArgumentException("OrderId cannot be null or empty");
            }
            if (this.amount <= 0) {
                throw new IllegalArgumentException("Amount cannot be null");
            }

            if (this.currency == null) {
                throw new IllegalArgumentException("Currency cannot be null");
            }
            return new Order(this);
        }
    }

}
