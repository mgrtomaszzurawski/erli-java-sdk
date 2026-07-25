package io.github.mgrtomaszzurawski.erli.demo;

import io.github.mgrtomaszzurawski.erli.ErliClient;
import io.github.mgrtomaszzurawski.erli.core.model.CategoryId;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.AttachmentQuery;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.Attribute;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.AttributeValues;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.BillingEntryType;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.Category;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.CountryCode;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.DeliveryMethod;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.DeliveryMethodQuery;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.DeliveryVendor;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.DictionariesAccess;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.NewResponsibleParty;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ResponsibleParty;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ResponsiblePartyQuery;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ResponsiblePartyUpdate;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ShippingMethod;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ShippingMethodQuery;

import java.util.List;

/**
 * Live end-to-end proof of bucket D Dictionaries: build a client from the environment
 * ({@code ERLI_BASE_URL} + {@code ERLI_API_KEY}) and exercise the dictionary surface against the real
 * sandbox — every read, plus one reversible write→read→delete cycle over the responsible-person
 * dictionary that proves the POST/PATCH/DELETE verbs and leaves the shop as it found it.
 *
 * <p>Most dictionaries are marketplace-wide reference data, so they return real payloads even though
 * the sandbox shop itself is empty — which is why bucket D doubles as the live-test enabler.
 *
 * <p>Run: {@code ./gradlew :erli-demo:runDictionaries} with the two environment variables set (sourced
 * from {@code /workspace/shared/secrets/erli-sandbox.env}). The API key is never printed.
 */
public final class ErliDictionariesDemo {

    private static final int SAMPLE_SIZE = 3;
    private static final int CATEGORY_PROBE_LIMIT = 250;
    /** A category known to be a leaf on the sandbox ("Elementy dekarskie"). */
    private static final CategoryId SAMPLE_LEAF_CATEGORY = CategoryId.of("4");
    private static final String SEED_KEY_PREFIX = "erli-sdk-demo-";

    private ErliDictionariesDemo() {
    }

    public static void main(String[] args) {
        try (ErliClient client = ErliClient.fromEnvironment()) {
            DictionariesAccess dictionaries = client.dictionaries();

            readTheSharedDictionaries(dictionaries);
            readTheCategoryTree(dictionaries);
            writeReadDeleteAResponsiblePerson(dictionaries);
        }
    }

    private static void readTheSharedDictionaries(DictionariesAccess dictionaries) {
        List<DeliveryMethod> deliveryMethods = dictionaries.deliveryMethods();
        System.out.printf("deliveryMethods            -> %d%n", deliveryMethods.size());

        List<DeliveryMethod> codOnly = dictionaries.deliveryMethods(
                DeliveryMethodQuery.builder().cashOnDelivery(true).build());
        System.out.printf("deliveryMethods(cod=true)  -> %d (filter narrowed the result: %b)%n",
                codOnly.size(), codOnly.size() < deliveryMethods.size());

        List<DeliveryVendor> vendors = dictionaries.deliveryVendors();
        System.out.printf("deliveryVendors            -> %d %s%n", vendors.size(), vendors);

        List<ShippingMethod> shippingMethods = dictionaries.shippingMethods(ShippingMethodQuery.none());
        System.out.printf("shippingMethods            -> %d%n", shippingMethods.size());
        shippingMethods.stream().limit(SAMPLE_SIZE).forEach(method -> System.out.printf(
                "    %-28s operator=%-8s maxDimensions=%s%n",
                method.id(), method.operator().map(Object::toString).orElse("-"),
                method.maxDimensions().map(Object::toString).orElse("-")));

        List<BillingEntryType> billingEntryTypes = dictionaries.billingEntryTypes();
        System.out.printf("billingEntryTypes          -> %d%n", billingEntryTypes.size());

        System.out.printf("attachments                -> %d%n",
                dictionaries.attachments(AttachmentQuery.none()).size());
    }

    private static void readTheCategoryTree(DictionariesAccess dictionaries) {
        // Exercises the lazy cursor: only as many pages as the terminal operation needs.
        List<Category> categories = dictionaries.categories().limit(CATEGORY_PROBE_LIMIT).toList();
        System.out.printf("categories (first %d)      -> %d, deepest breadcrumb=%d%n",
                CATEGORY_PROBE_LIMIT, categories.size(),
                categories.stream().mapToInt(category -> category.breadcrumb().size()).max().orElse(0));

        List<Attribute> attributes = dictionaries.attributes(SAMPLE_LEAF_CATEGORY);
        List<AttributeValues> attributeValues = dictionaries.attributeValues(SAMPLE_LEAF_CATEGORY);
        System.out.printf("attributes(category %s)     -> %d, attributeValues -> %d%n",
                SAMPLE_LEAF_CATEGORY, attributes.size(), attributeValues.size());
        attributes.stream().limit(SAMPLE_SIZE).forEach(attribute -> System.out.printf(
                "    %-24s type=%-11s required=%-5b maxValues=%s%n",
                attribute.name(), attribute.type(), attribute.required(), attribute.maxValues()));
    }

    /**
     * Creates a responsible person, reads it back, patches it, then deletes it — proving POST, PATCH
     * and DELETE live. The entry is removed again so the sandbox is left exactly as it was found.
     */
    private static void writeReadDeleteAResponsiblePerson(DictionariesAccess dictionaries) {
        String idempotenceKey = SEED_KEY_PREFIX + System.currentTimeMillis();
        ResponsibleParty created = dictionaries.createResponsiblePerson(NewResponsibleParty.builder()
                .name("SDK demo importer")
                .idempotenceKey(idempotenceKey)
                .properName("SDK Demo Sp. z o.o.")
                .country(CountryCode.PL)
                .address("ul. Testowa 1")
                .postalCode("00-001")
                .city("Warszawa")
                .email("sdk-demo@example.com")
                .build());
        System.out.printf("createResponsiblePerson    -> id=%d, key=%s%n", created.id(), created.idempotenceKey());
        System.out.printf("    toString redacts PII: %s%n", created);

        List<ResponsibleParty> found = dictionaries.responsiblePersons(ResponsiblePartyQuery.byId(created.id()));
        System.out.printf("responsiblePersons(byId)   -> %d%n", found.size());

        ResponsibleParty patched = dictionaries.updateResponsiblePerson(created.id(),
                ResponsiblePartyUpdate.builder(CountryCode.PL).city("Kraków").build());
        System.out.printf("updateResponsiblePerson    -> city now %s%n", patched.city());

        dictionaries.deleteResponsiblePerson(created.id());
        boolean stillThere = dictionaries.responsiblePersons(ResponsiblePartyQuery.byId(created.id()))
                .stream()
                .anyMatch(party -> party.id() == created.id());
        System.out.printf("deleteResponsiblePerson    -> removed: %b (sandbox left clean)%n", !stillThere);
    }
}
