package banking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionTest {

    @Test
    public void testTransactionWithValidPin() throws Exception {
        Bank bank = new Bank(new InMemoryExchangeRateService());
        Long accountNumber = bank.openConsumerAccount(new Person("Ada", "Byron", 909), 2468, 200.0, Currency.USD);

        Transaction txn = new Transaction(bank, accountNumber, 2468);
        assertEquals(200.0, txn.getBalance(), 0.0001);

        txn.credit(50.0);
        assertEquals(250.0, txn.getBalance(), 0.0001);

        assertTrue(txn.debit(100.0));
        assertEquals(150.0, txn.getBalance(), 0.0001);
    }

    @Test
    public void testTransactionWithInvalidPin() {
        Bank bank = new Bank(new InMemoryExchangeRateService());
        Long accountNumber = bank.openConsumerAccount(new Person("Ada", "Byron", 910), 2468, 200.0, Currency.USD);

        assertThrows(Exception.class, () -> new Transaction(bank, accountNumber, 1111));
    }
}
