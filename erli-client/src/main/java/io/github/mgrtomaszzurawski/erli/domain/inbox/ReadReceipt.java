package io.github.mgrtomaszzurawski.erli.domain.inbox;

import java.util.List;
import java.util.Objects;

/**
 * What to acknowledge as read. The API accepts two alternatives, and this sealed interface keeps them
 * distinct instead of hiding both behind one nullable-field record.
 *
 * <p>Prefer {@link UpTo} while draining the inbox in order: the messages arrive oldest-first, so one
 * call acknowledges the whole batch. Use {@link Exactly} when only some messages were processed
 * successfully.
 */
public sealed interface ReadReceipt {

    /** Acknowledge every message up to and including {@code lastMessageId}. */
    static ReadReceipt upTo(MessageId lastMessageId) {
        return new UpTo(lastMessageId);
    }

    /** Acknowledge exactly the listed messages. */
    static ReadReceipt exactly(List<MessageId> ids) {
        return new Exactly(ids);
    }

    /**
     * Acknowledge the batch ending at one message.
     *
     * @param lastMessageId the newest message that was processed
     */
    record UpTo(MessageId lastMessageId) implements ReadReceipt {

        public UpTo {
            Objects.requireNonNull(lastMessageId, "lastMessageId");
        }
    }

    /**
     * Acknowledge an explicit set of messages.
     *
     * @param ids the messages that were processed, at least one
     */
    record Exactly(List<MessageId> ids) implements ReadReceipt {

        public Exactly {
            Objects.requireNonNull(ids, "ids");
            if (ids.isEmpty()) {
                throw new IllegalArgumentException("ids must name at least one message");
            }
            ids = List.copyOf(ids);
        }
    }
}
