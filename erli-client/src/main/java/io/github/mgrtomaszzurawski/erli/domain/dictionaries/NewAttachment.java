package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

import java.util.List;
import java.util.Objects;

/**
 * A request to create an attachment ({@code POST /dictionaries/attachment}). Build one with
 * {@link #builder()}; every field the API requires is required here too, so a missing one fails
 * before the request is sent rather than as a 400.
 *
 * <p>The file itself is uploaded out of band; {@code filePath} refers to the already-stored file.
 *
 * @param kind what kind of document this is
 * @param name the display name
 * @param originalName the original file name
 * @param filePath the storage path of the uploaded file
 * @param markets the storefronts the attachment applies to; at least one
 */
public record NewAttachment(
        AttachmentKind kind,
        String name,
        String originalName,
        String filePath,
        List<Market> markets) {

    public NewAttachment {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(originalName, "originalName");
        Objects.requireNonNull(filePath, "filePath");
        markets = List.copyOf(Objects.requireNonNull(markets, "markets"));
        if (markets.isEmpty()) {
            throw new IllegalArgumentException("markets must name at least one storefront");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link NewAttachment}. */
    public static final class Builder {

        private AttachmentKind kind;
        private String name;
        private String originalName;
        private String filePath;
        private List<Market> markets = List.of();

        private Builder() {
        }

        public Builder kind(AttachmentKind value) {
            this.kind = value;
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

        public Builder filePath(String value) {
            this.filePath = value;
            return this;
        }

        public Builder markets(List<Market> value) {
            this.markets = value == null ? List.of() : List.copyOf(value);
            return this;
        }

        public Builder market(Market value) {
            return markets(List.of(Objects.requireNonNull(value, "market")));
        }

        public NewAttachment build() {
            return new NewAttachment(kind, name, originalName, filePath, markets);
        }
    }
}
