package io.github.mgrtomaszzurawski.erli.domain.shipping;

import java.util.Objects;
import java.util.Optional;

/**
 * Filters for {@link ShippingAccess#postingPoints(PostingPointQuery)}. Every filter is optional;
 * {@link #none()} fetches every posting point the shop has defined.
 *
 * @param id             match one posting-point id
 * @param groupId        match a delivery group
 * @param onlyDefault    restrict to the default posting point; the API accepts only {@code true} here,
 *                       so {@code false} is sent as no filter at all
 */
public record PostingPointQuery(Optional<Long> id, Optional<String> groupId, boolean onlyDefault) {

    private static final PostingPointQuery NONE =
            new PostingPointQuery(Optional.empty(), Optional.empty(), false);

    public PostingPointQuery {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(groupId, "groupId");
    }

    /** No filters — every posting point. */
    public static PostingPointQuery none() {
        return NONE;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link PostingPointQuery}. */
    public static final class Builder {

        private Long id;
        private String groupId;
        private boolean onlyDefault;

        private Builder() {
        }

        public Builder id(long postingPointId) {
            this.id = postingPointId;
            return this;
        }

        public Builder groupId(String value) {
            this.groupId = Objects.requireNonNull(value, "groupId");
            return this;
        }

        /** Restrict to the default posting point. */
        public Builder onlyDefault() {
            this.onlyDefault = true;
            return this;
        }

        public PostingPointQuery build() {
            return new PostingPointQuery(
                    Optional.ofNullable(id), Optional.ofNullable(groupId), onlyDefault);
        }
    }
}
