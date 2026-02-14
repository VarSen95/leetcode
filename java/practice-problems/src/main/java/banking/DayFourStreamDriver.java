package banking;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

/**
 * Seed data and entrypoint for the dayFour stream exercises.
 * The TODOs in {@link #main(String[])} are the spots to implement each
 * exercise.
 */
public class DayFourStreamDriver {
    public enum TransactionStatus {
        APPROVED, DECLINED, REFUNDED
    }

    public static final class PracticeTransaction {
        private final String id;
        private final String merchantId;
        private final double amount;
        private final TransactionStatus status;

        public PracticeTransaction(String id, String merchantId, double amount, TransactionStatus status) {
            this.id = id;
            this.merchantId = merchantId;
            this.amount = amount;
            this.status = status;
        }

        public String id() {
            return id;
        }

        public String merchantId() {
            return merchantId;
        }

        public double amount() {
            return amount;
        }

        public TransactionStatus status() {
            return status;
        }

        @Override
        public String toString() {
            return "Transaction{id='%s', merchantId='%s', amount=%.2f, status=%s}"
                    .formatted(id, merchantId, amount, status);
        }
    }

    public static List<PracticeTransaction> seedTransactions() {
        return List.of(
                new PracticeTransaction("T1", "M1", 500.0, TransactionStatus.APPROVED),
                new PracticeTransaction("T2", "M1", 200.0, TransactionStatus.DECLINED),
                new PracticeTransaction("T3", "M2", 800.0, TransactionStatus.APPROVED),
                new PracticeTransaction("T4", "M2", 150.0, TransactionStatus.APPROVED),
                new PracticeTransaction("T5", "M3", 1200.0, TransactionStatus.DECLINED),
                new PracticeTransaction("T6", "M3", 300.0, TransactionStatus.APPROVED),
                new PracticeTransaction("T7", "M1", 950.0, TransactionStatus.APPROVED),
                new PracticeTransaction("T8", "M2", 100.0, TransactionStatus.REFUNDED));
    }

    public static void main(String[] args) {
        List<PracticeTransaction> transactions = seedTransactions();
        System.out.println("Seeded transactions:");
        transactions.forEach(System.out::println);
        List<PracticeTransaction> approvedTransactions = transactions.stream()
                .filter(t -> t.status() == TransactionStatus.APPROVED)
                .toList();
        System.out.println(String.format("List of all APPROVED transactions %s", approvedTransactions));
        List<PracticeTransaction> overFiveHundredTransactions = transactions.stream()
                .filter(t -> t.amount() > 500)
                .toList();
        System.out.println(String.format("List of over 500 transactions %s", overFiveHundredTransactions));
        List<PracticeTransaction> overTwoHundredTransactions = transactions.stream()
                .filter(t -> t.status() == TransactionStatus.APPROVED)
                .filter(t -> t.amount() > 200)
                .toList();
        System.out.println(String.format("List of over 200 transactions %s", overTwoHundredTransactions));

        // **Exercise 2: map**

        List<String> merchantIds = transactions.stream().map(t -> t.merchantId()).toList();
        System.out.println(String.format("List of overmerchant ids %s", merchantIds));

        List<Double> amounts = transactions.stream().map(t -> t.amount()).toList();
        System.out.println(String.format("List of amounts %s", amounts));

        List<String> transactionSummaries = transactions.stream()
                .filter(t -> t.status() == TransactionStatus.DECLINED)
                .map(t -> t.id())
                .toList();

        System.out.println(String.format("List of transactions %s", transactionSummaries));

        // Exercise 3

        Double total = transactions.stream().mapToDouble(t -> t.amount()).sum();
        System.out.println(String.format("Sum of all transactions %s", total));
        Double sumApprovedAmounts = transactions.stream().filter(t -> t.status() == TransactionStatus.APPROVED)
                .mapToDouble(t -> t.amount()).sum();
        Long totalApprovedAmounts = transactions.stream().filter(t -> t.status() == TransactionStatus.APPROVED).count();
        System.out.println(String.format("Average of all transactions %s", sumApprovedAmounts / totalApprovedAmounts));

        OptionalDouble maxTransaction = transactions.stream().mapToDouble(t -> t.amount()).max();
        System.out.println(String.format("Max of all transactions %s", maxTransaction.getAsDouble()));
        OptionalDouble minTransaction = transactions.stream().mapToDouble(t -> t.amount()).min();
        System.out.println(String.format("Min of all transactions %s", minTransaction.getAsDouble()));

        // Exercise 4

        List<PracticeTransaction> threeHighestTransctions = transactions.stream()
                .sorted(Comparator.comparingDouble(PracticeTransaction::amount).reversed())
                .limit(3)
                .collect(Collectors.toList());
        System.out.println(String.format("Top 3 highest %s", threeHighestTransctions));

        List<PracticeTransaction> bottomLowesttTransctions = transactions.stream()
                .filter(t -> t.status() == TransactionStatus.APPROVED)
                .sorted(Comparator.comparingDouble(PracticeTransaction::amount))
                .limit(2)
                .collect(Collectors.toList());
        System.out.println(String.format("Bottom 2 lowest %s", bottomLowesttTransctions));

        List<PracticeTransaction> allTransactionsSortedByMerchantThenAmount = transactions.stream()
                .sorted(Comparator.comparing(PracticeTransaction::merchantId)
                        .thenComparingDouble(PracticeTransaction::amount))
                .collect(Collectors.toList());
        System.out.println(String.format("All transactions sorted by merchant then by amount %s",
                allTransactionsSortedByMerchantThenAmount));

        // Exercise 5

        Map<TransactionStatus, Long> statusCount = transactions.stream()
                .collect(Collectors.groupingBy(PracticeTransaction::status, Collectors.counting()));

        System.out.println(String.format("Status counts %s", statusCount));
        Map<String, Long> transactionsPerMerchant = transactions.stream()
                .collect(Collectors.groupingBy(PracticeTransaction::merchantId, Collectors.counting()));
        System.out.println(String.format("Transactions per merchant %s", transactionsPerMerchant));

        // Exercise 6

        Map<String, Double> revenuePerMecrhant = transactions.stream()
                .collect(Collectors.groupingBy(PracticeTransaction::merchantId,
                        Collectors.summingDouble(PracticeTransaction::amount)));

        System.out.println(String.format("Transactions per merchant %s", revenuePerMecrhant));

        Map<TransactionStatus, Double> revenuePerStatus = transactions.stream()
                .collect(Collectors.groupingBy(PracticeTransaction::status,
                        Collectors.summingDouble(PracticeTransaction::amount)));

        System.out.println(String.format("Transactions per status %s", revenuePerStatus));

        Map<String, Double> averagePerMerchant = transactions.stream()
                .collect(Collectors.groupingBy(PracticeTransaction::merchantId,
                        Collectors.averagingDouble(PracticeTransaction::amount)));
        System.out.println(String.format("Transactions per average %s", averagePerMerchant));

        Map<TransactionStatus, Double> averagePerStatus = transactions.stream()
                .collect(Collectors.groupingBy(PracticeTransaction::status,
                        Collectors.averagingDouble(PracticeTransaction::amount)));
        System.out.println(String.format("Average per status %s", averagePerStatus));

        Map<TransactionStatus, List<String>> idsPerStatus = transactions.stream()
                .collect(Collectors.groupingBy(PracticeTransaction::status,
                        Collectors.mapping(PracticeTransaction::id, Collectors.toList())));
        System.out.println(String.format("Transaction ids per status %s", idsPerStatus));
        Map<String, List<String>> idsPerMerchant = transactions.stream()
                .collect(Collectors.groupingBy(PracticeTransaction::merchantId,
                        Collectors.mapping(PracticeTransaction::id, Collectors.toList())));
        System.out.println(String.format("Transaction ids per merchant %s", idsPerMerchant));

        Map<String, Map<TransactionStatus, List<PracticeTransaction>>> byMerchantThenStatus = transactions.stream()
                .collect(Collectors.groupingBy(PracticeTransaction::merchantId,
                        Collectors.groupingBy(PracticeTransaction::status)));
        System.out.println(String.format("Group by merchant, then by status %s", byMerchantThenStatus));

        Map<String, Map<TransactionStatus, Long>> byMerchantStatusCount = transactions.stream()
                .collect(Collectors.groupingBy(PracticeTransaction::merchantId,
                        Collectors.groupingBy(PracticeTransaction::status, Collectors.counting())));
        System.out.println(String.format("Group by merchant, then count by status %s", byMerchantStatusCount));

        Map<String, Double> transactionIdToAmount = transactions.stream()
                .collect(Collectors.toMap(PracticeTransaction::id, PracticeTransaction::amount));
        System.out.println(String.format("Transaction ID to amount  %s", transactionIdToAmount));

        Map<String, PracticeTransaction> transactionIdToTransaction = transactions.stream()
                .collect(Collectors.toMap(PracticeTransaction::id, t -> t));
        System.out.println(String.format("Transaction ID to transaction  %s", transactionIdToTransaction));

        Map<String, Double> merchantToHighestAmount = transactions.stream()
                .collect(Collectors.toMap(PracticeTransaction::merchantId, PracticeTransaction::amount, Double::max));
        System.out
                .println(String.format("Merchant to highest amount (handle duplicates)  %s", merchantToHighestAmount));

        // Session 2
        String merchantWithHighestTotalRevenue = transactions.stream()
                .collect(Collectors.groupingBy(PracticeTransaction::merchantId,
                        Collectors.summingDouble(PracticeTransaction::amount)))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey())
                .orElse("");

        System.out.println(
                String.format("Merchant with highest amount  %s", merchantWithHighestTotalRevenue));

        Long approved = transactions.stream()
                .filter(t -> t.status() == TransactionStatus.APPROVED)
                .count();
        long totalTransactions = transactions.size();
        double percentageOfApprovedTransactions = (double) approved / totalTransactions * 100;
        System.out.println(
                String.format("Percentage of approved transactions  %s", percentageOfApprovedTransactions));

        List<String> toptwoMerchantsWithHighestTransactions = transactions.stream()
                .collect(Collectors.groupingBy(PracticeTransaction::merchantId,
                        Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(2)
                .map(e -> e.getKey())
                .collect(Collectors.toList());

        System.out.println(
                String.format("Top two merchants  %s", toptwoMerchantsWithHighestTransactions));

        double avgTop3 = transactions.stream()
                .sorted(Comparator.comparingDouble(PracticeTransaction::amount).reversed())
                .limit(3)
                .mapToDouble(PracticeTransaction::amount)
                .average()
                .orElse(0.0);

        System.out.println(
                String.format("Top three avg  %s", avgTop3));

        List<String> merchantsDeclined = transactions.stream()
                .filter(t -> t.status() == TransactionStatus.DECLINED)
                .map(PracticeTransaction::merchantId)
                .distinct()
                .collect(Collectors.toList());

        System.out.println(
                String.format("Merchants declined %s", merchantsDeclined));
    }
}
