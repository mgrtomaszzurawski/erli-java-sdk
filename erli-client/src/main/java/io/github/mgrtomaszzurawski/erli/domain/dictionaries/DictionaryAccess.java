package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

import java.util.List;

/**
 * Read access to the marketplace reference dictionaries under {@code /dictionaries}. Reach it through
 * {@code client.dictionaries()}.
 *
 * <p>Dictionary contents are marketplace-wide: with the documented exception of the per-price-list
 * delivery methods, results do not depend on the shop the API key belongs to. Values are returned as
 * value types rather than Java enums so that a marketplace adding an entry does not break consumers.
 */
public interface DictionaryAccess {

    /**
     * All delivery methods that may appear as {@code Order.delivery}
     * ({@code GET /dictionaries/deliveryMethods}).
     *
     * @return the delivery methods, in the order the API returned them; never {@code null}
     */
    List<DeliveryMethod> deliveryMethods();
}
