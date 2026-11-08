package model;

public enum TransactionType {

    DEPOSIT,
    WITHDRAW,
    TRANSFER_OUT,
    TRANSFER_IN;

    /** Returns a display label for console output, e.g. {@code "TRANSFER OUT"}. */
    public String displayName() {
        return name().replace('_', ' ');
    }
}