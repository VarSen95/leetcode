package banking;

import java.util.ArrayList;
import java.util.List;

public class RequestIdInMemoryStore implements RequestIdStore {
    private final List<String> requestIds = new ArrayList<>();

    @Override
    public void logRequestId(String requestId) {
        if (requestId != null) {
            requestIds.add(requestId);
        }
    }
}
