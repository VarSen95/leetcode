package selfpractice.dayTwo;

import java.util.ArrayList;
import java.util.List;

public class BlockedCountryRule implements FraudRule {

    private final List<String> blockedCountries = new ArrayList<>();
    private final String ruleName;

    public BlockedCountryRule(List<String> blockedCountries) {
        this.blockedCountries.addAll(blockedCountries);
        this.ruleName = "BlockedCountryRule";

    }

    @Override
    public boolean isFraudulent(Transaction transaction) {
        return transaction.getCountry() != null && blockedCountries.contains(transaction.getCountry());

    }

    @Override
    public String getRuleName() {
        return this.ruleName;
    }

}
