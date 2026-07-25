package io.github.mgrtomaszzurawski.erli.domain.inbox;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * One message in the shop's inbox: an event Erli raised for the shop, with the event's body in
 * {@link #payload()}.
 *
 * <p>The inbox is the pull-based counterpart of the webhook subscriptions in
 * {@code io.github.mgrtomaszzurawski.erli.domain.hooks} — a shop that cannot expose an HTTP endpoint
 * polls {@code client.inbox()} instead, and acknowledges what it processed.
 *
 * @param id       the message's id, used to acknowledge it
 * @param shopId   the shop the message belongs to
 * @param created  when Erli raised the event
 * @param read     whether the message has already been acknowledged
 * @param type     what the message announces, or {@link MessageType#UNKNOWN} for a type this SDK
 *                 version does not know
 * @param typeName the raw type as the API sent it — the value to report when {@code type} is unknown
 * @param payload  the event body, empty only when {@code type} is {@link MessageType#UNKNOWN} and the
 *                 SDK therefore cannot say how to read it
 */
public record Message(
        MessageId id,
        long shopId,
        OffsetDateTime created,
        boolean read,
        MessageType type,
        String typeName,
        Optional<MessagePayload> payload) {

    public Message {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(created, "created");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(typeName, "typeName");
        Objects.requireNonNull(payload, "payload");
    }

    /** The payload as an order event, or empty when this message is not an {@code ORDER_*} one. */
    public Optional<OrderEvent> orderEvent() {
        return payload.filter(OrderEvent.class::isInstance).map(OrderEvent.class::cast);
    }

    /** The payload as a product-sync event, or empty when this message is not a sync one. */
    public Optional<ProductsSyncEvent> productsSyncEvent() {
        return payload.filter(ProductsSyncEvent.class::isInstance).map(ProductsSyncEvent.class::cast);
    }
}
