package exception;
/**
 * Thrown when a transaction request violates a business rule not covered by
 * {@link InsufficientBalanceException} or {@link AccountNotFoundException}.
 *
 * <p>Examples: zero/negative amount, or transfer sender equals receiver.
 */
public class InvalidTransactionException extends Exception {

    public InvalidTransactionException(String message) {
        super(message);
    }

    public InvalidTransactionException(String message, Throwable cause) {
        super(message, cause);
    }
}
