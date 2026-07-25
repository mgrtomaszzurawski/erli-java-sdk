package io.github.mgrtomaszzurawski.erli.domain.inbox;

import java.util.List;

/**
 * Read the shop's event inbox and acknowledge what has been processed. Reached via
 * {@code client.inbox()}.
 *
 * <p>The inbox is Erli's pull-based delivery of the same events the webhook subscriptions push. The
 * loop is: fetch unread messages, process them, acknowledge them with
 * {@link #markRead(ReadReceipt)} — until then the same messages come back.
 *
 * <p>The API returns <strong>at most 500 messages, oldest first</strong>, and offers no cursor for
 * these operations, so the SDK returns a plain list rather than a lazy stream: draining the inbox means
 * calling {@link #unread()} again after acknowledging.
 *
 * <p>Public surface — consumers import only this package.
 */
public interface InboxAccess {

    /**
     * The oldest unread messages, up to the API's cap of {@value #MAX_MESSAGES_PER_CALL}
     * ({@code GET /inbox}).
     *
     * @return the unread messages, oldest first; empty when the inbox is drained
     */
    List<Message> unread();

    /**
     * The oldest unread messages of the requested types, up to the same cap
     * ({@code POST /inbox/_search}).
     *
     * @param query which types to return
     * @return the matching unread messages, oldest first
     */
    List<Message> search(MessageQuery query);

    /**
     * Acknowledge messages as read ({@code POST /inbox/mark-read}). Acknowledged messages stop being
     * returned by {@link #unread()} and {@link #search(MessageQuery)}.
     *
     * @param receipt which messages were processed
     * @return how many messages the API marked as read
     */
    int markRead(ReadReceipt receipt);

    /** The API's cap on how many messages one call returns. */
    int MAX_MESSAGES_PER_CALL = 500;
}
