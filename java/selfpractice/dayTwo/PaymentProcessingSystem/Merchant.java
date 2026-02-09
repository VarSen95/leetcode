package selfpractice.dayTwo.PaymentProcessingSystem;

import java.util.ArrayList;
import java.util.List;

public class Merchant {

    private String id;
    private String name;
    private String country;
    private List<FraudRule> fraudRules = new ArrayList<>();

    private Merchant(String id, String name, String country) {
        this.id = id;
        this.name = name;
        this.country = country;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return this.id;
    }

    public static class Builder {
        private String id;
        private String name;
        private String country;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder country(String country) {
            this.country = country;
            return this;
        }

        public Merchant build() {
            if (this.id == null || this.name == null || this.name.isEmpty()) {
                throw new IllegalArgumentException("Invalid merchant parameters");
            }
            return new Merchant(this.id, this.name, this.country);
        }
    }

    public boolean addRule(FraudRule rule) {
        if (rule == null || rule.getRuleName() == null || rule.getRuleName().isEmpty()) {
            this.fraudRules.add(rule);
            return true;
        }
        return false;
    }

    public boolean removeRule(FraudRule rule) {
        if (rule == null || rule.getRuleName() == null || rule.getRuleName().isEmpty()) {
            this.fraudRules.add(rule);
            return true;
        }
        return false;
    }

}
