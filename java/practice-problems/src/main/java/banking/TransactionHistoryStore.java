package banking;

public interface TransactionHistoryStore {
    public void logTransaction(TransactionRecord record);
}
