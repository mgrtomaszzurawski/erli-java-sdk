package io.github.mgrtomaszzurawski.erli.internal.client.shipping;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.mgrtomaszzurawski.erli.core.model.ShippingMethodId;
import io.github.mgrtomaszzurawski.erli.internal.JsonCodec;
import io.github.mgrtomaszzurawski.erli.domain.shipping.CarrierPoint;
import io.github.mgrtomaszzurawski.erli.domain.shipping.GeoLocation;
import io.github.mgrtomaszzurawski.erli.domain.shipping.PointAddress;
import io.github.mgrtomaszzurawski.erli.domain.shipping.PostingPoint;
import io.github.mgrtomaszzurawski.erli.domain.shipping.PostingPointType;
import io.github.mgrtomaszzurawski.erli.rest.model.PostingPointAnyOf;
import io.github.mgrtomaszzurawski.erli.rest.model.PostingPointAnyOf1;
import io.github.mgrtomaszzurawski.erli.rest.model.PostingPointAnyOf1Location;
import io.github.mgrtomaszzurawski.erli.rest.model.PostingPointAnyOf1PointAddress;
import io.github.mgrtomaszzurawski.erli.rest.model.PostingPointAnyOf2;
import io.github.mgrtomaszzurawski.erli.rest.model.PostingPointAnyOf2PointsInner;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Maps the generated posting-point payload onto its domain record. Internal.
 *
 * <p>The API states {@code PostingPoint} as an {@code anyOf} over three shapes that share a base and
 * differ only in how the drop-off is described — a courier address, one carrier point, or several. The
 * generator emits one class per shape with no common supertype, so this mapper dispatches on the branch
 * the payload bound to and fills the branch-specific fields from it.
 *
 * <p><strong>Why the discriminator is read off the raw tree — do not "simplify" this away.</strong>
 * The generated {@code anyOf} deserializer is first-match-wins, and the shared codec both ignores
 * unknown properties and decodes an unknown enum value as {@code null}, so the base {@code address}
 * branch parses a {@code point} payload too and silently drops the point detail that is the whole
 * reason the branch exists.
 *
 * <p>The {@code normalizeSpec} composite-merge (CORE-3) deliberately leaves this schema alone: it
 * refuses composites whose branches define a shared property differently, and all three define
 * {@code type} with a different single-valued enum. That is exactly right — {@code type} <em>is</em>
 * the discriminator, and merging would destroy it. So this reads {@code type} and binds the node to the
 * matching generated class with the shared codec, keeping every field coming from Layer 1. Verified
 * against the vendored spec on 2026-07-25.
 */
final class PostingPointMapper {

    private PostingPointMapper() {
    }

    /** The property Erli discriminates the three posting-point shapes on. */
    private static final String TYPE_FIELD = "type";

    static PostingPoint toDomain(JsonNode rawPoint, JsonCodec codec) {
        Objects.requireNonNull(rawPoint, "raw PostingPoint");
        Objects.requireNonNull(codec, "codec");
        JsonNode type = rawPoint.get(TYPE_FIELD);
        if (type == null || type.isNull()) {
            throw new IllegalStateException("Posting point is missing the required 'type' field");
        }
        PostingPointType postingPointType = PostingPointType.fromWire(type.asText());
        switch (postingPointType) {
            case POINT:
                return fromSinglePoint(codec.convert(rawPoint, PostingPointAnyOf1.class));
            case POINTS:
                return fromSeveralPoints(codec.convert(rawPoint, PostingPointAnyOf2.class));
            case ADDRESS:
            default:
                return fromAddress(codec.convert(rawPoint, PostingPointAnyOf.class));
        }
    }

    private static PostingPoint fromAddress(PostingPointAnyOf raw) {
        return new PostingPoint(
                requireField(raw.getId(), "id").longValue(),
                requireField(raw.getName(), "name"),
                postingPointTypeOf(raw.getType()),
                Boolean.TRUE.equals(raw.getIsDefault()),
                Optional.ofNullable(raw.getCompanyName()),
                Optional.ofNullable(raw.getPhone()),
                Optional.ofNullable(raw.getEmail()),
                Optional.ofNullable(raw.getStreet()),
                Optional.ofNullable(raw.getBuildingNumber()),
                Optional.ofNullable(raw.getFlatNumber()),
                Optional.ofNullable(raw.getZip()),
                Optional.ofNullable(raw.getCity()),
                Optional.empty(),
                List.of());
    }

    private static PostingPoint fromSinglePoint(PostingPointAnyOf1 raw) {
        CarrierPoint point = new CarrierPoint(
                Optional.ofNullable(raw.getPointCode()),
                Optional.ofNullable(raw.getPointAddress()).map(PostingPointMapper::toPointAddress),
                Optional.ofNullable(raw.getLocation()).map(PostingPointMapper::toLocation),
                Optional.empty());
        return new PostingPoint(
                requireField(raw.getId(), "id").longValue(),
                requireField(raw.getName(), "name"),
                postingPointTypeOf(raw.getType()),
                Boolean.TRUE.equals(raw.getIsDefault()),
                Optional.ofNullable(raw.getCompanyName()),
                Optional.ofNullable(raw.getPhone()),
                Optional.ofNullable(raw.getEmail()),
                Optional.ofNullable(raw.getStreet()),
                Optional.ofNullable(raw.getBuildingNumber()),
                Optional.ofNullable(raw.getFlatNumber()),
                Optional.ofNullable(raw.getZip()),
                Optional.ofNullable(raw.getCity()),
                Optional.of(point),
                List.of());
    }

    private static PostingPoint fromSeveralPoints(PostingPointAnyOf2 raw) {
        return new PostingPoint(
                requireField(raw.getId(), "id").longValue(),
                requireField(raw.getName(), "name"),
                postingPointTypeOf(raw.getType()),
                Boolean.TRUE.equals(raw.getIsDefault()),
                Optional.ofNullable(raw.getCompanyName()),
                Optional.ofNullable(raw.getPhone()),
                Optional.ofNullable(raw.getEmail()),
                Optional.ofNullable(raw.getStreet()),
                Optional.ofNullable(raw.getBuildingNumber()),
                Optional.ofNullable(raw.getFlatNumber()),
                Optional.ofNullable(raw.getZip()),
                Optional.ofNullable(raw.getCity()),
                Optional.empty(),
                toPoints(raw.getPoints()));
    }

    private static List<CarrierPoint> toPoints(List<PostingPointAnyOf2PointsInner> rawPoints) {
        if (rawPoints == null) {
            return List.of();
        }
        List<CarrierPoint> points = new ArrayList<>(rawPoints.size());
        for (PostingPointAnyOf2PointsInner rawPoint : rawPoints) {
            if (rawPoint == null) {
                throw new IllegalStateException("Posting point field 'points' has a null element");
            }
            points.add(new CarrierPoint(
                    Optional.ofNullable(rawPoint.getPointCode()),
                    Optional.ofNullable(rawPoint.getPointAddress()).map(PostingPointMapper::toPointAddress),
                    Optional.ofNullable(rawPoint.getLocation()).map(PostingPointMapper::toLocation),
                    Optional.ofNullable(rawPoint.getTypeId())
                            .map(PostingPointAnyOf2PointsInner.TypeIdEnum::getValue)
                            .map(ShippingMethodId::of)));
        }
        return List.copyOf(points);
    }

    private static PointAddress toPointAddress(PostingPointAnyOf1PointAddress raw) {
        return new PointAddress(
                Optional.ofNullable(raw.getStreet()),
                Optional.ofNullable(raw.getBuildingNumber()),
                Optional.ofNullable(raw.getFlatNumber()),
                Optional.ofNullable(raw.getZip()),
                Optional.ofNullable(raw.getCity()));
    }

    private static GeoLocation toLocation(PostingPointAnyOf1Location raw) {
        return new GeoLocation(
                requireField(raw.getLatitude(), "location.latitude"),
                requireField(raw.getLongitude(), "location.longitude"));
    }

    /** The branch enums are single-valued, so the wire value is the branch's own constant. */
    private static PostingPointType postingPointTypeOf(Object branchType) {
        return PostingPointType.fromWire(String.valueOf(branchType));
    }

    private static <T> T requireField(T value, String fieldName) {
        if (value == null) {
            throw new IllegalStateException("Posting point is missing the required '" + fieldName + "' field");
        }
        return value;
    }
}
