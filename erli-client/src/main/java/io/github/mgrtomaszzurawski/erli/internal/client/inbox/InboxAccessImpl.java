package io.github.mgrtomaszzurawski.erli.internal.client.inbox;

import com.fasterxml.jackson.databind.JsonNode;
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
        return rawMessages.stream()
                .map(messageNode -> MessageMapper.toDomain(messageNode, codec))
                .toList();
    }
}
