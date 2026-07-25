package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

import java.util.List;

/**
 * Access to Erli's reference dictionaries (shared across shops). Reached via {@code client.dictionaries()}.
 * The Core-M1-follow-on starter slice exposes delivery methods; the rest of the dictionary surface
 * (categories, attributes, shipping methods, responsible persons/producers, attachments) is filled by
 * this bucket. Public surface — consumers import only this package.
 */
public interface DictionariesAccess {

    /** All delivery methods ({@code GET /dictionaries/deliveryMethods}). */
    List<DeliveryMethod> deliveryMethods();

    /**
     * Delivery methods matching the given filters.
     *
     * @param query optional filters (never null; use {@link DeliveryMethodQuery#none()} for all)
     * @return the matching delivery methods
     */
    List<DeliveryMethod> deliveryMethods(DeliveryMethodQuery query);
}
