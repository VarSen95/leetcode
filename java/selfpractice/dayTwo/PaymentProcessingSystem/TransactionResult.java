package selfpractice.dayTwo.PaymentProcessingSystem;

public class TransactionResult {

    private TransactionStatus status;
    private String message;

    public TransactionResult() {
        this.status = TransactionStatus.CREATED;
    }

    public String toString() {
        return "TransactionResult{" +

                "status=" + status +

                ", message='" + message + '\'' +

                '}';

    }

    private static Builder builder() {
        return new Builder();
    }

    public static TransactionResult created(String message) {
        return TransactionResult.builder().status("CREATED").message(message).build();
    }

    public static TransactionResult pending(String message) {
        return TransactionResult.builder().status("PENDING").message(message).build();
    }

    public static TransactionResult declined(String message) {
        return TransactionResult.builder().status("DECLINED").message(message).build();
    }

    public static TransactionResult approved(String message) {
        return TransactionResult.builder().status("APPROVED").message(message).build();
    }

    public static TransactionResult fraud(String message) {
        return TransactionResult.builder().status("FRAUD").message(message).build();
    }

    public static TransactionResult refunded(String message) {
        return TransactionResult.builder().status("REFUNDED").message(message).build();
    }

    public TransactionStatus getTransactionStatus() {
        return this.status;
    }

    public String getMessage() {
        return this.message;
    }

    private static class Builder {
        private TransactionStatus status;
        private String message;

        public Builder status(String status) {
            if (TransactionStatus.isValid(status)) {
                this.status = TransactionStatus.valueOf(status);
                return this;
            }
            throw new IllegalArgumentException("Invalid Transaction Status");
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public TransactionResult build() {
            TransactionResult transactionResult = new TransactionResult();
            transactionResult.status = this.status;
            transactionResult.message = this.message;
            return transactionResult;
        }
    }
}
