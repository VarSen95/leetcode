package banking;

/**
 * Private Variables:<br>
 * {@link #accountNumber}: Long<br>
 * {@link #bank}: BankInterface<br>
 * <br>
 *
 * Design note:
 * - Authentication happens once at construction; further operations assume
 * validity.
 */
public class Transaction implements TransactionInterface {
    private Long accountNumber;
    // FOLLOW UP: Use BankInterface (not Bank) to keep Transaction extensible and
    // easier to test.
    private BankInterface bank;

    private Long timestamp;

    /**
     * @param bank          The bank where the account is housed.
     * @param accountNumber The customer's account number.
     * @param attemptedPin  The PIN entered by the customer.
     * @throws Exception Account validation failed.
     */
    public Transaction(BankInterface bank, Long accountNumber, int attemptedPin) throws Exception {
        this.bank = bank;
        this.accountNumber = accountNumber;

        if (!bank.authenticateUser(accountNumber, attemptedPin)) {
            // FOLLOW UP: Change the exception type
            throw new NotAuthorizedException("User us not authorized");
        }
    }

    public double getBalance() {
        return bank.getBalance(accountNumber);
    }

    public void credit(double amount) {
        bank.credit(accountNumber, amount);
    }

    public boolean debit(double amount) {
        return bank.debit(accountNumber, amount);
    }

    // FOLLOW UP: Add timestamp
    public long getTimestamp() {
        return this.timestamp;
    }
}
