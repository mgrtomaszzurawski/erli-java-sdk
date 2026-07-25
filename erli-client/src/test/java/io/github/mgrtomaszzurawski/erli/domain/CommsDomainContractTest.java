package io.github.mgrtomaszzurawski.erli.domain;

import io.github.mgrtomaszzurawski.erli.core.model.ProductExternalId;
import io.github.mgrtomaszzurawski.erli.domain.hooks.BuyabilityQuery;
import io.github.mgrtomaszzurawski.erli.domain.hooks.Hook;
import io.github.mgrtomaszzurawski.erli.domain.hooks.HookKind;
import io.github.mgrtomaszzurawski.erli.domain.hooks.ProductSyncNotification;
import io.github.mgrtomaszzurawski.erli.domain.inbox.BankAccount;
import io.github.mgrtomaszzurawski.erli.domain.inbox.Buyer;
import io.github.mgrtomaszzurawski.erli.domain.inbox.Country;
import io.github.mgrtomaszzurawski.erli.domain.inbox.DeliveryAddress;
import io.github.mgrtomaszzurawski.erli.domain.inbox.InvoiceAddress;
import io.github.mgrtomaszzurawski.erli.domain.inbox.InvoiceAddressType;
import io.github.mgrtomaszzurawski.erli.domain.inbox.MessageQuery;
import io.github.mgrtomaszzurawski.erli.domain.inbox.MessageType;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract of the public Comms &amp; Automation records: the constraints they enforce up front, and the
 * redaction the SDK promises for buyer personal data and shop credentials (fleet rule 8).
 */
class CommsDomainContractTest {

    private static final String HOOK_URL = "https://shop.example/hook";
    private static final String ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiIs";
    private static final String BUYER_EMAIL = "buyer@example.com";
    private static final String BUYER_PHONE = "601234567";
    private static final String ACCOUNT_NUMBER = "12345678901234567890123456";
    private static final String COMPANY_NAME = "Kowalska Sp. z o.o.";
    private static final String COMPANY_TAX_ID = "5252445767";
    private static final int OVER_THE_PRODUCT_LIMIT = ProductSyncNotification.MAX_PRODUCT_IDS + 1;

    @Test
    void hookRedactsItsAccessTokenAndAnythingCarriedInTheUrlQuery() {
        String described = Hook.of(HookKind.CHECK_BUYABILITY, URI.create(HOOK_URL), ACCESS_TOKEN).toString();

        assertFalse(described.contains(ACCESS_TOKEN), "the shop's own credential must not be printable");
        assertTrue(described.contains("shop.example"), "the host aids debugging and is not a secret");

        // A webhook URL commonly carries the shared secret as a query parameter, so the query goes too.
        String withSecretInQuery = Hook.of(
                HookKind.CHECK_BUYABILITY, URI.create(HOOK_URL + "?token=" + ACCESS_TOKEN)).toString();

        assertFalse(withSecretInQuery.contains(ACCESS_TOKEN), "a secret in the query must not be printable");
        assertTrue(withSecretInQuery.contains("/hook"), "the path is still useful and stays visible");
    }

    @Test
    void hookRejectsAnOverlongUrlOrToken() {
        String longPath = "https://shop.example/" + "x".repeat(Hook.MAX_URL_LENGTH);
        assertThrows(IllegalArgumentException.class,
                () -> Hook.of(HookKind.ORDER_CREATED, URI.create(longPath)));
        assertThrows(IllegalArgumentException.class, () -> Hook.of(HookKind.ORDER_CREATED,
                URI.create(HOOK_URL), "t".repeat(Hook.MAX_ACCESS_TOKEN_LENGTH + 1)));
    }

    /**
     * Erli sends the access token and, for the {@code ORDER_*} kinds, the buyer's personal data to this
     * endpoint, so a cleartext or credential-bearing URL is refused rather than merely discouraged.
     */
    @Test
    void hookRejectsAnEndpointThatIsNotPlainHttps() {
        assertThrows(IllegalArgumentException.class,
                () -> Hook.of(HookKind.ORDER_CREATED, URI.create("http://shop.example/hook")));
        assertThrows(IllegalArgumentException.class,
                () -> Hook.of(HookKind.ORDER_CREATED, URI.create("/relative/hook")));
        assertThrows(IllegalArgumentException.class,
                () -> Hook.of(HookKind.ORDER_CREATED, URI.create("https://user:pass@shop.example/hook")));
    }

    @Test
    void hookKindRejectsANameThisVersionDoesNotKnow() {
        assertEquals(HookKind.ORDER_SELLER_STATUS_CHANGED, HookKind.fromWireValue("orderSellerStatusChanged"));
        assertThrows(IllegalArgumentException.class, () -> HookKind.fromWireValue("somethingNew"));
    }

    @Test
    void buyabilityQueryRequiresAPositiveQuantity() {
        assertThrows(IllegalArgumentException.class,
                () -> BuyabilityQuery.of(ProductExternalId.of("555"), 0));
    }

    @Test
    void productSyncNotificationEnforcesTheApiBatchLimits() {
        assertThrows(IllegalArgumentException.class, () -> ProductSyncNotification.ofProducts(List.of()));
        List<ProductExternalId> tooMany = IntStream.range(0, OVER_THE_PRODUCT_LIMIT)
                .mapToObj(index -> ProductExternalId.of("SKU-" + index))
                .toList();
        assertThrows(IllegalArgumentException.class, () -> ProductSyncNotification.ofProducts(tooMany));
    }

    /**
     * The search filter's enum omits {@code orderSellerStatusChanged} even though messages of that type
     * exist, so the query rejects it locally with an actionable message instead of letting the server
     * answer with a validation error (see {@code KNOWN-SERVER-BEHAVIORS.md}).
     */
    @Test
    void messageQueryRejectsTheTypeTheSearchFilterCannotExpress() {
        assertTrue(MessageQuery.all().types().isEmpty());
        assertEquals(Set.of(MessageType.ORDER_CREATED),
                MessageQuery.ofTypes(Set.of(MessageType.ORDER_CREATED)).types());

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> MessageQuery.ofTypes(Set.of(MessageType.ORDER_SELLER_STATUS_CHANGED)));
        assertTrue(thrown.getMessage().contains("ORDER_SELLER_STATUS_CHANGED"), thrown.getMessage());
    }

    @Test
    void buyerDataIsRedactedInEveryRecordThatCarriesIt() {
        DeliveryAddress delivery = new DeliveryAddress("Anna", "Kowalska", Optional.empty(),
                "Prosta 12", "Prosta", "12", Optional.empty(), "00-838", "Warszawa", Country.PL, BUYER_PHONE);
        InvoiceAddress invoice = new InvoiceAddress(InvoiceAddressType.COMPANY, "Krucza 5", "Krucza", "5",
                Optional.empty(), "00-548", "Warszawa", Country.PL, Optional.of("Anna"),
                Optional.of("Kowalska"), Optional.of(COMPANY_NAME), Optional.of(COMPANY_TAX_ID));
        Buyer buyer = new Buyer(BUYER_EMAIL, delivery, Optional.of(invoice));

        String described = buyer.toString();
        assertFalse(described.contains(BUYER_EMAIL), "e-mail must not be printable");
        assertFalse(described.contains("Kowalska"), "the buyer's name must not be printable");
        assertFalse(described.contains(BUYER_PHONE), "the buyer's phone must not be printable");
        assertFalse(described.contains("Prosta"), "the delivery street must not be printable");
        // The invoice address is reached through Optional<InvoiceAddress>, so it must redact too.
        assertFalse(described.contains("Krucza"), "the invoice street must not be printable");
        assertFalse(described.contains(COMPANY_NAME), "the invoice company must not be printable");
        assertFalse(described.contains(COMPANY_TAX_ID), "the invoice tax id must not be printable");
        // The non-identifying locality stays visible so a log line is still useful for diagnosis.
        assertTrue(described.contains("Warszawa"));
        assertTrue(described.contains("00-838"));

        String account = new BankAccount(ACCOUNT_NUMBER, "Anna Kowalska").toString();
        assertFalse(account.contains(ACCOUNT_NUMBER), "a refund account number must not be printable");
        assertFalse(account.contains("Kowalska"));
    }
}
