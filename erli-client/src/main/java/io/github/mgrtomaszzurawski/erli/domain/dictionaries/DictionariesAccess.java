package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

import io.github.mgrtomaszzurawski.erli.core.model.CategoryId;
import io.github.mgrtomaszzurawski.erli.core.model.DeliveryVendor;

import java.util.List;
import java.util.stream.Stream;

/**
 * Access to Erli's reference dictionaries ({@code /dictionaries/*}). Reached via
 * {@code client.dictionaries()}.
 *
 * <p>Most of this surface is marketplace-wide and identical for every shop — categories, attributes,
 * delivery and shipping methods, carriers, billing entry types. The rest is per shop: the
 * per-price-list delivery methods, the two responsible-party dictionaries and the attachments.
 * Because the shared dictionaries return data even for a brand-new shop, this is the domain to reach
 * for when you need something real to test against.
 *
 * <p>Public surface — consumers import only this package.
 */
public interface DictionariesAccess {

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

    /** All delivery methods ({@code GET /dictionaries/deliveryMethods}). */
    List<DeliveryMethod> deliveryMethods();

    /**
     * Delivery methods matching the given filters.
     *
     * @param query optional filters (never null; use {@link DeliveryMethodQuery#none()} for all)
     * @return the matching delivery methods
     */
    List<DeliveryMethod> deliveryMethods(DeliveryMethodQuery query);

    /**
     * Delivery methods available for one of the shop's price lists
     * ({@code GET /dictionaries/deliveryMethods/{priceList}}).
     *
     * @param priceList the price list to scope the dictionary to
     * @param query optional filters (never null; use {@link DeliveryMethodQuery#none()} for all)
     * @return the matching delivery methods; never {@code null}
     */
    List<DeliveryMethod> deliveryMethods(PriceListName priceList, DeliveryMethodQuery query);

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
     * @param query optional filters (never null; use {@link ShippingMethodQuery#none()} for all)
     * @return the matching shipping methods; never {@code null}
     */
    List<ShippingMethod> shippingMethods(ShippingMethodQuery query);

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
     * @param query optional filters (never null; use {@link ResponsiblePartyQuery#none()} for all)
     * @return the matching entries; never {@code null}
     */
    List<ResponsibleParty> responsiblePersons(ResponsiblePartyQuery query);

    /**
     * Create a responsible person ({@code POST /dictionaries/responsiblePersons}).
     *
     * @param party the entry to create; its {@code idempotenceKey} must be unused in this shop
     * @return the created entry
     * @throws io.github.mgrtomaszzurawski.erli.core.error.ErliValidationException if the
     *         {@code idempotenceKey} is already taken (HTTP 409) or a field is rejected
     */
    ResponsibleParty createResponsiblePerson(NewResponsibleParty party);

    /**
     * Update a responsible person ({@code PATCH /dictionaries/responsiblePersons/{id}}).
     *
     * @param id the entry to update
     * @param update the fields to change; unset fields are left untouched
     * @return the updated entry
     */
    ResponsibleParty updateResponsiblePerson(long id, ResponsiblePartyUpdate update);

    /**
     * Delete a responsible person ({@code DELETE /dictionaries/responsiblePersons/{id}}).
     *
     * @param id the entry to delete
     */
    void deleteResponsiblePerson(long id);

    /**
     * The shop's product producers ({@code GET /dictionaries/responsibleProducers}).
     *
     * @param query optional filters (never null; use {@link ResponsiblePartyQuery#none()} for all)
     * @return the matching entries; never {@code null}
     */
    List<ResponsibleParty> responsibleProducers(ResponsiblePartyQuery query);

    /**
     * Create a product producer ({@code POST /dictionaries/responsibleProducers}).
     *
     * @param party the entry to create; its {@code idempotenceKey} must be unused in this shop
     * @return the created entry
     * @throws io.github.mgrtomaszzurawski.erli.core.error.ErliValidationException if the
     *         {@code idempotenceKey} is already taken (HTTP 409) or a field is rejected
     */
    ResponsibleParty createResponsibleProducer(NewResponsibleParty party);

    /**
     * Update a product producer ({@code PATCH /dictionaries/responsibleProducers/{id}}).
     *
     * @param id the entry to update
     * @param update the fields to change; unset fields are left untouched
     * @return the updated entry
     */
    ResponsibleParty updateResponsibleProducer(long id, ResponsiblePartyUpdate update);

    /**
     * Delete a product producer ({@code DELETE /dictionaries/responsibleProducers/{id}}).
     *
     * @param id the entry to delete
     */
    void deleteResponsibleProducer(long id);

    /**
     * The shop's product attachments ({@code GET /dictionaries/attachments}).
     *
     * @param query optional filters (never null; use {@link AttachmentQuery#none()} for all)
     * @return the matching attachments; never {@code null}
     */
    List<Attachment> attachments(AttachmentQuery query);

    /**
     * Create an attachment ({@code POST /dictionaries/attachment}).
     *
     * @param attachment the attachment to create; the file itself is uploaded out of band
     * @return the created attachment
     */
    Attachment createAttachment(NewAttachment attachment);

    /**
     * Update an attachment ({@code PATCH /dictionaries/attachment}).
     *
     * @param update the fields to change; unset fields are left untouched
     * @return the updated attachment
     */
    Attachment updateAttachment(AttachmentUpdate update);

    /**
     * Attach products to an attachment ({@code PATCH /dictionaries/attachment/attach}).
     *
     * <p>Partial success is reported with HTTP 200, so check the result rather than relying on the
     * absence of an exception — see {@link ProductAttachmentResult}.
     *
     * @param attachmentId the attachment to attach products to
     * @param productIds the products to attach; at least one
     * @return which products were attached and which the API refused
     */
    ProductAttachmentResult attachProducts(long attachmentId, List<Long> productIds);

    /**
     * Detach products from an attachment ({@code PATCH /dictionaries/attachment/detach}).
     *
     * <p>Partial success is reported with HTTP 200, so check the result rather than relying on the
     * absence of an exception — see {@link ProductAttachmentResult}.
     *
     * @param attachmentId the attachment to detach products from
     * @param productIds the products to detach; at least one
     * @return which products were detached and which the API refused
     */
    ProductAttachmentResult detachProducts(long attachmentId, List<Long> productIds);

    /**
     * Delete attachments ({@code DELETE /dictionaries/attachments}).
     *
     * <p>The API reports removals and failures separately rather than failing the whole call, so a
     * partial success is normal — check {@link AttachmentRemoval#isComplete()}.
     *
     * @param attachmentIds the attachments to delete; at least one
     * @return what was removed and what failed
     */
    AttachmentRemoval deleteAttachments(List<Long> attachmentIds);
}
