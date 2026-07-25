package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

import io.github.mgrtomaszzurawski.erli.core.model.CategoryId;

import java.util.List;
import java.util.stream.Stream;

/**
 * Read and write access to the marketplace reference dictionaries under {@code /dictionaries}. Reach
 * it through {@code client.dictionaries()}.
 *
 * <p>Dictionary contents are marketplace-wide: with the exception of the per-price-list delivery
 * methods and the two responsible-party dictionaries (which are per shop), results do not depend on
 * the shop the API key belongs to. Because these endpoints return data on an otherwise empty shop,
 * this is the domain to reach for when you need something real to test against.
 *
 * <p>Values that the marketplace owns and may extend — carriers, operators, countries, integration
 * sources — are modelled as value types rather than Java enums, so a new entry never breaks a
 * compiled consumer. Genuinely closed, structural taxonomies such as {@link AttributeType} are enums.
 */
public interface DictionaryAccess {

    /**
     * Every category in the marketplace tree, streamed lazily
     * ({@code POST /dictionaries/category/_search}).
     *
     * <p>The API pages this dictionary with an {@code after}/{@code limit} cursor over the category
     * id; the returned stream walks it on demand, so a short-circuiting terminal operation such as
     * {@code findFirst()} fetches only the pages it needs.
     *
     * @return a lazy stream of categories, ordered by id ascending
     */
    Stream<Category> categories();

    /**
     * The attributes defined for a category ({@code POST /dictionaries/attributes/_search}).
     *
     * <p>Attributes are defined on leaf categories; a non-leaf category yields an empty list.
     *
     * @param categoryId the category whose attributes to read
     * @return the attributes, ordered by id ascending; never {@code null}
     */
    List<Attribute> attributes(CategoryId categoryId);

    /**
     * The allowed values of the dictionary attributes of a category
     * ({@code POST /dictionaries/attributeValues/_search}).
     *
     * @param categoryId the category whose attribute values to read
     * @return one entry per {@link AttributeType#DICTIONARY} attribute; never {@code null}
     */
    List<AttributeValues> attributeValues(CategoryId categoryId);

    /**
     * All delivery methods that may appear as {@code Order.delivery}
     * ({@code GET /dictionaries/deliveryMethods}).
     *
     * @return the delivery methods, in the order the API returned them; never {@code null}
     */
    List<DeliveryMethod> deliveryMethods();

    /**
     * Delivery methods matching a filter ({@code GET /dictionaries/deliveryMethods}).
     *
     * @param filter the server-side filter; use {@link DeliveryMethodFilter#all()} for no filtering
     * @return the matching delivery methods; never {@code null}
     */
    List<DeliveryMethod> deliveryMethods(DeliveryMethodFilter filter);

    /**
     * Delivery methods available for one of the shop's price lists
     * ({@code GET /dictionaries/deliveryMethods/{priceList}}).
     *
     * @param priceList the price list to scope the dictionary to
     * @param filter the server-side filter; use {@link DeliveryMethodFilter#all()} for no filtering
     * @return the matching delivery methods; never {@code null}
     */
    List<DeliveryMethod> deliveryMethods(PriceListName priceList, DeliveryMethodFilter filter);

    /**
     * The carriers ERLI accepts tracking numbers for ({@code GET /dictionaries/deliveryVendors}).
     *
     * <p>This is the <em>tracking</em> carrier list and is deliberately narrower than the set of
     * carriers appearing as {@link DeliveryMethod#vendor()} — do not treat it as a closed domain for
     * delivery methods.
     *
     * @return the carriers; never {@code null}
     */
    List<DeliveryVendor> deliveryVendors();

    /**
     * ERLI's own shipping methods ({@code GET /dictionaries/shippingMethods}).
     *
     * @param filter the server-side filter; use {@link ShippingMethodFilter#all()} for no filtering
     * @return the matching shipping methods; never {@code null}
     */
    List<ShippingMethod> shippingMethods(ShippingMethodFilter filter);

    /**
     * The billing operation types used by the Finance domain
     * ({@code GET /dictionaries/billingEntryTypes}).
     *
     * @return the billing entry types; never {@code null}
     */
    List<BillingEntryType> billingEntryTypes();

    /**
     * The shop's responsible persons — those who introduced a product to the market
     * ({@code GET /dictionaries/responsiblePersons}).
     *
     * @param filter the server-side filter; use {@link ResponsiblePartyFilter#all()} for no filtering
     * @return the matching entries; never {@code null}
     */
    List<ResponsibleParty> responsiblePersons(ResponsiblePartyFilter filter);

    /**
     * Create a responsible person ({@code POST /dictionaries/responsiblePersons}).
     *
     * @param party the entry to create; its {@code idempotenceKey} must be unused in this shop
     * @return the shop's responsible persons as the API returned them after the create
     * @throws io.github.mgrtomaszzurawski.erli.core.error.ErliValidationException if the
     *         {@code idempotenceKey} is already taken (HTTP 409) or a field is rejected
     */
    List<ResponsibleParty> createResponsiblePerson(NewResponsibleParty party);

    /**
     * The shop's product producers ({@code GET /dictionaries/responsibleProducers}).
     *
     * @param filter the server-side filter; use {@link ResponsiblePartyFilter#all()} for no filtering
     * @return the matching entries; never {@code null}
     */
    List<ResponsibleParty> responsibleProducers(ResponsiblePartyFilter filter);

    /**
     * Create a product producer ({@code POST /dictionaries/responsibleProducers}).
     *
     * @param party the entry to create; its {@code idempotenceKey} must be unused in this shop
     * @return the shop's producers as the API returned them after the create
     * @throws io.github.mgrtomaszzurawski.erli.core.error.ErliValidationException if the
     *         {@code idempotenceKey} is already taken (HTTP 409) or a field is rejected
     */
    List<ResponsibleParty> createResponsibleProducer(NewResponsibleParty party);
}
