package bankingPractice;

/**
 * The public methods for the {@link Bank} class.
 */
public interface BankInterface {

    /**
     * Retrieves the account with the specified account number.
     * 
     * @param accountNumber The account number.
     * @return The account for the given account number.
     */
    AccountDTO getAccount(Long accountNumber);

    /**
     * Creates a new account and adds it to {@link Bank#accounts}.
     *
     * @param company
     * @param pin
     * @param startingDeposit
     * @return The account number for the newly created account.
     */
    Long openCommercialAccount(Company company, int pin, double startingDeposit);

    /**
     * Creates a new account and adds it to {@link Bank#accounts}.
     *
     * @param person
     * @param pin
     * @param startingDeposit
     * @return The account number for the newly created account.
     */
    Long openConsumerAccount(Person person, int pin, double startingDeposit);

    /**
     * @param accountNumber The account number for the transaction.
     * @param pin
     * @return true if authentication was successful.
     */
    boolean authenticateUser(Long accountNumber, int pin);

    /**
     * @param accountNumber The account number for the transaction.
     * @return the balance of the account.
     */
    double getBalance(Long accountNumber);

    /**
     * @param accountNumber The account number for the transaction.
     * @param amount        The amount of money being deposited.
     */
    void credit(Long accountNumber, double amount);

    /**
     * @param accountNumber The account number for the transaction.
     * @param amount
     * @return true if amount could be withdrawn; otherwise, return false.
     */
    boolean debit(Long accountNumber, double amount);

    /**
     * @param sourceAccount      Source account number.
     * @param destinationAccount Destination account number.
     * @param amount             Amount to transfer.
     * @return true if both debit and credit succeeded; otherwise, false.
     */
    boolean transfer(Long sourceAccount, Long destinationAccount, double amount);
}
