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

    private final HttpRuntime runtime;

    public ShippingAccessImpl(HttpRuntime runtime) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
    }

    @Override
    public Parcel parcel(ParcelId parcelId) {
        Objects.requireNonNull(parcelId, "parcelId");
        String path = ApiPaths.SHIPPING_PARCEL_BY_ID.replace(PARCEL_ID_PLACEHOLDER, encodeSegment(parcelId.value()));
        io.github.mgrtomaszzurawski.erli.rest.model.Parcel raw =
                runtime.get(path, io.github.mgrtomaszzurawski.erli.rest.model.Parcel.class);
        return ParcelMapper.toDomain(raw);
    }

    /** Percent-encode a value for use as a single path segment. */
    private static String encodeSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace(FORM_ENCODED_SPACE, PATH_ENCODED_SPACE);
    }
}
