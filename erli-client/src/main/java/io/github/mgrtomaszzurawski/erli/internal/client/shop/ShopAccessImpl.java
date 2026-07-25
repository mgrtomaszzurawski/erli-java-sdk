package io.github.mgrtomaszzurawski.erli.internal.client.shop;

import io.github.mgrtomaszzurawski.erli.domain.shop.Shop;
import io.github.mgrtomaszzurawski.erli.domain.shop.ShopAccess;
import io.github.mgrtomaszzurawski.erli.internal.ApiPaths;
import io.github.mgrtomaszzurawski.erli.internal.HttpRuntime;
import io.github.mgrtomaszzurawski.erli.rest.model.ShopResponse;

import java.util.Objects;

/**
 * {@link ShopAccess} implementation: calls {@code GET /me} through the shared {@link HttpRuntime} and
 * maps the raw response to the {@link Shop} domain record. Internal: never exported.
 */
public final class ShopAccessImpl implements ShopAccess {

    private final HttpRuntime runtime;

    public ShopAccessImpl(HttpRuntime runtime) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
    }

    @Override
    public Shop me() {
        ShopResponse raw = runtime.get(ApiPaths.ME, ShopResponse.class);
        return ShopMapper.toDomain(raw);
    }
}
