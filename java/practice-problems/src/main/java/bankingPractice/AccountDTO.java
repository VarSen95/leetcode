package bankingPractice;

public class AccountDTO {
    private final Long accountNumber;
    private final String holderName;
    private final int pin;
    private final double balance;

    public AccountDTO(Long accountNumber, String holderName, int pin, double balance) {
        this.accountNumber = accountNumber;
        this.holderName = holderName;
        this.pin = pin;
        this.balance = balance;
    }

    public Long getAccountNumber() {
        return accountNumber;
    }

    public String getHolderName() {
        return holderName;
    }

    public int getPin() {
        return pin;
    }

    public double getBalance() {
        return balance;
    }
}
