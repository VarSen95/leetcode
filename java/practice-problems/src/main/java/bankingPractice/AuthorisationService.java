package bankingPractice;

/**
 * Lightweight authentication helper using AccountDao. Use this before
 * constructing {@link Transaction}; the constructor now assumes authentication
 * already succeeded.
 */
public class AuthorisationService {

    private final AccountDao accountDao;

    public AuthorisationService(AccountDao accountDao) {
        this.accountDao = accountDao;
    }

    public boolean authenticate(Long accountNumber, int attemptedPin) {
        AuthResult result = authenticateWithReason(accountNumber, attemptedPin);
        return result.isSuccess();
    }

    public AuthResult authenticateWithReason(Long accountNumber, int attemptedPin) {
        if (accountNumber == null) {
            return AuthResult.failure("Missing account");
        }
        AccountDTO account = accountDao.findById(accountNumber);
        if (account == null) {
            return AuthResult.failure("Account not found");
        }
        boolean ok = account.getPin() == attemptedPin;
        return ok ? AuthResult.success() : AuthResult.failure("Invalid credentials");
    }

    public static class AuthResult {
        private final boolean success;
        private final String message;

        private AuthResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static AuthResult success() {
            return new AuthResult(true, null);
        }

        public static AuthResult failure(String message) {
            return new AuthResult(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}
