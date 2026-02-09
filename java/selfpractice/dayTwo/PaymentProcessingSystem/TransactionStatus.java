package selfpractice.dayTwo.PaymentProcessingSystem;

public enum TransactionStatus {
    CREATED, PENDING, APPROVED, DECLINED, REFUNDED, FRAUD;

    public static boolean isValid(String status) {
        try {
            valueOf(status);
            return true;
        } catch (IllegalArgumentException e){
            return false;
        }
    }

}