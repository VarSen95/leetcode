package banking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AccountTest {

    @Test
    public void testValidatePinAndBalanceOperations() {
        Person owner = new Person("Katherine", "Johnson", 505);
        Account account = new ConsumerAccount(owner, 1L, 4321, 25.0);

        assertTrue(account.validatePin(4321));
        assertFalse(account.validatePin(1111));

        account.creditAccount(10.0);
        assertEquals(35.0, account.getBalance(), 0.0001);

        assertTrue(account.debitAccount(5.0));
        assertEquals(30.0, account.getBalance(), 0.0001);

        assertFalse(account.debitAccount(100.0));
        assertEquals(30.0, account.getBalance(), 0.0001);

        account.creditAccount(-10.0);
        assertEquals(30.0, account.getBalance(), 0.0001);
    }

    @Test
    public void testGetAccountHolder() {
        Person owner = new Person("Mary", "Jackson", 606);
        Account account = new ConsumerAccount(owner, 2L, 5555, 0.0);

        assertSame(owner, account.getAccountHolder());
    }
}
