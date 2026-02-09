package selfpractice.dayTwo.PaymentProcessingSystem;

public class BankTransferPaymentProcessor implements PaymentProcesser {
    @Override
    public TransactionResult process(Transaction transaction) {
        PaymentMethod paymentMethod = transaction.getPaymentMethod();
        if (!supports(paymentMethod)) {
            return TransactionResult.declined("Unsupported payment method");
        }

        BankTransferPayment bankTransfer = (BankTransferPayment) paymentMethod;
        double amount = transaction.getAmount();
        if (bankTransfer.getBalance() < amount) {
            return TransactionResult.declined("Insufficient funds");
        }

        return bankTransfer.processPayment(amount)
                ? TransactionResult.approved("Successful")
                : TransactionResult.declined("Payment failed");
    }

    @Override
    public TransactionResult refund(Transaction transaction) {
        PaymentMethod paymentMethod = transaction.getPaymentMethod();
        if (!(paymentMethod instanceof BankTransferPayment)) {
            return TransactionResult.declined("Unsupported payment method");
        }

        boolean refunded = paymentMethod.refund(transaction.getAmount());
        return refunded
                ? TransactionResult.approved("Successful")
                : TransactionResult.declined("Refund failed");
    }

    public boolean supports(PaymentMethod method) {
        return method instanceof BankTransferPayment;
    }
}
