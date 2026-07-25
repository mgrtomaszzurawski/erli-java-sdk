package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

import java.util.List;

/**
 * A partial update of an attachment ({@code PATCH /dictionaries/attachment}). Only the fields set on
 * the builder are sent; an unset field is left untouched by the API rather than cleared.
 *
 * @param id the attachment to update; required
 * @param filePath the new storage path, or {@code null} to leave it
 * @param name the new display name, or {@code null} to leave it
 * @param originalName the new original file name, or {@code null} to leave it
 * @param markets the new storefront list, or {@code null} to leave it
 */
public record AttachmentUpdate(
        long id,
        String filePath,
        String name,
        String originalName,
        List<Market> markets) {

    public AttachmentUpdate {
        if (markets != null) {
            markets = List.copyOf(markets);
            if (markets.isEmpty()) {
                // null means "leave the markets alone"; an empty array would ask the API to clear
                // them, which it rejects (minItems: 1). Fail here rather than on the wire.
                throw new IllegalArgumentException(
                        "markets must name at least one storefront; leave it unset to keep the current ones");
            }
        }
    }

    public static Builder builder(long id) {
        return new Builder(id);
    }

    /** Builder for {@link AttachmentUpdate}. */
    public static final class Builder {

        private final long id;
        private String filePath;
        private String name;
        private String originalName;
        private List<Market> markets;

        private Builder(long id) {
            this.id = id;
        }

        public Builder filePath(String value) {
            this.filePath = value;
            return this;
        }

        public Builder name(String value) {
            this.name = value;
            return this;
        }

        public Builder originalName(String value) {
            this.originalName = value;
            return this;
        }

        public Builder markets(List<Market> value) {
            this.markets = value == null ? null : List.copyOf(value);
            return this;
        }

        public AttachmentUpdate build() {
            return new AttachmentUpdate(id, filePath, name, originalName, markets);
        }
    }
}
