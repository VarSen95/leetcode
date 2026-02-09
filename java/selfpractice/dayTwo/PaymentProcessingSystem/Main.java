package selfpractice.dayTwo.PaymentProcessingSystem;

import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;

public class Main {
        public static void main(String[] args) {
                // Create components
                TransactionStore store = new InMemoryTransactionStore();
                FraudEngine fraudEngine = new FraudEngine();
                fraudEngine.addRule(new AmountThresholdRule(9000));
                fraudEngine.addRule(new VelocityRule(2, Duration.ofMinutes(10)));
                PaymentProcesser router = new PaymentProcessorRouter(
                                Arrays.asList(new CardPaymentProcessor(), new BankTransferPaymentProcessor()));
                PaymentService service = new PaymentService(fraudEngine, router, store);

                // Process a payment
                Transaction txn = Transaction.builder()
                                .merchant(Merchant.builder().country("IE").id("merchant-1").name("merchantName")
                                                .build())
                                .amount(150.00)
                                .currency("EUR")
                                .id(UUID.randomUUID().toString())
                                .paymentMethod(
                                                CardPayment.builder().cardNumber("CARD-123").cvv("123")
                                                                .expiryDate("2027-01-01")
                                                                .initialBalance(3000).build())
                                .build();
                // Process a payment
                Transaction txn2 = Transaction.builder()
                                .merchant(Merchant.builder().country("IE").id("merchant-1").name("merchantName")
                                                .build())
                                .amount(50.00)
                                .currency("EUR")
                                .id(UUID.randomUUID().toString())
                                .paymentMethod(
                                                CardPayment.builder().cardNumber("CARD-123").cvv("123")
                                                                .expiryDate("2029-01-01")
                                                                .initialBalance(40000).build())
                                .build();

                Transaction txn3 = Transaction.builder()
                                .merchant(Merchant.builder().country("IE").id("merchant-1").name("merchantName")
                                                .build())
                                .amount(50.00)
                                .currency("EUR")
                                .id(UUID.randomUUID().toString())
                                .paymentMethod(BankTransferPayment.builder().accountNumber("CARD-123")
                                                .initialBalance(30000.0)
                                                .bankCode("lala").build())
                                .build();
                System.out.println(
                                String.format("Processing transaction: %s %s", txn.getPaymentMethod(),
                                                txn2.getPaymentMethod()));

                TransactionResult result = service.processPayment(txn);
                TransactionResult result2 = service.processPayment(txn2);
                TransactionResult result3 = service.processPayment(txn3);

                System.out.println(String.format("Result: %s %s %s", result.toString(), result2.toString(),
                                result3.toString()));
        }
}
