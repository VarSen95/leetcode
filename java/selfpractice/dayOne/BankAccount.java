package selfpractice.dayOne;

public class BankAccount {
    private String accountId;
    private double balance;
    private String ownerName;

    BankAccount(String accountId, double balance, String ownerName) {
        this.accountId = accountId;
        this.balance = balance;
        this.ownerName = ownerName;
    }


    public double getBalance() {
        return balance;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void deposit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must not be negative");
        }
        this.balance += amount;
        }

    public void withdraw(double amount){
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must not be negative");
        } else if (amount > this.balance) {
            throw new IllegalArgumentException("Amount must not be greater than balance");
        }
        this.balance -= amount;
    }

    public String toString(){
        return "Owner:" + this.ownerName + ", AccountId: " + this.accountId + ", Balance: " + this.balance;
    }

    public static void main(String[] args) {
        BankAccount account = new BankAccount("1234", 500.0, "Ala");
        System.out.println(account);
        account.withdraw(100);
        System.out.println(account);
        account.deposit(300);
        System.out.println(account);
    }
    
}
