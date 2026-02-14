package practice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FraudRulesCheckEngine {
    private final List<MerchantRule> rules;

    private FraudRulesCheckEngine(List<MerchantRule> rules) {
        this.rules = Collections.unmodifiableList(rules);
    }

    public RuleCheckResult checkRules(Payment payment) {
        for (MerchantRule rule : rules) {
            RuleCheckResult result = rule.checkRule(payment);
            if (!result.isAllowed()) {
                return result;
            }
        }
        return RuleCheckResult.builder().allowed(true).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final List<MerchantRule> rules = new ArrayList<>();

        public Builder rules(List<MerchantRule> rules) {
            this.rules.addAll(rules);
            return this;
        }

        public Builder addRule(MerchantRule rule) {
            this.rules.add(rule);
            return this;
        }

        public FraudRulesCheckEngine build() {
            return new FraudRulesCheckEngine(this.rules);
        }
    }

}
