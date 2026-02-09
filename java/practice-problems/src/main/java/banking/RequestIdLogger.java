package banking;

public class RequestIdLogger {
    private final RequestIdStore requestIdStore;

    public RequestIdLogger(RequestIdStore requestIdStore) {
        this.requestIdStore = requestIdStore;
    }

    public void log(String requestId) {
        this.requestIdStore.logRequestId(requestId);
    }
}
