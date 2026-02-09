package selfpractice.dayTwo;

public class Transaction {
    private int amount;
    private String country;
    private String cardNumber;
    private long timestamp;

    public Transaction(int amount, String country, String cardNumber) {
        this.amount = amount;
        this.country = country;
        this.cardNumber = cardNumber;
        this.timestamp = System.currentTimeMillis();
    }

    public int getAmount() {
        return this.amount;
    }

    public String getCountry() {
        return this.country;
    }

    public String getCardNumber() {
        return this.cardNumber;
    }

    public long getTimestamp() {
        return this.timestamp;
    }
}
