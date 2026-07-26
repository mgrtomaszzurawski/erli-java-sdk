package io.github.mgrtomaszzurawski.erli.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.erli.core.model.BankAccount;
import io.github.mgrtomaszzurawski.erli.core.model.Buyer;
import io.github.mgrtomaszzurawski.erli.core.model.Country;
import io.github.mgrtomaszzurawski.erli.core.model.DeliveryAddress;
import io.github.mgrtomaszzurawski.erli.core.model.InvoiceAddress;
import io.github.mgrtomaszzurawski.erli.core.model.InvoiceAddressType;
import io.github.mgrtomaszzurawski.erli.core.model.ProductExternalId;
import io.github.mgrtomaszzurawski.erli.domain.hooks.BuyabilityQuery;
import io.github.mgrtomaszzurawski.erli.domain.hooks.Hook;
import io.github.mgrtomaszzurawski.erli.domain.hooks.HookKind;
import io.github.mgrtomaszzurawski.erli.domain.hooks.ProductSyncNotification;
import io.github.mgrtomaszzurawski.erli.domain.inbox.MessageQuery;
import io.github.mgrtomaszzurawski.erli.domain.inbox.MessageType;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

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
    void hookRedactsItsAccessTokenButKeepsTheEndpointReadable() {
        Hook hook = Hook.of(HookKind.CHECK_BUYABILITY, URI.create(HOOK_URL), ACCESS_TOKEN);

        String described = hook.toString();

        assertFalse(described.contains(ACCESS_TOKEN), "the shop's own credential must not be printable");
        assertTrue(described.contains(HOOK_URL), "a query-less endpoint stays fully readable for debugging");
    }

    /** A webhook URL commonly carries the shared secret as a query parameter, so the query goes too. */
    @Test
    void hookRedactsASecretCarriedInTheUrlQuery() {
        Hook hook = Hook.of(HookKind.CHECK_BUYABILITY, URI.create(HOOK_URL + "?token=" + ACCESS_TOKEN));

        String described = hook.toString();

        assertFalse(described.contains(ACCESS_TOKEN), "a secret in the query must not be printable");
        assertTrue(described.contains(HOOK_URL), "scheme, host and path stay visible");
    }

    @Test
    void hookRejectsAnOverlongUrlOrToken() {
        URI overlongUrl = URI.create("https://shop.example/" + "x".repeat(Hook.MAX_URL_LENGTH));
        URI validUrl = URI.create(HOOK_URL);
        String overlongToken = "t".repeat(Hook.MAX_ACCESS_TOKEN_LENGTH + 1);

        assertThrows(IllegalArgumentException.class, () -> Hook.of(HookKind.ORDER_CREATED, overlongUrl));
        assertThrows(IllegalArgumentException.class,
                () -> Hook.of(HookKind.ORDER_CREATED, validUrl, overlongToken));
    }

    /**
     * Erli sends the access token and, for the {@code ORDER_*} kinds, the buyer's personal data to this
     * endpoint, so a cleartext or credential-bearing URL is refused rather than merely discouraged.
     */
    @Test
    void hookRejectsAnEndpointThatIsNotPlainHttps() {
        // Parsed up front: URI.create throws the same exception type being asserted, so leaving it
        // inside the lambda would let a malformed literal pass the test for the wrong reason.
        URI cleartext = URI.create("http://shop.example/hook");
        URI relative = URI.create("/relative/hook");
        URI hostless = URI.create("https:///hook");
        URI withCredentials = URI.create("https://user:pass@shop.example/hook");

        assertThrows(IllegalArgumentException.class, () -> Hook.of(HookKind.ORDER_CREATED, cleartext));
        assertThrows(IllegalArgumentException.class, () -> Hook.of(HookKind.ORDER_CREATED, relative));
        assertThrows(IllegalArgumentException.class, () -> Hook.of(HookKind.ORDER_CREATED, hostless));
        assertThrows(IllegalArgumentException.class, () -> Hook.of(HookKind.ORDER_CREATED, withCredentials));
    }

    @Test
    void hookKindRejectsANameThisVersionDoesNotKnow() {
        assertEquals(HookKind.ORDER_SELLER_STATUS_CHANGED, HookKind.fromWireValue("orderSellerStatusChanged"));
        assertThrows(IllegalArgumentException.class, () -> HookKind.fromWireValue("somethingNew"));
    }

    @Test
    void buyabilityQueryRequiresAPositiveQuantity() {
        ProductExternalId productId = ProductExternalId.of("555");
        assertThrows(IllegalArgumentException.class,
                () -> BuyabilityQuery.of(productId, 0));
    }

    @Test
    void productSyncNotificationEnforcesTheApiBatchLimits() {
        List<ProductExternalId> noProducts = List.of();
        assertThrows(IllegalArgumentException.class, () -> ProductSyncNotification.ofProducts(noProducts));
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

        Set<MessageType> unsupportedTypes = Set.of(MessageType.ORDER_SELLER_STATUS_CHANGED);
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> MessageQuery.ofTypes(unsupportedTypes));
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
