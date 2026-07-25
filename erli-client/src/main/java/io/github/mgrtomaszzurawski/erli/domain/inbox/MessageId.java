package io.github.mgrtomaszzurawski.erli.domain.inbox;

/**
 * Identifier of an inbox message. Observed as a 24-character hexadecimal id; the length is not
 * enforced here so a future id shape cannot break decoding. Owned by the inbox domain — no other
 * bucket references messages.
 *
 * @param value the non-blank message id
 */
public record MessageId(String value) {

    public MessageId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("MessageId must not be null or blank");
        }
        value = value.trim();
    }

    public static MessageId of(String value) {
        return new MessageId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
