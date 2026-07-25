package io.github.mgrtomaszzurawski.erli.internal.client.inbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.mgrtomaszzurawski.erli.core.model.ProductExternalId;
import io.github.mgrtomaszzurawski.erli.domain.inbox.Message;
import io.github.mgrtomaszzurawski.erli.domain.inbox.MessageId;
import io.github.mgrtomaszzurawski.erli.domain.inbox.MessagePayload;
import io.github.mgrtomaszzurawski.erli.domain.inbox.MessageQuery;
import io.github.mgrtomaszzurawski.erli.domain.inbox.MessageType;
import io.github.mgrtomaszzurawski.erli.domain.inbox.ProductsSyncEvent;
import io.github.mgrtomaszzurawski.erli.domain.inbox.ReadReceipt;
import io.github.mgrtomaszzurawski.erli.internal.JsonCodec;
import io.github.mgrtomaszzurawski.erli.rest.model.MarkRead;
import io.github.mgrtomaszzurawski.erli.rest.model.MarkReadAnyOf;
import io.github.mgrtomaszzurawski.erli.rest.model.MarkReadAnyOf1;
import io.github.mgrtomaszzurawski.erli.rest.model.MessagePayloadAnyOf;
import io.github.mgrtomaszzurawski.erli.rest.model.MessagePayloadAnyOf3;
import io.github.mgrtomaszzurawski.erli.rest.model.MessageRequest;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Maps inbox messages between the wire and the domain. Internal.
 *
 * <p>The Layer-1 envelope type is referenced fully qualified below: it shares its simple name with the
 * domain {@link Message} this mapper produces, and the domain name is the one that should read plainly
 * here.
 *
 * <p><strong>Why this mapper works from a tree.</strong> A message's {@code payload} is declared as an
 * {@code anyOf} over four branches — three identical order shapes and one product-sync shape — and the
 * real discriminator is the sibling {@code type} field, not anything inside the payload. The generated
 * {@code anyOf} wrapper cannot use that: it tries the branches in declaration order and keeps the first
 * that deserializes, and because the codec ignores unknown properties the <em>first</em> branch always
 * succeeds. A product-sync payload would bind to the order branch with every field null and its product
 * ids would be lost silently. So the response is decoded as a tree and this mapper binds the payload
 * subtree to the branch that {@code type} names.
 */
final class MessageMapper {

    private static final String FIELD_PAYLOAD = "payload";

    private MessageMapper() {
    }

    static Message toDomain(JsonNode messageNode, JsonCodec codec) {
        Objects.requireNonNull(messageNode, "message node");
        JsonNode payloadNode = messageNode.get(FIELD_PAYLOAD);
        io.github.mgrtomaszzurawski.erli.rest.model.Message rawMessage = codec.convert(
                withoutPayload(messageNode), io.github.mgrtomaszzurawski.erli.rest.model.Message.class);
        String typeName = requireText(rawMessage.getType(), "type");
        MessageType type = MessageType.fromWireValue(typeName).orElse(MessageType.UNKNOWN);
        return new Message(
                MessageId.of(requireText(rawMessage.getId(), "id")),
                requirePresent(rawMessage.getShopId(), "shopId").longValue(),
                requirePresent(rawMessage.getCreated(), "created"),
                requirePresent(rawMessage.getRead(), "read"),
                type,
                typeName,
                toPayload(type, payloadNode, codec));
    }

    static MessageRequest toRaw(MessageQuery query) {
        Objects.requireNonNull(query, "query");
        MessageRequest rawRequest = new MessageRequest();
        if (query.types().isEmpty()) {
            // An empty filter means "every type". The generated model initialises `types` to an empty
            // list, which would go on the wire as {"types":[]} — plausibly read as "match nothing".
            // The spec's documented default for this body is {}, so the field is cleared instead.
            rawRequest.types(null);
        } else {
            rawRequest.types(query.types().stream().map(MessageType::wireValue).sorted().toList());
        }
        return rawRequest;
    }

    static MarkRead toRaw(ReadReceipt receipt) {
        Objects.requireNonNull(receipt, "receipt");
        // An if-chain rather than a pattern switch: pattern matching in switch is a preview feature on
        // the SDK's Java 17 baseline. ReadReceipt is sealed, so these two cases are the whole domain.
        if (receipt instanceof ReadReceipt.UpTo upTo) {
            return new MarkRead(new MarkReadAnyOf().lastMessageId(upTo.lastMessageId().value()));
        }
        if (receipt instanceof ReadReceipt.Exactly exactly) {
            return new MarkRead(new MarkReadAnyOf1().ids(
                    exactly.ids().stream().map(MessageId::value).toList()));
        }
        throw new IllegalStateException("Unsupported ReadReceipt implementation: " + receipt.getClass());
    }

    /**
     * Bind the payload to the branch the message type names. An unknown type yields no payload rather
     * than a guess: binding it to a branch chosen at random would invent data.
     */
    private static Optional<MessagePayload> toPayload(MessageType type, JsonNode payloadNode, JsonCodec codec) {
        // An unknown type is answered before the payload is even inspected: the SDK has nothing to say
        // about a shape it does not know, so a message whose payload is missing or oddly shaped still
        // reaches the caller as UNKNOWN rather than failing the whole page.
        if (type == MessageType.UNKNOWN) {
            return Optional.empty();
        }
        if (payloadNode == null || payloadNode.isNull()) {
            throw new IllegalStateException("Message is missing the required 'payload' field");
        }
        if (type.carriesOrderEvent()) {
            MessagePayloadAnyOf rawOrder = codec.convert(payloadNode, MessagePayloadAnyOf.class);
            return Optional.of(OrderEventMapper.toDomain(rawOrder, payloadNode, codec));
        }
        if (type == MessageType.PRODUCTS_NEED_SYNC) {
            return Optional.of(toProductsSyncEvent(codec.convert(payloadNode, MessagePayloadAnyOf3.class)));
        }
        // Reached only if MessageType gains a constant without a payload shape being wired here.
        throw new IllegalStateException("No payload mapping is wired for message type " + type);
    }

    /**
     * The envelope without its payload. The generated envelope declares {@code payload} as the anyOf
     * wrapper, so binding the message as-is makes Jackson deep-bind the whole order snapshot into a
     * branch this mapper discards and re-binds itself — about half the mapping cost of a message, for
     * a value that is thrown away. The copy is shallow (child references are shared, not cloned) and
     * the caller's tree is left untouched.
     */
    private static JsonNode withoutPayload(JsonNode messageNode) {
        if (!(messageNode instanceof ObjectNode objectNode)) {
            return messageNode;
        }
        ObjectNode envelopeOnly = JsonNodeFactory.instance.objectNode().setAll(objectNode);
        envelopeOnly.remove(FIELD_PAYLOAD);
        return envelopeOnly;
    }

    private static ProductsSyncEvent toProductsSyncEvent(MessagePayloadAnyOf3 rawPayload) {
        List<String> rawProductIds = rawPayload.getExternalProductIds();
        if (rawProductIds == null || rawProductIds.isEmpty()) {
            throw new IllegalStateException(
                    "productsNeedSync payload is missing the required 'externalProductIds' field");
        }
        List<String> rawFields = rawPayload.getFields();
        return new ProductsSyncEvent(
                // The spec marks payload.id required, but the message observed live on the sandbox
                // carried no id at all (only the envelope id). Treated as optional rather than fatal.
                Optional.ofNullable(rawPayload.getId()),
                rawProductIds.stream().map(ProductExternalId::of).toList(),
                rawFields == null ? List.of() : List.copyOf(rawFields));
    }

    private static <T> T requirePresent(T value, String field) {
        if (value == null) {
            throw new IllegalStateException("Message is missing the required '" + field + "' field");
        }
        return value;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Message is missing the required '" + field + "' field");
        }
        return value;
    }
}
