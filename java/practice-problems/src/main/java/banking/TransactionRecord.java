package banking;

import java.time.Instant;

public class TransactionRecord {
    private final Long accountNumber;
    private final Long targetAccountNumber; // null if not transfer
    private final double amount;
    private final TransactionType type;
    private final Instant timestamp;
    private final boolean success;

    // For credit/debit
    public TransactionRecord(Long accountNumber, double amount,
            TransactionType type, boolean success) {
        this.accountNumber = accountNumber;
        this.targetAccountNumber = null;
        this.amount = amount;
        this.type = type;
        this.timestamp = Instant.now();
        this.success = success;
    }

    // For transfers
    public TransactionRecord(Long sourceAccount, Long targetAccount,
            double amount, boolean success) {
        this.accountNumber = sourceAccount;
        this.targetAccountNumber = targetAccount;
        this.amount = amount;
        this.type = TransactionType.TRANSFER;
        this.timestamp = Instant.now();
        this.success = success;
    }

    // getters
    public Long getAccountNumber() {
        return accountNumber;
    }

    public Long getTargetAccountNumber() {
        return targetAccountNumber;
    }

    public double getAmount() {
        return amount;
    }

    public TransactionType getType() {
        return type;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public boolean isSuccess() {
        return success;
    }
}
