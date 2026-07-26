package io.github.mgrtomaszzurawski.erli.internal.client.dictionaries;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.mgrtomaszzurawski.erli.core.model.AttachmentKind;
import io.github.mgrtomaszzurawski.erli.core.model.CategoryId;
import io.github.mgrtomaszzurawski.erli.core.model.Cursor;
import io.github.mgrtomaszzurawski.erli.core.model.DeliveryMethodId;
import io.github.mgrtomaszzurawski.erli.core.model.DeliveryVendor;
import io.github.mgrtomaszzurawski.erli.core.model.ShippingMethodId;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.Attachment;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.AttachmentQuery;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.AttachmentRemoval;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.AttachmentUpdate;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.Attribute;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.AttributeValues;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.BillingEntryType;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.Category;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.DeliveryMethod;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.DeliveryMethodQuery;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.DictionariesAccess;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.NewAttachment;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.NewResponsibleParty;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.PriceListName;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ProductAttachmentResult;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ResponsibleParty;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ResponsiblePartyQuery;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ResponsiblePartyUpdate;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ShippingMethod;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ShippingMethodQuery;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ShippingOperator;
import io.github.mgrtomaszzurawski.erli.internal.ApiPaths;
import io.github.mgrtomaszzurawski.erli.internal.CursorPagination;
import io.github.mgrtomaszzurawski.erli.internal.HttpRuntime;
import io.github.mgrtomaszzurawski.erli.internal.JsonCodec;
import io.github.mgrtomaszzurawski.erli.internal.Page;
import io.github.mgrtomaszzurawski.erli.internal.PathTemplate;
import io.github.mgrtomaszzurawski.erli.internal.QueryParameters;
import io.github.mgrtomaszzurawski.erli.rest.model.AttachmentResponse;
import io.github.mgrtomaszzurawski.erli.rest.model.AttributeFilter;
import io.github.mgrtomaszzurawski.erli.rest.model.AttributeResponseInner;
import io.github.mgrtomaszzurawski.erli.rest.model.AttributeValuesFilter;
import io.github.mgrtomaszzurawski.erli.rest.model.AttributeValuesResponseInner;
import io.github.mgrtomaszzurawski.erli.rest.model.BillingEntryTypesResponseInner;
import io.github.mgrtomaszzurawski.erli.rest.model.CategoryFilter;
import io.github.mgrtomaszzurawski.erli.rest.model.CategoryResponse;
import io.github.mgrtomaszzurawski.erli.rest.model.DeleteAttachmentsResponse;
import io.github.mgrtomaszzurawski.erli.rest.model.GetAttachmentsResponseInner;
import io.github.mgrtomaszzurawski.erli.rest.model.ResponsibleSchema;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * {@link DictionariesAccess} implementation: turns the domain query records into transport
 * {@link QueryParameters}, calls the {@code /dictionaries} endpoints through the shared
 * {@link HttpRuntime}, and maps the raw responses to domain records. Internal: never exported.
 */
public final class DictionariesAccessImpl implements DictionariesAccess {

    private static final String PARAM_ID = "id";
    private static final String PARAM_COD = "cod";
    private static final String PARAM_VENDOR = "vendor";
    private static final String PARAM_GROUP_ID = "groupId";
    private static final String PARAM_OPERATOR = "operator";
    private static final String PARAM_NAME = "name";
    private static final String PARAM_KIND = "kind";

    /** The largest page the API accepts for the category cursor; a larger limit is rejected with 400. */
    private static final int CATEGORY_PAGE_SIZE = 200;

    private final HttpRuntime runtime;
    private final JsonCodec codec;

    public DictionariesAccessImpl(HttpRuntime runtime, JsonCodec codec) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.codec = Objects.requireNonNull(codec, "codec");
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
        List<Category> categories =
                runtime.postList(ApiPaths.DICTIONARIES_CATEGORY_SEARCH, filter, CategoryResponse.class)
                        .stream()
                        .map(CategoryMapper::toDomain)
                        .toList();
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
        return runtime.postList(ApiPaths.DICTIONARIES_ATTRIBUTES_SEARCH, filter, AttributeResponseInner.class)
                .stream()
                .map(AttributeMapper::toAttribute)
                .toList();
    }

    @Override
    public List<AttributeValues> attributeValues(CategoryId categoryId) {
        AttributeValuesFilter filter = new AttributeValuesFilter().categoryId(numericCategoryId(categoryId));
        return runtime.postList(ApiPaths.DICTIONARIES_ATTRIBUTE_VALUES_SEARCH, filter, AttributeValuesResponseInner.
                class)
                .stream()
                .map(AttributeMapper::toAttributeValues)
                .toList();
    }

    @Override
    public List<DeliveryMethod> deliveryMethods() {
        return deliveryMethods(DeliveryMethodQuery.none());
    }

    @Override
    public List<DeliveryMethod> deliveryMethods(DeliveryMethodQuery query) {
        Objects.requireNonNull(query, "query");
        return runtime.getList(ApiPaths.DICTIONARIES_DELIVERY_METHODS, deliveryMethodParameters(query),
                        io.github.mgrtomaszzurawski.erli.rest.model.DeliveryMethod.class)
                .stream()
                .map(DeliveryMethodMapper::toDomain)
                .toList();
    }

    @Override
    public List<DeliveryMethod> deliveryMethods(PriceListName priceList, DeliveryMethodQuery query) {
        Objects.requireNonNull(priceList, "priceList");
        Objects.requireNonNull(query, "query");
        Map<String, String> pathValues = Map.of(ApiPaths.PRICE_LIST_PARAM, priceList.value());
        return runtime.getList(PathTemplate.expand(ApiPaths.DICTIONARIES_DELIVERY_METHODS_BY_PRICE_LIST, pathValues),
                        deliveryMethodParameters(query),
                        io.github.mgrtomaszzurawski.erli.rest.model.DeliveryMethod.class)
                .stream()
                .map(DeliveryMethodMapper::toDomain)
                .toList();
    }

    @Override
    public List<DeliveryVendor> deliveryVendors() {
        // The dictionary is a bare array of carrier identifiers, not of objects.
        return runtime.getList(ApiPaths.DICTIONARIES_DELIVERY_VENDORS, QueryParameters.empty(), String.class)
                .stream()
                .map(DeliveryVendor::fromWire)
                .toList();
    }

    @Override
    public List<ShippingMethod> shippingMethods(ShippingMethodQuery query) {
        Objects.requireNonNull(query, "query");
        ShippingMethodId id = query.id();
        ShippingOperator operator = query.operator();
        QueryParameters parameters = QueryParameters.builder()
                .add(PARAM_ID, id == null ? null : id.value())
                .add(PARAM_GROUP_ID, query.groupId())
                .add(PARAM_OPERATOR, operator == null ? null : operator.wireValue())
                .addBoolean(PARAM_COD, query.cashOnDelivery())
                .build();
        // Read as a tree: the maxDimensions anyOf can only be discriminated before it is decoded.
        return runtime.getList(ApiPaths.DICTIONARIES_SHIPPING_METHODS, parameters, JsonNode.class)
                .stream()
                .map(rawMethod -> ShippingMethodMapper.toDomain(rawMethod, codec))
                .toList();
    }

    @Override
    public List<BillingEntryType> billingEntryTypes() {
        return runtime.getList(ApiPaths.DICTIONARIES_BILLING_ENTRY_TYPES, QueryParameters.empty(),
                        BillingEntryTypesResponseInner.class)
                .stream()
                .map(BillingEntryTypeMapper::toDomain)
                .toList();
    }

    @Override
    public List<ResponsibleParty> responsiblePersons(ResponsiblePartyQuery query) {
        Objects.requireNonNull(query, "query");
        return runtime.getList(ApiPaths.DICTIONARIES_RESPONSIBLE_PERSONS, responsiblePartyParameters(query),
                        ResponsibleSchema.class)
                .stream()
                .map(ResponsiblePartyMapper::toDomain)
                .toList();
    }

    @Override
    public ResponsibleParty createResponsiblePerson(NewResponsibleParty party) {
        return ResponsiblePartyMapper.toDomain(runtime.post(ApiPaths.DICTIONARIES_RESPONSIBLE_PERSONS,
                ResponsiblePartyMapper.toCreateRequest(party), ResponsibleSchema.class));
    }

    @Override
    public ResponsibleParty updateResponsiblePerson(long id, ResponsiblePartyUpdate update) {
        Objects.requireNonNull(update, "update");
        Map<String, String> pathValues = Map.of(ApiPaths.RESPONSIBLE_ID_PARAM, String.valueOf(id));
        return ResponsiblePartyMapper.toDomain(
                runtime.patch(PathTemplate.expand(ApiPaths.DICTIONARIES_RESPONSIBLE_PERSON_BY_ID, pathValues),
                        ResponsiblePartyMapper.toUpdateRequest(update), ResponsibleSchema.class));
    }

    @Override
    public void deleteResponsiblePerson(long id) {
        Map<String, String> pathValues = Map.of(ApiPaths.RESPONSIBLE_ID_PARAM, String.valueOf(id));
        runtime.delete(PathTemplate.expand(ApiPaths.DICTIONARIES_RESPONSIBLE_PERSON_BY_ID, pathValues),
                QueryParameters.empty(), Void.class);
    }

    @Override
    public List<ResponsibleParty> responsibleProducers(ResponsiblePartyQuery query) {
        Objects.requireNonNull(query, "query");
        return runtime.getList(ApiPaths.DICTIONARIES_RESPONSIBLE_PRODUCERS, responsiblePartyParameters(query),
                        ResponsibleSchema.class)
                .stream()
                .map(ResponsiblePartyMapper::toDomain)
                .toList();
    }

    @Override
    public ResponsibleParty createResponsibleProducer(NewResponsibleParty party) {
        return ResponsiblePartyMapper.toDomain(runtime.post(ApiPaths.DICTIONARIES_RESPONSIBLE_PRODUCERS,
                ResponsiblePartyMapper.toCreateRequest(party), ResponsibleSchema.class));
    }

    @Override
    public ResponsibleParty updateResponsibleProducer(long id, ResponsiblePartyUpdate update) {
        Objects.requireNonNull(update, "update");
        Map<String, String> pathValues = Map.of(ApiPaths.RESPONSIBLE_ID_PARAM, String.valueOf(id));
        return ResponsiblePartyMapper.toDomain(
                runtime.patch(PathTemplate.expand(ApiPaths.DICTIONARIES_RESPONSIBLE_PRODUCER_BY_ID, pathValues),
                        ResponsiblePartyMapper.toUpdateRequest(update), ResponsibleSchema.class));
    }

    @Override
    public void deleteResponsibleProducer(long id) {
        Map<String, String> pathValues = Map.of(ApiPaths.RESPONSIBLE_ID_PARAM, String.valueOf(id));
        runtime.delete(PathTemplate.expand(ApiPaths.DICTIONARIES_RESPONSIBLE_PRODUCER_BY_ID, pathValues),
                QueryParameters.empty(), Void.class);
    }

    @Override
    public List<Attachment> attachments(AttachmentQuery query) {
        Objects.requireNonNull(query, "query");
        AttachmentKind kind = query.kind();
        Long id = query.id();
        QueryParameters parameters = QueryParameters.builder()
                .add(PARAM_ID, id == null ? null : String.valueOf(id))
                .add(PARAM_KIND, kind == null ? null : kind.wireValue())
                .add(PARAM_NAME, query.name())
                .build();
        return runtime.getList(ApiPaths.DICTIONARIES_ATTACHMENTS, parameters, GetAttachmentsResponseInner.class)
                .stream()
                .map(AttachmentMapper::toDomain)
                .toList();
    }

    @Override
    public Attachment createAttachment(NewAttachment attachment) {
        AttachmentResponse raw = runtime.post(ApiPaths.DICTIONARIES_ATTACHMENT,
                AttachmentMapper.toCreateRequest(attachment), AttachmentResponse.class);
        return AttachmentMapper.toDomain(raw);
    }

    @Override
    public Attachment updateAttachment(AttachmentUpdate update) {
        AttachmentResponse raw = runtime.patch(ApiPaths.DICTIONARIES_ATTACHMENT,
                AttachmentMapper.toPatchRequest(update), AttachmentResponse.class);
        return AttachmentMapper.toDomain(raw);
    }

    @Override
    public ProductAttachmentResult attachProducts(long attachmentId, List<Long> productIds) {
        return AttachmentMapper.toProductAttachmentResult(runtime.patch(ApiPaths.DICTIONARIES_ATTACHMENT_ATTACH,
                AttachmentMapper.toAttachRequest(attachmentId, productIds), ManageAttachedProductsResponse.class));
    }

    @Override
    public ProductAttachmentResult detachProducts(long attachmentId, List<Long> productIds) {
        return AttachmentMapper.toProductAttachmentResult(runtime.patch(ApiPaths.DICTIONARIES_ATTACHMENT_DETACH,
                AttachmentMapper.toDetachRequest(attachmentId, productIds), ManageAttachedProductsResponse.class));
    }

    @Override
    public AttachmentRemoval deleteAttachments(List<Long> attachmentIds) {
        return AttachmentMapper.toRemoval(runtime.delete(ApiPaths.DICTIONARIES_ATTACHMENTS,
                AttachmentMapper.toDeleteRequest(attachmentIds), QueryParameters.empty(),
                DeleteAttachmentsResponse.class));
    }

    private static QueryParameters deliveryMethodParameters(DeliveryMethodQuery query) {
        DeliveryMethodId id = query.id();
        DeliveryVendor vendor = query.vendor();
        return QueryParameters.builder()
                .add(PARAM_ID, id == null ? null : id.value())
                .addBoolean(PARAM_COD, query.cashOnDelivery())
                .add(PARAM_VENDOR, vendor == null ? null : vendor.wireValue())
                .build();
    }

    private static QueryParameters responsiblePartyParameters(ResponsiblePartyQuery query) {
        Long id = query.id();
        return QueryParameters.builder()
                .add(PARAM_ID, id == null ? null : String.valueOf(id))
                .add(PARAM_NAME, query.name())
                .build();
    }

    /** Category ids are integers on the wire; {@link CategoryId} is core-owned and string-valued. */
    private static Integer numericCategoryId(CategoryId categoryId) {
        Objects.requireNonNull(categoryId, "categoryId");
        return CategoryMapper.numericId(categoryId);
    }
}
