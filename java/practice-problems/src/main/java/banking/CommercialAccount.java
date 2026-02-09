package banking;

import java.util.ArrayList;
import java.util.List;

/**
 * Account implementation for commercial (business) customers.<br>
 * <br>
 * <p>
 * Private Variables:<br>
 * {@link #authorizedUsers}: List&lt;Person&gt;<br>
 */
public class CommercialAccount extends Account {
    private List<Person> authorizedUsers;

    public CommercialAccount(Company company, Long accountNumber, int pin, double startingDeposit, Currency currency) {
        super(company, accountNumber, pin, startingDeposit, currency);
        this.authorizedUsers = new ArrayList<>();
    }

    /**
     * @param person The authorized user to add to the account.
     */
    protected synchronized void addAuthorizedUser(Person person) {
        if (person != null && !authorizedUsers.contains(person)) {
            authorizedUsers.add(person);
        }
    }

    /**
     * @param person
     * @return true if person matches an authorized user in
     *         {@link #authorizedUsers}; otherwise, false.
     */
    public synchronized boolean isAuthorizedUser(Person person) {
        return person != null && authorizedUsers.contains(person);
    }
}
