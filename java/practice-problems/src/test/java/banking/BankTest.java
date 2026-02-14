package banking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BankTest {

    private Bank newBank() {
        return new Bank(new InMemoryExchangeRateService());
    }

    @Test
    public void testOpenAccountsAndAuthenticate() {
        Bank bank = newBank();

        Long consumerAccount = bank.openConsumerAccount(new Person("Ada", "Lovelace", 101), 1234, 50.0, Currency.USD);
        Long commercialAccount = bank.openCommercialAccount(new Company("Acme", 202), 9999, 500.0, Currency.USD);

        assertNotNull(consumerAccount);
        assertNotNull(commercialAccount);
        assertNotEquals(consumerAccount, commercialAccount);

        assertTrue(bank.authenticateUser(consumerAccount, 1234));
        assertFalse(bank.authenticateUser(consumerAccount, 1111));
        assertTrue(bank.authenticateUser(commercialAccount, 9999));
    }

    @Test
    public void testGetBalanceCreditDebit() {
        Bank bank = newBank();
        Long accountNumber = bank.openConsumerAccount(new Person("Grace", "Hopper", 303), 2468, 100.0, Currency.USD);

        assertEquals(100.0, bank.getBalance(accountNumber), 0.0001);

        bank.credit(accountNumber, 50.0);
        assertEquals(150.0, bank.getBalance(accountNumber), 0.0001);

        boolean debited = bank.debit(accountNumber, 70.0);
        assertTrue(debited);
        assertEquals(80.0, bank.getBalance(accountNumber), 0.0001);
    }

    @Test
    public void testDebitInsufficientFunds() {
        Bank bank = newBank();
        Long accountNumber = bank.openConsumerAccount(new Person("Alan", "Turing", 404), 1357, 10.0, Currency.USD);

        boolean debited = bank.debit(accountNumber, 50.0);
        assertFalse(debited);
        assertEquals(10.0, bank.getBalance(accountNumber), 0.0001);
    }

    @Test
    public void testMissingAccountBehavior() {
        Bank bank = newBank();
        Long missing = 999999999L;

        assertFalse(bank.authenticateUser(missing, 1234));
        assertThrows(AccountNotFoundException.class, () -> bank.getBalance(missing));
        assertThrows(AccountNotFoundException.class, () -> bank.debit(missing, 10.0));
        assertThrows(AccountNotFoundException.class, () -> bank.credit(missing, 10.0));
    }
}
