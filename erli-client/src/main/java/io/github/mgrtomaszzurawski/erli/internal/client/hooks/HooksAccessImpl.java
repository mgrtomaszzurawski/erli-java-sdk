package io.github.mgrtomaszzurawski.erli.internal.client.hooks;

import io.github.mgrtomaszzurawski.erli.domain.hooks.BuyabilityQuery;
import io.github.mgrtomaszzurawski.erli.domain.hooks.Hook;
import io.github.mgrtomaszzurawski.erli.domain.hooks.HookKind;
import io.github.mgrtomaszzurawski.erli.domain.hooks.HooksAccess;
import io.github.mgrtomaszzurawski.erli.domain.hooks.ProductBuyability;
import io.github.mgrtomaszzurawski.erli.domain.hooks.ProductSyncNotification;
import io.github.mgrtomaszzurawski.erli.internal.ApiPaths;
import io.github.mgrtomaszzurawski.erli.internal.HttpRuntime;
import io.github.mgrtomaszzurawski.erli.internal.QueryParameters;
import io.github.mgrtomaszzurawski.erli.rest.model.CheckBuyabilityResponseInner;
import io.github.mgrtomaszzurawski.erli.rest.model.HookResponseInner;

import java.util.List;
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
        return mapAll(rawHooks, HookMapper::toDomain);
    }

    @Override
    public void save(Hook hook) {
        Objects.requireNonNull(hook, "hook").requireRegisterable();
        runtime.put(ApiPaths.HOOK_BY_NAME.replace(ApiPaths.HOOK_NAME_PLACEHOLDER, hook.kind().wireValue()),
                HookMapper.toRaw(hook), Void.class);
    }

    @Override
    public void delete(HookKind kind) {
        Objects.requireNonNull(kind, "kind");
        runtime.delete(ApiPaths.HOOK_BY_NAME.replace(ApiPaths.HOOK_NAME_PLACEHOLDER, kind.wireValue()),
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

    /** An absent (rather than empty) JSON array is treated as no results. */
    private static <R, D> List<D> mapAll(List<R> rawElements, Function<R, D> mapper) {
        if (rawElements == null) {
            return List.of();
        }
        return rawElements.stream().map(mapper).toList();
    }
}
