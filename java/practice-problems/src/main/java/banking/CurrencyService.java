package banking;

public class CurrencyService {

    private ExchangeRateService exchangeRateService;

    CurrencyService(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    public double convert(
            double amount,
            Currency from,
            Currency to) {
        if (from == to) {
            return amount;
        }

        return amount * this.exchangeRateService.getExchangeRate(from, to);
    }
}