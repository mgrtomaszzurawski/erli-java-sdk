package io.github.mgrtomaszzurawski.erli.internal.client.shipping;

import io.github.mgrtomaszzurawski.erli.core.model.ParcelId;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ExternalParcel;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ExternalParcelDraft;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ExternalParcelResult;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ExternalParcelUpdate;
import io.github.mgrtomaszzurawski.erli.domain.shipping.Parcel;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ParcelDraft;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ParcelFilter;
import io.github.mgrtomaszzurawski.erli.domain.shipping.PickupProtocols;
import io.github.mgrtomaszzurawski.erli.domain.shipping.PostingPoint;
import io.github.mgrtomaszzurawski.erli.domain.shipping.PostingPointQuery;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ShippingAccess;
import io.github.mgrtomaszzurawski.erli.internal.ApiPaths;
import io.github.mgrtomaszzurawski.erli.internal.HttpRuntime;
import io.github.mgrtomaszzurawski.erli.internal.JsonCodec;
import io.github.mgrtomaszzurawski.erli.internal.PathTemplate;
import io.github.mgrtomaszzurawski.erli.internal.QueryParameters;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * {@link ShippingAccess} implementation: calls the {@code /shipping/*} endpoints through the shared
 * {@link HttpRuntime} and maps raw payloads to domain records. Internal: never exported.
 *
 * <p>By-id paths are expanded with the core {@link PathTemplate}, which percent-encodes each segment
 * and rejects {@code .}/{@code ..} — an id must not be able to re-route the request to another
 * resource, and on the by-id {@code DELETE}s below that would be destructive.
 */
public final class ShippingAccessImpl implements ShippingAccess {

    private static final String PARCEL_ID_PARAMETER = "id";
    private static final String PARAM_PARCEL_IDS = "parcelIds";
    private static final String PARAM_ID = "id";
    private static final String PARAM_GROUP_ID = "groupId";
    private static final String PARAM_IS_DEFAULT = "isDefault";
    /** The API accepts only {@code true} for the default-posting-point filter. */
    private static final String FILTER_DEFAULT_ONLY = "true";

    private final HttpRuntime runtime;
    private final JsonCodec codec;

    public ShippingAccessImpl(HttpRuntime runtime, JsonCodec codec) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.codec = Objects.requireNonNull(codec, "codec");
    }

    @Override
    public Parcel parcel(ParcelId parcelId) {
        Objects.requireNonNull(parcelId, "parcelId");
        return ParcelMapper.toDomain(runtime.get(byParcelId(ApiPaths.SHIPPING_PARCEL_BY_ID, parcelId),
                io.github.mgrtomaszzurawski.erli.rest.model.Parcel.class));
    }

    @Override
    public List<Parcel> createParcels(List<ParcelDraft> drafts) {
        requireNotEmpty(drafts, "drafts");
        return runtime.postList(ApiPaths.SHIPPING_PARCELS, ShippingRequestMapper.toRawParcels(drafts),
                        io.github.mgrtomaszzurawski.erli.rest.model.Parcel.class)
                .stream()
                .map(ParcelMapper::toDomain)
                .toList();
    }

    @Override
    public List<Parcel> searchParcels(ParcelFilter filter) {
        Objects.requireNonNull(filter, "filter");
        return runtime.postList(ApiPaths.SHIPPING_PARCELS_SEARCH, ShippingRequestMapper.toRawSearch(filter),
                        io.github.mgrtomaszzurawski.erli.rest.model.Parcel.class)
                .stream()
                .map(ParcelMapper::toDomain)
                .toList();
    }

    @Override
    public Parcel cancelParcel(ParcelId parcelId) {
        Objects.requireNonNull(parcelId, "parcelId");
        return ParcelMapper.toDomain(runtime.delete(byParcelId(ApiPaths.SHIPPING_PARCEL_BY_ID, parcelId),
                QueryParameters.empty(), io.github.mgrtomaszzurawski.erli.rest.model.Parcel.class));
    }

    @Override
    public List<ExternalParcelResult> registerExternalParcels(List<ExternalParcelDraft> drafts) {
        requireNotEmpty(drafts, "drafts");
        return runtime.postList(ApiPaths.SHIPPING_EXTERNAL,
                        ShippingRequestMapper.toRawExternalParcels(drafts), JsonNode.class)
                .stream()
                .map(rawEntry -> ExternalParcelMapper.toResult(rawEntry, codec))
                .toList();
    }

    @Override
    public ExternalParcel externalParcel(ParcelId parcelId) {
        Objects.requireNonNull(parcelId, "parcelId");
        return ExternalParcelMapper.toDomain(runtime.get(byParcelId(ApiPaths.SHIPPING_EXTERNAL_BY_ID, parcelId),
                io.github.mgrtomaszzurawski.erli.rest.model.ExternalParcel.class));
    }

    @Override
    public ExternalParcel updateExternalParcel(ParcelId parcelId, ExternalParcelUpdate update) {
        Objects.requireNonNull(parcelId, "parcelId");
        Objects.requireNonNull(update, "update");
        return ExternalParcelMapper.toDomain(runtime.patch(byParcelId(ApiPaths.SHIPPING_EXTERNAL_BY_ID, parcelId),
                ShippingRequestMapper.toRaw(update),
                io.github.mgrtomaszzurawski.erli.rest.model.ExternalParcel.class));
    }

    @Override
    public void deleteExternalParcel(ParcelId parcelId) {
        Objects.requireNonNull(parcelId, "parcelId");
        // Answered with 204 and no body; Void.class tells the runtime to expect nothing back.
        runtime.delete(byParcelId(ApiPaths.SHIPPING_EXTERNAL_BY_ID, parcelId), QueryParameters.empty(), Void.class);
    }

    @Override
    public PickupProtocols pickupProtocols(List<ParcelId> parcelIds) {
        requireNotEmpty(parcelIds, "parcelIds");
        QueryParameters parameters = QueryParameters.builder()
                .addCsv(PARAM_PARCEL_IDS, parcelIds.stream().map(ParcelId::value).toList())
                .build();
        return PickupProtocolsMapper.toDomain(runtime.get(ApiPaths.SHIPPING_PICKUP_PROTOCOLS, parameters,
                io.github.mgrtomaszzurawski.erli.rest.model.PickupProtocols.class));
    }

    @Override
    public List<PostingPoint> postingPoints(PostingPointQuery query) {
        Objects.requireNonNull(query, "query");
        QueryParameters parameters = QueryParameters.builder()
                .add(PARAM_ID, query.id().map(String::valueOf).orElse(null))
                .add(PARAM_GROUP_ID, query.groupId().orElse(null))
                .add(PARAM_IS_DEFAULT, query.onlyDefault() ? FILTER_DEFAULT_ONLY : null)
                .build();
        return runtime.getList(ApiPaths.SHIPPING_POSTING_POINTS, parameters,
                        io.github.mgrtomaszzurawski.erli.rest.model.PostingPoint.class)
                .stream()
                .map(PostingPointMapper::toDomain)
                .toList();
    }

    /**
     * Expand a by-id template. Inlined at each call site rather than assigned to a local so the
     * transport verb and its {@code ApiPaths} constant stay on one source line — that pairing is what
     * the DoD coverage auditor reads to prove an operation is wired (see {@code tools/README.md}).
     */
    private static String byParcelId(String pathTemplate, ParcelId parcelId) {
        return PathTemplate.expand(pathTemplate, Map.of(PARCEL_ID_PARAMETER, parcelId.value()));
    }

    private static void requireNotEmpty(List<?> values, String name) {
        Objects.requireNonNull(values, name);
        if (values.isEmpty()) {
            throw new IllegalArgumentException("'" + name + "' must not be empty");
        }
    }
}
