package io.github.mgrtomaszzurawski.erli.domain.inbox;

import java.util.Objects;

/**
 * Where the buyer wants a refund paid.
 *
 * <p><strong>Buyer personal data.</strong> {@link #toString()} redacts both components — an account
 * number identifies a person and must not reach a log.
 *
 * @param number the 26-digit Polish account number
 * @param name   the account holder's name
 */
public record BankAccount(String number, String name) {

    private static final String REDACTED = "<redacted>";

    public BankAccount {
        Objects.requireNonNull(number, "number");
        Objects.requireNonNull(name, "name");
    }

    @Override
    public String toString() {
        return "BankAccount[number=" + REDACTED + ", name=" + REDACTED + "]";
    }
}
