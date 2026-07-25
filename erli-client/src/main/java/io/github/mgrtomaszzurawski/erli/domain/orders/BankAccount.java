package io.github.mgrtomaszzurawski.erli.domain.orders;

/**
 * The account a refund is to be paid into.
 *
 * <p><strong>Personal data.</strong> An account number plus a holder name identifies a natural person
 * and is a payment credential, so {@link #toString()} renders nothing but the type name.
 *
 * @param number the 26-digit Polish account number
 * @param name   the account holder's name
 */
public record BankAccount(String number, String name) {

    private static final String REDACTED_RENDERING = "BankAccount[REDACTED]";

    /** Redacted: an account number must never reach a log through a string rendering. */
    @Override
    public String toString() {
        return REDACTED_RENDERING;
    }
}
