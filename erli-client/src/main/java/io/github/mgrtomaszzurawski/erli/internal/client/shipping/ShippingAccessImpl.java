package io.github.mgrtomaszzurawski.erli.internal.client.shipping;

import io.github.mgrtomaszzurawski.erli.core.model.ParcelId;
import io.github.mgrtomaszzurawski.erli.domain.shipping.Parcel;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ShippingAccess;
import io.github.mgrtomaszzurawski.erli.internal.ApiPaths;
import io.github.mgrtomaszzurawski.erli.internal.HttpRuntime;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * {@link ShippingAccess} implementation: calls the {@code /shipping/*} endpoints through the shared
 * {@link HttpRuntime} and maps raw responses to domain records. Internal: never exported.
 */
public final class ShippingAccessImpl implements ShippingAccess {

    /** Placeholder occupied by the parcel id in the templated path constants. */
    private static final String PARCEL_ID_PLACEHOLDER = "{id}";
    /**
     * {@link URLEncoder} targets form encoding, where a space becomes {@code +}. In a path segment a
     * space must be {@code %20}, so the one differing escape is corrected after encoding.
     */
    private static final String FORM_ENCODED_SPACE = "+";
    private static final String PATH_ENCODED_SPACE = "%20";
    /** Relative path segments; percent-encoding leaves both intact because {@code .} is unreserved. */
    private static final String DOT_SEGMENT = ".";
    private static final String DOUBLE_DOT_SEGMENT = "..";

    private final HttpRuntime runtime;

    public ShippingAccessImpl(HttpRuntime runtime) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
    }

    @Override
    public Parcel parcel(ParcelId parcelId) {
        Objects.requireNonNull(parcelId, "parcelId");
        return ParcelMapper.toDomain(runtime.get(withId(ApiPaths.SHIPPING_PARCEL_BY_ID, parcelId.value()),
                io.github.mgrtomaszzurawski.erli.rest.model.Parcel.class));
    }

    /**
     * Fill a templated path's {@code {id}} placeholder with an encoded segment.
     *
     * <p>Kept as a call-site expression rather than a local variable so the transport verb and its
     * {@code ApiPaths} constant stay on one line — that pairing is what the DoD coverage auditor reads
     * to prove an operation is genuinely wired (see {@code tools/README.md}).
     */
    private static String withId(String pathTemplate, String idValue) {
        if (!pathTemplate.contains(PARCEL_ID_PLACEHOLDER)) {
            throw new IllegalStateException(
                    "Path template '" + pathTemplate + "' has no " + PARCEL_ID_PLACEHOLDER + " placeholder");
        }
        return pathTemplate.replace(PARCEL_ID_PLACEHOLDER, encodeSegment(idValue));
    }

    /**
     * Percent-encode a value for use as a single path segment.
     *
     * <p>Percent-encoding alone is not enough. {@link URLEncoder} leaves {@code .} untouched because it
     * is an unreserved character, so an id of {@code "."} or {@code ".."} survives encoding and is then
     * collapsed as a relative path segment by the server's gateway — {@code /shipping/parcels/.} becomes
     * the collection endpoint. Harmless on a read, destructive once the by-id {@code DELETE} operations
     * of this bucket use the same helper, so dot segments are rejected before the request is built.
     */
    private static String encodeSegment(String value) {
        if (DOT_SEGMENT.equals(value) || DOUBLE_DOT_SEGMENT.equals(value)) {
            throw new IllegalArgumentException(
                    "'" + value + "' is a relative path segment and cannot address a resource");
        }
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace(FORM_ENCODED_SPACE, PATH_ENCODED_SPACE);
    }
}
