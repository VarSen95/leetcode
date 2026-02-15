
package practice;

public class RuleCheckResult {

    private final boolean allowed;
    private final String reason;

    private RuleCheckResult(boolean allowed, String reason) {
        this.allowed = allowed;
        this.reason = reason;
    }

    public static Builder builder(){
        return new Builder();
    }

    public boolean isAllowed() {
        return allowed;
    }

    public String getReason() {
        return reason;
    }

    public static class Builder{
        private boolean allowed;
        private String reason;

        public Builder allowed(boolean allowed) {
            this.allowed = allowed;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public RuleCheckResult build() {
            return new RuleCheckResult(allowed, reason);
        }
    }
}
