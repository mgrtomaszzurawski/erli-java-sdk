package io.github.mgrtomaszzurawski.erli.domain.delivery;

import java.util.List;

/**
 * Access to delivery price lists — the shop's catalogue of delivery methods and what each costs.
 * Reached via {@code client.delivery()}.
 *
 * <p>Public surface — consumers import only this package. Parcels and their carriage live in
 * {@code domain.shipping}; this area is purely about pricing.
 */
public interface DeliveryAccess {

    /**
     * List every price list, id and name only ({@code GET /delivery/priceLists}).
     *
     * @return the shop's price lists; empty if it has none
     */
    List<PriceListSummary> priceLists();

    /**
     * Fetch price lists with their full priced entries ({@code GET /delivery/priceListsDetails}).
     *
     * @param query filters to apply; {@link PriceListQuery#none()} for all lists
     * @return the matching price lists
     */
    List<PriceList> priceListDetails(PriceListQuery query);

    /**
     * Create a price list ({@code POST /delivery/priceList}).
     *
     * @param draft the list to create
     * @return the created list as the API stored it, including its assigned id
     * @throws io.github.mgrtomaszzurawski.erli.core.error.ErliValidationException if the name is
     *         already taken — names must be unique per shop
     */
    PriceList createPriceList(PriceListDraft draft);

    /**
     * Replace an existing price list's content ({@code PATCH /delivery/priceList/{id}}).
     *
     * @param priceListId the list to update
     * @param update      the complete replacement content
     * @return the updated list as the API stored it
     */
    PriceList updatePriceList(long priceListId, PriceListUpdate update);
}
