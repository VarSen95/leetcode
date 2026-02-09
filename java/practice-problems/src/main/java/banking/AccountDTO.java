package banking;

public class AccountDTO {
    private final Long accountNumber;
    private final double balance;
    private final Currency currency;
    private final String holderType;
    private final int holderId;
    private final String holderFirstName;
    private final String holderLastName;
    private final String companyName;

    public AccountDTO(Long accountNumber, double balance, Currency currency,
            String holderType, int holderId,
            String holderFirstName, String holderLastName,
            String companyName) {
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.currency = currency;
        this.holderType = holderType;
        this.holderId = holderId;
        this.holderFirstName = holderFirstName;
        this.holderLastName = holderLastName;
        this.companyName = companyName;
    }

    public Long getAccountNumber() {
        return accountNumber;
    }

    public double getBalance() {
        return balance;
    }

    public Currency getCurrency() {
        return currency;
    }

    public String getHolderType() {
        return holderType;
    }

    public int getHolderId() {
        return holderId;
    }

    public String getHolderFirstName() {
        return holderFirstName;
    }

    public String getHolderLastName() {
        return holderLastName;
    }

    public String getCompanyName() {
        return companyName;
    }
}
