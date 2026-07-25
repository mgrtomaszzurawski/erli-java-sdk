package io.github.mgrtomaszzurawski.erli.internal.client.dictionaries;

import io.github.mgrtomaszzurawski.erli.core.model.DeliveryMethodId;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.DeliveryMethod;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.DeliveryMethodQuery;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.DeliveryVendor;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.DictionariesAccess;
import io.github.mgrtomaszzurawski.erli.internal.ApiPaths;
import io.github.mgrtomaszzurawski.erli.internal.HttpRuntime;
import io.github.mgrtomaszzurawski.erli.internal.QueryParameters;

import java.util.List;
import java.util.Objects;

/**
 * {@link DictionariesAccess} implementation: turns a {@link DeliveryMethodQuery} into transport
 * {@link QueryParameters}, calls {@code GET /dictionaries/deliveryMethods} through the shared
 * {@link HttpRuntime}, and maps the raw array to domain records. Internal: never exported.
 */
public final class DictionariesAccessImpl implements DictionariesAccess {

    private static final String PARAM_ID = "id";
    private static final String PARAM_COD = "cod";
    private static final String PARAM_VENDOR = "vendor";

    private final HttpRuntime runtime;

    public DictionariesAccessImpl(HttpRuntime runtime) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
    }

    @Override
    public List<DeliveryMethod> deliveryMethods() {
        return deliveryMethods(DeliveryMethodQuery.none());
    }

    @Override
    public List<DeliveryMethod> deliveryMethods(DeliveryMethodQuery query) {
        Objects.requireNonNull(query, "query");
        DeliveryMethodId id = query.id();
        DeliveryVendor vendor = query.vendor();
        QueryParameters parameters = QueryParameters.builder()
                .add(PARAM_ID, id == null ? null : id.value())
                .addBoolean(PARAM_COD, query.cashOnDelivery())
                .add(PARAM_VENDOR, vendor == null ? null : vendor.wireValue())
                .build();
        return runtime.getList(ApiPaths.DICTIONARIES_DELIVERY_METHODS, parameters,
                        io.github.mgrtomaszzurawski.erli.rest.model.DeliveryMethod.class)
                .stream()
                .map(DeliveryMethodMapper::toDomain)
                .toList();
    }
}
