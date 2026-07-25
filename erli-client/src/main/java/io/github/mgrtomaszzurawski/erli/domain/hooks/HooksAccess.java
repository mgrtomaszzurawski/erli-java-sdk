package io.github.mgrtomaszzurawski.erli.domain.hooks;

import java.util.List;

/**
 * Manage the shop's webhook subscriptions and test-fire them. Reached via {@code client.hooks()}.
 *
 * <p>A subscription tells Erli which shop endpoint to call for a {@link HookKind}. The two
 * {@code run} operations fire a hook on demand: <strong>Erli calls the shop's registered endpoint</strong>
 * and, for buyability, hands back what the shop answered. They exercise the shop's own integration —
 * they do not read Erli's data.
 *
 * <p>Public surface — consumers import only this package.
 */
public interface HooksAccess {

    /**
     * List the shop's registered subscriptions ({@code GET /hooks}).
     *
     * @return every registered subscription; empty when the shop has none
     */
    List<Hook> list();

    /**
     * Create or overwrite one subscription ({@code PUT /hooks/{hookName}}). The subscription replaced
     * is the one named by {@link Hook#kind()}.
     *
     * @param hook the subscription to store
     */
    void save(Hook hook);

    /**
     * Remove one subscription ({@code DELETE /hooks/{hookName}}). Erli stops calling the shop for
     * this kind of event.
     *
     * @param kind which subscription to remove
     */
    void delete(HookKind kind);

    /**
     * Test-fire the buyability hook ({@code POST /hooks/checkBuyability/run}): Erli asks the shop's
     * registered {@link HookKind#CHECK_BUYABILITY} endpoint about the given products and returns what
     * it answered.
     *
     * @param queries the products and quantities to ask about
     * @return the shop's answer per product
     */
    List<ProductBuyability> checkBuyability(List<BuyabilityQuery> queries);

    /**
     * Test-fire the product-sync hook ({@code POST /hooks/productsNeedSync/run}): Erli calls the
     * shop's registered {@link HookKind#PRODUCTS_NEED_SYNC} endpoint naming the given products. The
     * API answers {@code 204} — there is nothing to return.
     *
     * @param notification the products (and optionally fields) to name
     */
    void notifyProductsNeedSync(ProductSyncNotification notification);
}
