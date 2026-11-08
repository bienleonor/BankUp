package exception;

/**
 * Thrown when a requested account number does not exist in the database.
 */
public class AccountNotFoundException extends Exception {

    public AccountNotFoundException(String accountNumber) {
        super("Account not found: " + accountNumber);
    }

    public AccountNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
