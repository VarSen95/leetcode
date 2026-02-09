package selfpractice.dayTwo.PaymentProcessingSystem;

public class CardPaymentProcessor implements PaymentProcesser {

    @Override
    public synchronized TransactionResult process(Transaction transaction) {

        PaymentMethod paymentMethod = transaction.getPaymentMethod();
        if (!supports(paymentMethod)) {
            return TransactionResult.declined("Unsupported payment method");
        }
        CardPayment cardPayment = (CardPayment) paymentMethod;

        if (cardPayment.isExpired(transaction.getTimestamp())) {
            return TransactionResult.declined("Expired card");
        }

        double balance = cardPayment.getBalance();
        boolean enoughFunds = balance >= transaction.getAmount();

        if (!enoughFunds) {
            return TransactionResult.declined("Insufficient funds");
        } else {
            return cardPayment.processPayment(transaction.getAmount())
                    ? TransactionResult.approved("Successful")
                    : TransactionResult.declined("Payment failed");

        }

    }

    @Override
    public synchronized TransactionResult refund(Transaction transaction) {
        PaymentMethod paymentMethod = transaction.getPaymentMethod();
        if (!(paymentMethod instanceof CardPayment)) {
            return TransactionResult.declined("Unsupported payment method");
        }
        boolean refunded = paymentMethod.refund(transaction.getAmount());
        return refunded
                ? TransactionResult.approved("Successful")
                : TransactionResult.declined("Refund failed");

    }

    public boolean supports(PaymentMethod method) {
        return method instanceof CardPayment;
    }
}
