package banking;

public interface ExchangeRateService {
    public double getExchangeRate(Currency source, Currency target);
}
