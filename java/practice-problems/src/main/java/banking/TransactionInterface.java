package banking;

/**
 * Interface defining transaction operations.
 */
public interface TransactionInterface {
    /**
     * Get the balance of the account.
     * 
     * @return Account balance
     */
    double getBalance();

    /**
     * Credit (add money to) the account.
     * 
     * @param amount Amount to credit
     */
    void credit(double amount);

    /**
     * Debit (withdraw money from) the account.
     * 
     * @param amount Amount to debit
     * @return true if successful, false if insufficient funds
     */
    boolean debit(double amount);
}
