package banking;

import java.util.ArrayList;
import java.util.List;

public class TransactionHistoryInMemoryStore implements TransactionHistoryStore {

    private List<TransactionRecord> store;

    public TransactionHistoryInMemoryStore() {
        this.store = new ArrayList<>();
    }

    @Override
    public void logTransaction(TransactionRecord record) {
        // FOLLOW UP: TransactionRecord keeps auditing extensible with richer metadata per event.
        store.add(record);
    }

}
