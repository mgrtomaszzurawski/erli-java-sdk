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
    private static final String UNIDENTIFIED_MESSAGE = "with no id";

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
     * Failing the batch rather than dropping the message is deliberate: a shop is expected to act on
     * every inbox message, so one the SDK cannot represent must be visible, not swallowed.
     *
     * <p>What reaches here narrowed under CORE-12. An unrecognised <em>enum</em> value no longer fails
     * the response — the codec decodes it to {@code null}, so an optional one simply becomes an empty
     * {@code Optional} and only a <em>required</em> one still stops the mapping. What is left are
     * genuinely unmappable messages: a required property absent or unrecognised, or a payload whose
     * shape does not match its {@code type}.
     *
     * <p>An unacknowledged message is returned again by the next call, so such a message would stall
     * the drain loop forever with no clue which one it was. Naming the offending id turns that dead end
     * into something a caller can act on: acknowledge that id and carry on.
     */
    private Message toDomain(JsonNode messageNode) {
        JsonNode idNode = messageNode.get(FIELD_ID);
        String messageId = idNode == null ? UNIDENTIFIED_MESSAGE : idNode.asText();
        try {
            return MessageMapper.toDomain(messageNode, codec);
        } catch (IllegalStateException | IllegalArgumentException | ErliTransportException failure) {
            // Deliberately narrow: anything else is an SDK bug, and labelling it "unmappable wire data"
            // would send the caller looking in the wrong place.
            throw new ErliTransportException(
                    "Could not map inbox message " + messageId + "; acknowledge it with markRead to skip it",
                    failure);
        }
    }
}
