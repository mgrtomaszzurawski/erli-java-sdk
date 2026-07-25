package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

/**
 * Optional server-side filters for {@link DictionariesAccess#attachments(AttachmentQuery)}.
 * A {@code null} component means "do not filter on this"; use {@link #none()} for every attachment.
 *
 * @param id keep only the attachment with this identifier
 * @param kind keep only attachments of this kind
 * @param name keep only attachments whose name contains this fragment, case-insensitively
 */
public record AttachmentQuery(Long id, AttachmentKind kind, String name) {

    private static final AttachmentQuery NONE = new AttachmentQuery(null, null, null);

    /** The empty query — every attachment. */
    public static AttachmentQuery none() {
        return NONE;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link AttachmentQuery}. */
    public static final class Builder {

        private Long id;
        private AttachmentKind kind;
        private String name;

        private Builder() {
        }

        public Builder id(Long value) {
            this.id = value;
            return this;
        }

        public Builder kind(AttachmentKind value) {
            this.kind = value;
            return this;
        }

        public Builder name(String value) {
            this.name = value;
            return this;
        }

        public AttachmentQuery build() {
            return new AttachmentQuery(id, kind, name);
        }
    }
}
