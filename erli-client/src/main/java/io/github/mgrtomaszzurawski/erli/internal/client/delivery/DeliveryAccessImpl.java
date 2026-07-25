package io.github.mgrtomaszzurawski.erli.internal.client.delivery;

import io.github.mgrtomaszzurawski.erli.domain.delivery.DeliveryAccess;
import io.github.mgrtomaszzurawski.erli.domain.delivery.PriceList;
import io.github.mgrtomaszzurawski.erli.domain.delivery.PriceListDraft;
import io.github.mgrtomaszzurawski.erli.domain.delivery.PriceListQuery;
import io.github.mgrtomaszzurawski.erli.domain.delivery.PriceListSummary;
import io.github.mgrtomaszzurawski.erli.domain.delivery.PriceListUpdate;
import io.github.mgrtomaszzurawski.erli.internal.ApiPaths;
import io.github.mgrtomaszzurawski.erli.internal.HttpRuntime;
import io.github.mgrtomaszzurawski.erli.internal.PathTemplate;
import io.github.mgrtomaszzurawski.erli.internal.QueryParameters;
import io.github.mgrtomaszzurawski.erli.rest.model.PriceListDetailsSchema;
import io.github.mgrtomaszzurawski.erli.rest.model.PriceListListItem;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * {@link DeliveryAccess} implementation: calls the {@code /delivery/*} endpoints through the shared
 * {@link HttpRuntime} and maps raw payloads to domain records. Internal: never exported.
 */
public final class DeliveryAccessImpl implements DeliveryAccess {

    private static final String PRICE_LIST_ID_PARAMETER = "id";
    private static final String PARAM_ID = "id";
    private static final String PARAM_NAME = "name";
    private static final String PARAM_ERLI_PRO_ENABLED = "erliProEnabled";

    private final HttpRuntime runtime;

    public DeliveryAccessImpl(HttpRuntime runtime) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
    }

    @Override
    public List<PriceListSummary> priceLists() {
        return runtime.getList(ApiPaths.DELIVERY_PRICE_LISTS, QueryParameters.empty(), PriceListListItem.class)
                .stream()
                .map(PriceListMapper::toSummary)
                .toList();
    }

    @Override
    public List<PriceList> priceListDetails(PriceListQuery query) {
        Objects.requireNonNull(query, "query");
        QueryParameters parameters = QueryParameters.builder()
                .addRepeated(PARAM_ID, query.ids().stream().map(String::valueOf).toList())
                .addRepeated(PARAM_NAME, query.names())
                .addBoolean(PARAM_ERLI_PRO_ENABLED, query.erliProEnabled().orElse(null))
                .build();
        return runtime.getList(ApiPaths.DELIVERY_PRICE_LISTS_DETAILS, parameters, PriceListDetailsSchema.class)
                .stream()
                .map(PriceListMapper::toDomain)
                .toList();
    }

    @Override
    public PriceList createPriceList(PriceListDraft draft) {
        Objects.requireNonNull(draft, "draft");
        return PriceListMapper.toDomain(runtime.post(ApiPaths.DELIVERY_PRICE_LIST,
                PriceListRequestMapper.toRaw(draft), PriceListDetailsSchema.class));
    }

    @Override
    public PriceList updatePriceList(long priceListId, PriceListUpdate update) {
        Objects.requireNonNull(update, "update");
        return PriceListMapper.toDomain(runtime.patch(byId(ApiPaths.DELIVERY_PRICE_LIST_BY_ID, priceListId),
                PriceListRequestMapper.toRaw(update), PriceListDetailsSchema.class));
    }

    /**
     * Expand a by-id template. Inlined at the call site rather than assigned to a local so the
     * transport verb and its {@code ApiPaths} constant stay on one source line — that pairing is what
     * the DoD coverage auditor reads to prove an operation is wired (see {@code tools/README.md}).
     */
    private static String byId(String pathTemplate, long priceListId) {
        return PathTemplate.expand(pathTemplate, Map.of(PRICE_LIST_ID_PARAMETER, String.valueOf(priceListId)));
    }
}
