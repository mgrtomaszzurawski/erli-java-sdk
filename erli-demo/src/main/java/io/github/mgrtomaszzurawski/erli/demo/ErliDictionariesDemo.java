package io.github.mgrtomaszzurawski.erli.demo;

import io.github.mgrtomaszzurawski.erli.ErliClient;
import io.github.mgrtomaszzurawski.erli.core.model.CategoryId;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.Attribute;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.AttributeValues;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.BillingEntryType;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.Category;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.DeliveryMethod;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.DeliveryMethodFilter;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.DeliveryVendor;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.DictionaryAccess;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ResponsibleParty;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ResponsiblePartyFilter;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ShippingMethod;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ShippingMethodFilter;

import java.util.List;

/**
 * Live end-to-end proof of the Dictionaries bucket: build a client from the environment
 * ({@code ERLI_BASE_URL} + {@code ERLI_API_KEY}) and exercise every implemented read against the real
 * sandbox.
 *
 * <p>Dictionaries are marketplace-wide reference data, so these return real payloads even though the
 * sandbox shop itself is empty — which is why bucket D doubles as the live-test enabler for the fleet.
 *
 * <p>Run: {@code ./gradlew :erli-demo:runDictionaries} with the two environment variables set (sourced
 * from {@code /workspace/shared/secrets/erli-sandbox.env}). The API key is never printed.
 */
public final class ErliDictionariesDemo {

    private static final int SAMPLE_SIZE = 3;
    private static final int CATEGORY_PROBE_LIMIT = 400;
    private static final CategoryId SAMPLE_LEAF_CATEGORY = CategoryId.of("4");

    private ErliDictionariesDemo() {
    }

    public static void main(String[] args) {
        try (ErliClient client = ErliClient.fromEnvironment()) {
            DictionaryAccess dictionaries = client.dictionaries();

            List<DeliveryMethod> deliveryMethods = dictionaries.deliveryMethods();
            System.out.printf("deliveryMethods           -> %d%n", deliveryMethods.size());

            List<DeliveryMethod> codOnly = dictionaries.deliveryMethods(
                    DeliveryMethodFilter.builder().cashOnDelivery(true).build());
            System.out.printf("deliveryMethods(cod=true) -> %d (filter narrowed the result: %b)%n",
                    codOnly.size(), codOnly.size() < deliveryMethods.size());

            List<DeliveryVendor> vendors = dictionaries.deliveryVendors();
            System.out.printf("deliveryVendors           -> %d %s%n", vendors.size(), vendors);

            List<ShippingMethod> shippingMethods = dictionaries.shippingMethods(ShippingMethodFilter.all());
            System.out.printf("shippingMethods           -> %d%n", shippingMethods.size());
            shippingMethods.stream().limit(SAMPLE_SIZE).forEach(method -> System.out.printf(
                    "    %-28s operator=%-8s maxDimensions=%s%n",
                    method.id(), method.operator().map(Object::toString).orElse("-"),
                    method.maxDimensions().map(Object::toString).orElse("-")));

            List<BillingEntryType> billingEntryTypes = dictionaries.billingEntryTypes();
            System.out.printf("billingEntryTypes         -> %d%n", billingEntryTypes.size());

            List<ResponsibleParty> persons = dictionaries.responsiblePersons(ResponsiblePartyFilter.all());
            List<ResponsibleParty> producers = dictionaries.responsibleProducers(ResponsiblePartyFilter.all());
            System.out.printf("responsiblePersons        -> %d%n", persons.size());
            System.out.printf("responsibleProducers      -> %d%n", producers.size());

            // A category known to be a leaf on the sandbox ("Elementy dekarskie"), so the attribute
            // probe does not depend on walking the tree first.
            List<Attribute> attributes = dictionaries.attributes(SAMPLE_LEAF_CATEGORY);
            List<AttributeValues> attributeValues = dictionaries.attributeValues(SAMPLE_LEAF_CATEGORY);
            System.out.printf("attributes(category %s)    -> %d, attributeValues -> %d%n",
                    SAMPLE_LEAF_CATEGORY, attributes.size(), attributeValues.size());
            attributes.stream().limit(SAMPLE_SIZE).forEach(attribute -> System.out.printf(
                    "    %-24s type=%-11s required=%-5b maxValues=%s%n",
                    attribute.name(), attribute.type(), attribute.required(), attribute.maxValues()));

            // Last, because it is the one read still blocked by the core JsonCodec null-serialization
            // defect (BACKLOG CORE-2): page 1 sends "after": null and the API answers 400.
            // Exercises the lazy cursor: only as many pages as the terminal operation needs.
            List<Category> categories = dictionaries.categories().limit(CATEGORY_PROBE_LIMIT).toList();
            System.out.printf("categories (first %d)     -> %d, deepest breadcrumb=%d%n",
                    CATEGORY_PROBE_LIMIT, categories.size(),
                    categories.stream().mapToInt(category -> category.breadcrumb().size()).max().orElse(0));
        }
    }
}
