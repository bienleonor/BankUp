package exception;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * Thrown when a withdrawal or transfer is attempted but the account balance
 * is less than the requested amount.
 */
@Getter
public class InsufficientBalanceException extends Exception {

    private final BigDecimal availableBalance;
    private final BigDecimal requestedAmount;

    public InsufficientBalanceException(BigDecimal availableBalance, BigDecimal requestedAmount) {
        super(String.format(
                "Insufficient balance. Available: %.2f, Requested: %.2f",
                availableBalance, requestedAmount
        ));
        this.availableBalance = availableBalance;
        this.requestedAmount  = requestedAmount;
    }

}