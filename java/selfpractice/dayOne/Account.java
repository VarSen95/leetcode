package selfpractice;

public class Account {

    private long accountNumber;
    private AccountHolder accountHolder;
    private double balance;


    public Account(long accountNumber, AccountHolder accountHolder, double balance){
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
        this.balance = balance;
    }

    public void deposit(double amount){
        if (amount <=0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if(amount > 0){
            balance += amount;
        }
    }

    public void withdraw(double amount){
        if (amount <=0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if(amount > 0 && amount <= balance){
            balance -= amount;
        }
    }

    public double getBalance() {
        return this.balance;
    }
    
}
