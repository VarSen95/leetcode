package banking;

import java.util.UUID;

public class AuditingBank implements BankInterface {
    private final BankInterface wrapped;
    private final TransactionHistoryLogger historyLogger;
    private final RequestIdLogger requestIdLogger;

    public AuditingBank(BankInterface wrapped, TransactionHistoryLogger historyLogger,
            RequestIdLogger requestIdLogger) {
        this.wrapped = wrapped;
        this.historyLogger = historyLogger;
        this.requestIdLogger = requestIdLogger;
    }

    // Just delegate — no logging needed
    @Override
    public Account getAccount(Long accountNumber) {
        return wrapped.getAccount(accountNumber);
    }

    @Override
    public Long openCommercialAccount(Company company, int pin, double startingDeposit, Currency currency) {
        return wrapped.openCommercialAccount(company, pin, startingDeposit, currency);
    }

    @Override
    public Long openConsumerAccount(Person person, int pin, double startingDeposit, Currency currency) {
        return wrapped.openConsumerAccount(person, pin, startingDeposit, currency);
    }

    @Override
    public boolean authenticateUser(Long accountNumber, int pin) {
        return wrapped.authenticateUser(accountNumber, pin);
    }

    @Override
    public double getBalance(Long accountNumber) {
        return wrapped.getBalance(accountNumber);
    }

    // Log these — money movement
    @Override
    public void credit(Long accountNumber, double amount) {
        String reqId = UUID.randomUUID().toString();
        RequestContext.setRequestId(reqId);
        try {
            requestIdLogger.log(reqId);
            wrapped.credit(accountNumber, amount);
            boolean success = amount > 0 && wrapped.getAccount(accountNumber) != null;
            historyLogger.log(new TransactionRecord(accountNumber, amount, TransactionType.CREDIT, success));
        } finally {
            RequestContext.clear();
        }
    }

    @Override
    public boolean debit(Long accountNumber, double amount) {
        String reqId = UUID.randomUUID().toString();
        RequestContext.setRequestId(reqId);
        try {
            requestIdLogger.log(reqId);
            boolean success = wrapped.debit(accountNumber, amount);
            historyLogger.log(new TransactionRecord(accountNumber, amount, TransactionType.DEBIT, success));
            return success;
        } finally {
            RequestContext.clear();
        }
    }
}
