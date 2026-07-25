package io.github.mgrtomaszzurawski.erli.internal.client.shipping;

import io.github.mgrtomaszzurawski.erli.core.model.ShippingMethodId;
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
 * <p><strong>Known limitation until CORE-3 lands.</strong> The generated {@code anyOf} deserializer
 * accepts the first branch that parses, and with unknown properties ignored the base branch always
 * parses — so a {@code point}/{@code points} payload can bind to the base shape and arrive without its
 * point detail. Bucket B's {@code normalizeSpec} composite-merge makes Layer 1 lossless and this
 * dispatch collapses to a single bind; see CORE-3 in {@code BACKLOG.md}.
 */
final class PostingPointMapper {

    private PostingPointMapper() {
    }

    static PostingPoint toDomain(io.github.mgrtomaszzurawski.erli.rest.model.PostingPoint rawPoint) {
        Objects.requireNonNull(rawPoint, "raw PostingPoint");
        Object branch = rawPoint.getActualInstance();
        if (branch instanceof PostingPointAnyOf1 singlePoint) {
            return fromSinglePoint(singlePoint);
        }
        if (branch instanceof PostingPointAnyOf2 severalPoints) {
            return fromSeveralPoints(severalPoints);
        }
        if (branch instanceof PostingPointAnyOf address) {
            return fromAddress(address);
        }
        throw new IllegalStateException("Posting point decoded to an unexpected branch: "
                + (branch == null ? "null" : branch.getClass().getName()));
    }

    private static PostingPoint fromAddress(PostingPointAnyOf raw) {
        return new PostingPoint(
                requireField(raw.getId(), "id").longValue(),
                requireField(raw.getName(), "name"),
                PostingPointType.fromWire(requireField(raw.getType(), "type").getValue()),
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
                PostingPointType.fromWire(requireField(raw.getType(), "type").getValue()),
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
                PostingPointType.fromWire(requireField(raw.getType(), "type").getValue()),
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

    private static <T> T requireField(T value, String fieldName) {
        if (value == null) {
            throw new IllegalStateException("Posting point is missing the required '" + fieldName + "' field");
        }
        return value;
    }
}
