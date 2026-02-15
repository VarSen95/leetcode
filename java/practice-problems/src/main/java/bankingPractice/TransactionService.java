package bankingPractice;

public interface TransactionService {
    void credit(Long accountNumber, double amount);

    boolean debit(Long accountNumber, double amount);

    boolean transfer(Long sourceAccount, Long destinationAccount, double amount);
}
