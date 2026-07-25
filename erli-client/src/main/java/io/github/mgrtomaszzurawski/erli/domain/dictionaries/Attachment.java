package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A document attached to products — a user manual, an energy label, a safety data sheet. Uploaded
 * once and then attached to any number of products.
 *
 * @param id the attachment identifier
 * @param shopId the shop that owns it
 * @param version the optimistic-concurrency version
 * @param name the display name
 * @param originalName the original file name as uploaded
 * @param filePath the storage path; combine with {@link #baseUrl()} to reach the file
 * @param kind what kind of document it is, when stated
 * @param baseUrl the base URL the file is served from, when stated
 * @param type the MIME-ish type string the API reports, when stated
 * @param attachedProductIds the products this attachment is currently attached to
 * @param created who created it and when
 * @param updated who last changed it and when, when it has been changed
 */
public record Attachment(
        long id,
        long shopId,
        long version,
        String name,
        String originalName,
        String filePath,
        Optional<AttachmentKind> kind,
        Optional<String> baseUrl,
        Optional<String> type,
        List<Long> attachedProductIds,
        AttachmentAudit created,
        Optional<AttachmentAudit> updated) {

    public Attachment {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(originalName, "originalName");
        Objects.requireNonNull(filePath, "filePath");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(baseUrl, "baseUrl");
        Objects.requireNonNull(type, "type");
        attachedProductIds = List.copyOf(Objects.requireNonNull(attachedProductIds, "attachedProductIds"));
        Objects.requireNonNull(created, "created");
        Objects.requireNonNull(updated, "updated");
    }

    /**
     * Who touched the attachment, and when.
     *
     * @param time when the change happened
     * @param userId the acting user's identifier
     * @param email the acting user's e-mail, when the API reports one
     */
    public record AttachmentAudit(OffsetDateTime time, String userId, Optional<String> email) {

        private static final String REDACTED = "***";

        public AttachmentAudit {
            Objects.requireNonNull(time, "time");
            Objects.requireNonNull(userId, "userId");
            Objects.requireNonNull(email, "email");
        }

        /** Redacts the e-mail: this is an identifiable person, and audit rows end up in logs. */
        @Override
        public String toString() {
            return "AttachmentAudit[time=" + time
                    + ", userId=" + userId
                    + ", email=" + (email.isPresent() ? REDACTED : Optional.empty())
                    + "]";
        }
    }
}
