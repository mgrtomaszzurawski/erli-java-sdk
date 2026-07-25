package io.github.mgrtomaszzurawski.erli.domain.shop;

/**
 * Access to the authenticated shop. Reached via {@code client.shop()}; the sole operation for M1 is
 * {@link #me()}, the {@code GET /me} proof slice. Public surface — consumers import only this package.
 */
public interface ShopAccess {

    /**
     * Fetch the shop that owns the configured API key ({@code GET /me}).
     *
     * @return the authenticated shop
     */
    Shop me();
}
