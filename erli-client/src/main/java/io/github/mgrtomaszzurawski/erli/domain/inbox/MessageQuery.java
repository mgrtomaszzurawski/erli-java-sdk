package io.github.mgrtomaszzurawski.erli.domain.inbox;

import java.util.Set;

/**
 * Which unread messages {@code POST /inbox/_search} should return. An empty {@link #types()} asks for
 * every type, which is what {@code GET /inbox} does.
 *
 * <p>The API's filter enum does not accept {@link MessageType#ORDER_SELLER_STATUS_CHANGED} even though
 * messages of that type exist, so this record rejects it up front rather than letting the server
 * decide — see {@link MessageType#filterable()} and {@code KNOWN-SERVER-BEHAVIORS.md}.
 *
 * @param types the message types to return, or empty for all of them
 */
public record MessageQuery(Set<MessageType> types) {

    public MessageQuery {
        if (types == null) {
            throw new IllegalArgumentException("types must not be null; use MessageQuery.all() for no filter");
        }
        types.forEach(MessageQuery::requireFilterable);
        types = Set.copyOf(types);
    }

    /** Every unread message, unfiltered. */
    public static MessageQuery all() {
        return new MessageQuery(Set.of());
    }

    /** Only unread messages of the given types. */
    public static MessageQuery ofTypes(Set<MessageType> types) {
        return new MessageQuery(types);
    }

    private static void requireFilterable(MessageType type) {
        if (!type.filterable()) {
            throw new IllegalArgumentException(
                    "The inbox search filter does not accept message type " + type
                            + "; fetch all types and filter client-side");
        }
    }
}
