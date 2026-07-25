package io.github.mgrtomaszzurawski.erli.internal.client.dictionaries;

import io.github.mgrtomaszzurawski.erli.domain.dictionaries.DeliveryMethod;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.DictionaryAccess;
import io.github.mgrtomaszzurawski.erli.internal.ApiPaths;
import io.github.mgrtomaszzurawski.erli.internal.HttpRuntime;

import java.util.List;
import java.util.Objects;

/**
 * {@link DictionaryAccess} implementation: calls the {@code /dictionaries} endpoints through the
 * shared {@link HttpRuntime} and maps the raw responses to domain records. Internal: never exported.
 */
public final class DictionaryAccessImpl implements DictionaryAccess {

    private final HttpRuntime runtime;

    public DictionaryAccessImpl(HttpRuntime runtime) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
    }

    @Override
    public List<DeliveryMethod> deliveryMethods() {
        io.github.mgrtomaszzurawski.erli.rest.model.DeliveryMethod[] raw =
                runtime.get(ApiPaths.DICTIONARIES_DELIVERY_METHODS,
                        io.github.mgrtomaszzurawski.erli.rest.model.DeliveryMethod[].class);
        return DeliveryMethodMapper.toDomainList(raw);
    }
}
