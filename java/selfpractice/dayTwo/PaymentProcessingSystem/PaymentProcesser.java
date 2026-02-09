package selfpractice.dayTwo.PaymentProcessingSystem;

public interface PaymentProcesser {
    TransactionResult process(Transaction transaction);
    TransactionResult refund(Transaction transaction);
    boolean supports(PaymentMethod paymentMethod);
}
