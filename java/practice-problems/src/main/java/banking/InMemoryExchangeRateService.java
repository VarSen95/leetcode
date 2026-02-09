package banking;

import java.util.HashMap;
import java.util.Map;

public class InMemoryExchangeRateService implements ExchangeRateService {
    private final Map<String, Double> rates = new HashMap<>();

    public void setRate(Currency from, Currency to, double rate) {
        rates.put(from + "_" + to, rate);
        rates.put(to + "_" + from, 1.0 / rate);
    }

    public InMemoryExchangeRateService() {
        // Seed default rates (all relative to USD)
        setRate(Currency.EUR, Currency.USD, 1.08);
        setRate(Currency.GBP, Currency.USD, 1.27);
        setRate(Currency.EUR, Currency.GBP, 0.85);
    }

    @Override
    public double getExchangeRate(Currency source, Currency target) {
        if (source == target)
            return 1.0;
        Double rate = rates.get(source + "_" + target);
        if (rate == null) {
            throw new IllegalArgumentException(
                    String.format("No rate for %s to %s", source, target));
        }
        return rate;
    }

}
