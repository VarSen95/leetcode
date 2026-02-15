package bankingPractice;

import java.util.ArrayList;
import java.util.List;

/**
 * Account implementation for commercial (business) customers.<br>
 * <br>
 * <p>
 * Private Variables:<br>
 * {@link #authorizedUsers}: List&lt;Person&gt;<br>
 */

// ISSUE: synchronized methods lock on 'this' — the same CommercialAccount
// instance.
// This means addAuthorizedUser and isAuthorizedUser share a lock with
// creditAccount, debitAccount, and getBalance from the parent Account class.
// Adding an authorized user blocks debits/credits even though they're
// completely unrelated operations.
//
// FIX: Use a separate lock object (private final Object userLock = new
// Object())
// and synchronize on that instead. This way user operations and balance
// operations can happen concurrently without blocking each other.


// IMPROVED VERSION: Uses a separate lock object so that user operations
// (addAuthorizedUser, isAuthorizedUser) don't block balance operations
// (creditAccount, debitAccount, getBalance) which lock on 'this'.
//
// public class CommercialAccount extends Account {
//     private final List<Person> authorizedUsers;
//     private final Object userLock = new Object();  // separate lock
//
//     public CommercialAccount(Company company, Long accountNumber, int pin, double startingDeposit) {
//         super(company, accountNumber, pin, startingDeposit);
//         this.authorizedUsers = new ArrayList<>();
//     }
//
//     protected void addAuthorizedUser(Person person) {
//         synchronized (userLock) {  // doesn't block balance operations
//             if (person != null && !authorizedUsers.contains(person)) {
//                 authorizedUsers.add(person);
//             }
//         }
//     }
//
//     public boolean isAuthorizedUser(Person person) {
//         synchronized (userLock) {  // doesn't block balance operations
//             return person != null && authorizedUsers.contains(person);
//         }
//     }
// }
public class CommercialAccount extends Account {
    private List<Person> authorizedUsers;

    public CommercialAccount(Company company, Long accountNumber, int pin, double startingDeposit) {
        super(company, accountNumber, pin, startingDeposit);
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