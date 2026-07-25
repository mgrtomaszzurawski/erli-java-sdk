package io.github.mgrtomaszzurawski.erli.internal.client.inbox;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.mgrtomaszzurawski.erli.core.error.ErliTransportException;
import io.github.mgrtomaszzurawski.erli.domain.inbox.InboxAccess;
import io.github.mgrtomaszzurawski.erli.domain.inbox.Message;
import io.github.mgrtomaszzurawski.erli.domain.inbox.MessageQuery;
import io.github.mgrtomaszzurawski.erli.domain.inbox.ReadReceipt;
import io.github.mgrtomaszzurawski.erli.internal.ApiPaths;
import io.github.mgrtomaszzurawski.erli.internal.HttpRuntime;
import io.github.mgrtomaszzurawski.erli.internal.JsonCodec;
import io.github.mgrtomaszzurawski.erli.internal.QueryParameters;

import java.util.List;
import java.util.Objects;

/**
 * {@link InboxAccess} implementation over the shared {@link HttpRuntime}.
 *
 * <p>Both list operations decode into a {@link JsonNode} array rather than the generated envelope type,
 * because a message's payload branch can only be chosen once its {@code type} is known — see
 * {@link MessageMapper}. The tree never leaves this package.
 *
 * <p>These operations are not paginated: the API returns at most
 * {@value InboxAccess#MAX_MESSAGES_PER_CALL} oldest-first messages and exposes no cursor for them,
 * so there is no page walk to build on the shared cursor pagination.
 *
 * <p>Internal: never exported.
 */
public final class InboxAccessImpl implements InboxAccess {

    private static final String FIELD_ID = "id";

    private final HttpRuntime runtime;
    private final JsonCodec codec;

    public InboxAccessImpl(HttpRuntime runtime, JsonCodec codec) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.codec = Objects.requireNonNull(codec, "codec");
    }

    @Override
    public List<Message> unread() {
        List<JsonNode> rawMessages = runtime.getList(ApiPaths.INBOX, QueryParameters.empty(), JsonNode.class);
        return toDomain(rawMessages);
    }

    @Override
    public List<Message> search(MessageQuery query) {
        Objects.requireNonNull(query, "query");
        List<JsonNode> rawMessages =
                runtime.postList(ApiPaths.INBOX_SEARCH, MessageMapper.toRaw(query), JsonNode.class);
        return toDomain(rawMessages);
    }

    @Override
    public int markRead(ReadReceipt receipt) {
        Objects.requireNonNull(receipt, "receipt");
        Integer marked =
                runtime.post(ApiPaths.INBOX_MARK_READ, MessageMapper.toRaw(receipt), Integer.class);
        // The operation answers with a bare integer count; an empty body means nothing was marked.
        return marked == null ? 0 : marked;
    }

    /** An absent (rather than empty) JSON array is treated as a drained inbox. */
    private List<Message> toDomain(List<JsonNode> rawMessages) {
        if (rawMessages == null) {
            return List.of();
        }
        return rawMessages.stream().map(this::toDomain).toList();
    }

    /**
     * Failing the batch is deliberate — the fleet's decision is to fail loud on wire data the vendored
     * spec does not describe, rather than silently drop a message a shop is expected to act on (see
     * {@code KNOWN-SERVER-BEHAVIORS.md}). But an unacknowledged message is returned again by the next
     * call, so a message this SDK version cannot map would stall the drain loop forever with no clue
     * which one it was. Naming the offending id turns that dead end into something a caller can act on:
     * acknowledge that id and carry on.
     */
    private Message toDomain(JsonNode messageNode) {
        JsonNode idNode = messageNode.get(FIELD_ID);
        try {
            return MessageMapper.toDomain(messageNode, codec);
        } catch (RuntimeException failure) {
            String messageId = idNode == null ? "unknown" : idNode.asText();
            throw new ErliTransportException(
                    "Could not map inbox message " + messageId + "; acknowledge it with markRead to skip it",
                    failure);
        }
    }
}
