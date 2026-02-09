package banking;

import java.time.Instant;

public class TransactionRecordDTO {
    private final Long accountNumber;
    private final Long targetAccountNumber;
    private final double amount;
    private final TransactionType type;
    private final Instant timestamp;
    private final boolean success;

    public TransactionRecordDTO(Long accountNumber, Long targetAccountNumber, double amount,
            TransactionType type, Instant timestamp, boolean success) {
        this.accountNumber = accountNumber;
        this.targetAccountNumber = targetAccountNumber;
        this.amount = amount;
        this.type = type;
        this.timestamp = timestamp;
        this.success = success;
    }

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
