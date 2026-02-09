package selfpractice.dayTwo.PaymentProcessingSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class PaymentProcessorRouter implements PaymentProcesser {

    private List<PaymentProcesser> paymentProcessors = new ArrayList<>();

    public PaymentProcessorRouter(List<PaymentProcesser> processors) {
        this.paymentProcessors = List.copyOf(processors);
    }

    @Override
    public TransactionResult process(Transaction transaction) {
        return findProcessor(transaction).process(transaction);
    }

    private PaymentProcesser findProcessor(Transaction transaction) {
        for (PaymentProcesser processor : paymentProcessors) {
            if (processor.supports(transaction.getPaymentMethod())) {
                return processor;
            }
        }
        throw new IllegalArgumentException("No processor for method: " +
                transaction.getPaymentMethod().getType());
    }

    @Override
    public TransactionResult refund(Transaction transaction) {
        return findProcessor(transaction).refund(transaction);
    }

    @Override
    public boolean supports(PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            return false;
        }
        for (PaymentProcesser processor : paymentProcessors) {
            if (processor.supports(paymentMethod)) {
                return true;
            }
        }
        return false;
    }

}
