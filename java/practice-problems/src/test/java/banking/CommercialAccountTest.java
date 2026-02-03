package banking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CommercialAccountTest {

    @Test
    public void testAuthorizedUsers() {
        Company company = new Company("Globex", 707);
        CommercialAccount account = new CommercialAccount(company, 3L, 7777, 1000.0);

        Person user = new Person("Elon", "Musk", 808);

        assertFalse(account.isAuthorizedUser(user));
        account.addAuthorizedUser(user);
        assertTrue(account.isAuthorizedUser(user));

        // adding same user again should not break behavior
        account.addAuthorizedUser(user);
        assertTrue(account.isAuthorizedUser(user));

        // null should not be authorized
        assertFalse(account.isAuthorizedUser(null));
    }
}
