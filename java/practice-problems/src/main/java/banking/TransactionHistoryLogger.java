package banking;

public class TransactionHistoryLogger {
    TransactionHistoryStore historyStore;

    public TransactionHistoryLogger(TransactionHistoryStore historyStore) {
        this.historyStore = historyStore;
    }

    public void log(TransactionRecord record) {
        this.historyStore.logTransaction(record);
    }

}
