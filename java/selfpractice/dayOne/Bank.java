package selfpractice.dayOne;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Random;

public class Bank {

    private Map<Long, Account> accounts = new HashMap<>();
    private static Random random = new Random();

    public long createAccount(AccountHolder accountHolder, double initialDeposit){
        long accountNumber = random.nextLong();
        Account account = new Account(accountNumber, accountHolder, initialDeposit);
        this.accounts.put(accountNumber, account);
        return accountNumber;
    }

    public Account getAccount(long accountNumber) {
        Account account = this.accounts.getOrDefault(accountNumber, null);
        if (account == null) {
            throw new IllegalArgumentException("Account not found");
        }
        return account;
    }

    public void transfer(long fromAccount, long toAccount, double amount) {
        Account sourceAccount = this.getAccount(fromAccount);
        Account targetAccount = this.getAccount(toAccount);
        sourceAccount.withdraw(amount);
        targetAccount.deposit(amount);
    }

    public static void main(String[] args) {
        Bank bank = new Bank();
        Person alice = new Person("Alice", 30);
        Company acme = new Company("Acme Corp", "REG-12345");

        long a1 = bank.createAccount(alice, 1000);
        long a2 = bank.createAccount(acme, 5000);

        bank.transfer(a2, a1, 300);
        System.out.println(String.format("Alice balance: %f",bank.getAccount(a1).getBalance()));
    }
    
}
