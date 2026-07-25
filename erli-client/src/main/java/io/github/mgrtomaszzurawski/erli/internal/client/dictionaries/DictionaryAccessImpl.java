package io.github.mgrtomaszzurawski.erli.internal.client.dictionaries;

import io.github.mgrtomaszzurawski.erli.core.model.CategoryId;
import io.github.mgrtomaszzurawski.erli.core.model.Cursor;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.Attribute;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.AttributeValues;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.BillingEntryType;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.Category;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.DeliveryMethod;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.DeliveryMethodFilter;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.DeliveryVendor;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.DictionaryAccess;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.NewResponsibleParty;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.PriceListName;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ResponsibleParty;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ResponsiblePartyFilter;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ShippingMethod;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ShippingMethodFilter;
import io.github.mgrtomaszzurawski.erli.internal.ApiPaths;
import io.github.mgrtomaszzurawski.erli.internal.CursorPagination;
import io.github.mgrtomaszzurawski.erli.internal.HttpRuntime;
import io.github.mgrtomaszzurawski.erli.internal.Page;
import io.github.mgrtomaszzurawski.erli.rest.model.AttributeFilter;
import io.github.mgrtomaszzurawski.erli.rest.model.AttributeResponseInner;
import io.github.mgrtomaszzurawski.erli.rest.model.AttributeValuesFilter;
import io.github.mgrtomaszzurawski.erli.rest.model.AttributeValuesResponseInner;
import io.github.mgrtomaszzurawski.erli.rest.model.BillingEntryTypesResponseInner;
import io.github.mgrtomaszzurawski.erli.rest.model.CategoryFilter;
import io.github.mgrtomaszzurawski.erli.rest.model.CategoryResponse;
import io.github.mgrtomaszzurawski.erli.rest.model.ResponsibleSchema;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * {@link DictionaryAccess} implementation: calls the {@code /dictionaries} endpoints through the
 * shared {@link HttpRuntime} and maps the raw responses to domain records. Internal: never exported.
 */
public final class DictionaryAccessImpl implements DictionaryAccess {

    /** The largest page the API accepts for the category cursor; a larger `limit` is rejected with 400. */
    private static final int CATEGORY_PAGE_SIZE = 200;

    private static final String PRICE_LIST_PLACEHOLDER = "{priceList}";

    private final HttpRuntime runtime;

    public DictionaryAccessImpl(HttpRuntime runtime) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
    }

    @Override
    public Stream<Category> categories() {
        // Reuses the shared cursor spliterator rather than a private loop. The API's cursor is the id
        // of the last category seen, passed back as `after`, so the Cursor carries it as decimal text.
        return CursorPagination.stream(this::fetchCategoryPage);
    }

    private Page<Category> fetchCategoryPage(Cursor after) {
        CategoryFilter filter = new CategoryFilter().limit(CATEGORY_PAGE_SIZE);
        if (after != null) {
            filter.after(Integer.valueOf(after.value()));
        }
        CategoryResponse[] raw = runtime.post(ApiPaths.DICTIONARIES_CATEGORY_SEARCH, filter, CategoryResponse[].class);
        List<Category> categories = CategoryMapper.toDomainList(raw);
        if (categories.size() < CATEGORY_PAGE_SIZE) {
            // A short page is the last page; a null cursor stops the spliterator.
            return new Page<>(categories, null);
        }
        Category last = categories.get(categories.size() - 1);
        return new Page<>(categories, Cursor.of(String.valueOf(CategoryMapper.numericId(last.id()))));
    }

    @Override
    public List<Attribute> attributes(CategoryId categoryId) {
        AttributeFilter filter = new AttributeFilter().categoryId(numericCategoryId(categoryId));
        AttributeResponseInner[] raw =
                runtime.post(ApiPaths.DICTIONARIES_ATTRIBUTES_SEARCH, filter, AttributeResponseInner[].class);
        return AttributeMapper.toAttributeList(raw);
    }

    @Override
    public List<AttributeValues> attributeValues(CategoryId categoryId) {
        AttributeValuesFilter filter = new AttributeValuesFilter().categoryId(numericCategoryId(categoryId));
        AttributeValuesResponseInner[] raw =
                runtime.post(ApiPaths.DICTIONARIES_ATTRIBUTE_VALUES_SEARCH, filter, AttributeValuesResponseInner[].class);
        return AttributeMapper.toAttributeValuesList(raw);
    }

    @Override
    public List<DeliveryMethod> deliveryMethods() {
        return deliveryMethods(DeliveryMethodFilter.all());
    }

    @Override
    public List<DeliveryMethod> deliveryMethods(DeliveryMethodFilter filter) {
        Objects.requireNonNull(filter, "filter");
        io.github.mgrtomaszzurawski.erli.rest.model.DeliveryMethod[] raw =
                runtime.get(ApiPaths.DICTIONARIES_DELIVERY_METHODS + deliveryMethodQuery(filter),
                        io.github.mgrtomaszzurawski.erli.rest.model.DeliveryMethod[].class);
        return DeliveryMethodMapper.toDomainList(raw);
    }

    @Override
    public List<DeliveryMethod> deliveryMethods(PriceListName priceList, DeliveryMethodFilter filter) {
        Objects.requireNonNull(priceList, "priceList");
        Objects.requireNonNull(filter, "filter");
        String encodedPriceList = PathSegment.encode(priceList.value());
        io.github.mgrtomaszzurawski.erli.rest.model.DeliveryMethod[] raw =
                runtime.get(ApiPaths.DICTIONARIES_DELIVERY_METHODS_BY_PRICE_LIST.replace(PRICE_LIST_PLACEHOLDER, encodedPriceList) + deliveryMethodQuery(filter),
                        io.github.mgrtomaszzurawski.erli.rest.model.DeliveryMethod[].class);
        return DeliveryMethodMapper.toDomainList(raw);
    }

    @Override
    public List<DeliveryVendor> deliveryVendors() {
        // The dictionary is a bare array of carrier identifiers, not of objects.
        String[] raw = runtime.get(ApiPaths.DICTIONARIES_DELIVERY_VENDORS, String[].class);
        if (raw == null) {
            return List.of();
        }
        return Arrays.stream(raw).map(DeliveryVendor::of).toList();
    }

    @Override
    public List<ShippingMethod> shippingMethods(ShippingMethodFilter filter) {
        Objects.requireNonNull(filter, "filter");
        String query = QueryString.builder()
                .add("id", filter.id().map(Object::toString))
                .add("groupId", filter.groupId())
                .add("operator", filter.operator().map(Object::toString))
                .add("cod", filter.cashOnDelivery())
                .build();
        io.github.mgrtomaszzurawski.erli.rest.model.ShippingMethod[] raw =
                runtime.get(ApiPaths.DICTIONARIES_SHIPPING_METHODS + query,
                        io.github.mgrtomaszzurawski.erli.rest.model.ShippingMethod[].class);
        return ShippingMethodMapper.toDomainList(raw);
    }

    @Override
    public List<BillingEntryType> billingEntryTypes() {
        BillingEntryTypesResponseInner[] raw =
                runtime.get(ApiPaths.DICTIONARIES_BILLING_ENTRY_TYPES, BillingEntryTypesResponseInner[].class);
        return BillingEntryTypeMapper.toDomainList(raw);
    }

    @Override
    public List<ResponsibleParty> responsiblePersons(ResponsiblePartyFilter filter) {
        Objects.requireNonNull(filter, "filter");
        ResponsibleSchema[] raw =
                runtime.get(ApiPaths.DICTIONARIES_RESPONSIBLE_PERSONS + responsiblePartyQuery(filter),
                        ResponsibleSchema[].class);
        return ResponsiblePartyMapper.toDomainList(raw);
    }

    @Override
    public List<ResponsibleParty> createResponsiblePerson(NewResponsibleParty party) {
        ResponsibleSchema[] raw = runtime.post(ApiPaths.DICTIONARIES_RESPONSIBLE_PERSONS,
                ResponsiblePartyMapper.toCreateRequest(party), ResponsibleSchema[].class);
        return ResponsiblePartyMapper.toDomainList(raw);
    }

    @Override
    public List<ResponsibleParty> responsibleProducers(ResponsiblePartyFilter filter) {
        Objects.requireNonNull(filter, "filter");
        ResponsibleSchema[] raw =
                runtime.get(ApiPaths.DICTIONARIES_RESPONSIBLE_PRODUCERS + responsiblePartyQuery(filter),
                        ResponsibleSchema[].class);
        return ResponsiblePartyMapper.toDomainList(raw);
    }

    @Override
    public List<ResponsibleParty> createResponsibleProducer(NewResponsibleParty party) {
        ResponsibleSchema[] raw = runtime.post(ApiPaths.DICTIONARIES_RESPONSIBLE_PRODUCERS,
                ResponsiblePartyMapper.toCreateRequest(party), ResponsibleSchema[].class);
        return ResponsiblePartyMapper.toDomainList(raw);
    }

    private static String deliveryMethodQuery(DeliveryMethodFilter filter) {
        return QueryString.builder()
                .add("id", filter.id().map(Object::toString))
                .add("cod", filter.cashOnDelivery())
                .add("vendor", filter.vendor().map(Object::toString))
                .build();
    }

    private static String responsiblePartyQuery(ResponsiblePartyFilter filter) {
        return QueryString.builder()
                .add("id", filter.id())
                .add("name", filter.name())
                .build();
    }

    /** Category ids are integers on the wire; {@link CategoryId} is core-owned and string-valued. */
    private static Integer numericCategoryId(CategoryId categoryId) {
        Objects.requireNonNull(categoryId, "categoryId");
        return CategoryMapper.numericId(categoryId);
    }
}
