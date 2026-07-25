package io.github.mgrtomaszzurawski.erli.internal.client.hooks;

import io.github.mgrtomaszzurawski.erli.core.error.ErliTransportException;
import io.github.mgrtomaszzurawski.erli.domain.hooks.BuyabilityQuery;
import io.github.mgrtomaszzurawski.erli.domain.hooks.Hook;
import io.github.mgrtomaszzurawski.erli.domain.hooks.HookKind;
import io.github.mgrtomaszzurawski.erli.domain.hooks.HooksAccess;
import io.github.mgrtomaszzurawski.erli.domain.hooks.ProductBuyability;
import io.github.mgrtomaszzurawski.erli.domain.hooks.ProductSyncNotification;
import io.github.mgrtomaszzurawski.erli.internal.ApiPaths;
import io.github.mgrtomaszzurawski.erli.internal.HttpRuntime;
import io.github.mgrtomaszzurawski.erli.internal.PathTemplate;
import io.github.mgrtomaszzurawski.erli.internal.QueryParameters;
import io.github.mgrtomaszzurawski.erli.rest.model.CheckBuyabilityResponseInner;
import io.github.mgrtomaszzurawski.erli.rest.model.HookResponseInner;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * {@link HooksAccess} implementation over the shared {@link HttpRuntime}. Every response array is
 * decoded into the generated Layer-1 element type and mapped by {@link HookMapper}; nothing raw
 * escapes. Internal: never exported.
 */
public final class HooksAccessImpl implements HooksAccess {

    private final HttpRuntime runtime;

    public HooksAccessImpl(HttpRuntime runtime) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
    }

    @Override
    public List<Hook> list() {
        List<HookResponseInner> rawHooks =
                runtime.getList(ApiPaths.HOOKS, QueryParameters.empty(), HookResponseInner.class);
        // A stored subscription the SDK cannot represent must still surface as an ErliException: the
        // documented contract is that catching ErliException covers every failure of a call. Since
        // CORE-12 this is reachable from wire data — an unrecognised hookName decodes to null — so the
        // mapper's IllegalStateException would otherwise escape bare.
        try {
            return mapAll(rawHooks, HookMapper::toDomain);
        } catch (IllegalStateException | IllegalArgumentException failure) {
            throw new ErliTransportException(
                    "The shop has a webhook subscription this SDK version cannot represent", failure);
        }
    }

    @Override
    public void save(Hook hook) {
        Objects.requireNonNull(hook, "hook").requireRegisterable();
        runtime.put(PathTemplate.expand(ApiPaths.HOOK_BY_NAME, hookNamePathParams(hook.kind())),
                HookMapper.toRaw(hook), Void.class);
    }

    @Override
    public void delete(HookKind kind) {
        Objects.requireNonNull(kind, "kind");
        runtime.delete(PathTemplate.expand(ApiPaths.HOOK_BY_NAME, hookNamePathParams(kind)),
                QueryParameters.empty(), Void.class);
    }

    @Override
    public List<ProductBuyability> checkBuyability(List<BuyabilityQuery> queries) {
        Objects.requireNonNull(queries, "queries");
        List<CheckBuyabilityResponseInner> rawBuyability = runtime.postList(ApiPaths.HOOK_CHECK_BUYABILITY_RUN,
                HookMapper.toRaw(queries), CheckBuyabilityResponseInner.class);
        return mapAll(rawBuyability, HookMapper::toDomain);
    }

    @Override
    public void notifyProductsNeedSync(ProductSyncNotification notification) {
        Objects.requireNonNull(notification, "notification");
        runtime.post(ApiPaths.HOOK_PRODUCTS_NEED_SYNC_RUN, HookMapper.toRaw(notification), Void.class);
    }

    private static Map<String, String> hookNamePathParams(HookKind kind) {
        return Map.of(ApiPaths.HOOK_NAME_PARAM, kind.wireValue());
    }

    /** An absent (rather than empty) JSON array is treated as no results. */
    private static <R, D> List<D> mapAll(List<R> rawElements, Function<R, D> mapper) {
        if (rawElements == null) {
            return List.of();
        }
        return rawElements.stream().map(mapper).toList();
    }
}
