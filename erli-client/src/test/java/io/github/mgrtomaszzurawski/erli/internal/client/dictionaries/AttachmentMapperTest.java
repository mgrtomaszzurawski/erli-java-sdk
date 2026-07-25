package io.github.mgrtomaszzurawski.erli.internal.client.dictionaries;

import io.github.mgrtomaszzurawski.erli.core.model.AttachmentKind;
import io.github.mgrtomaszzurawski.erli.core.model.Market;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.Attachment;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.AttachmentRemoval;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.AttachmentUpdate;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.NewAttachment;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ProductAttachmentResult;
import io.github.mgrtomaszzurawski.erli.internal.JsonCodec;
import io.github.mgrtomaszzurawski.erli.rest.model.AddAttachmentRequest;
import io.github.mgrtomaszzurawski.erli.rest.model.DeleteAttachmentsResponse;
import io.github.mgrtomaszzurawski.erli.rest.model.GetAttachmentsResponseInner;
import io.github.mgrtomaszzurawski.erli.rest.model.ManageAttachedProducts;
import io.github.mgrtomaszzurawski.erli.rest.model.PatchAttachmentRequest;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttachmentMapperTest {

    private static final String AUDIT_EMAIL = "operator@example.com";

    /** Shaped from the live sandbox create response (2026-07-25) plus the spec's optional fields. */
    private static final String ATTACHMENT_JSON = """
            [{"id":73,"shopId":100007,"version":2,"name":"Instrukcja",
              "originalName":"instrukcja.pdf","filePath":"shop/100007/instrukcja.pdf",
              "kind":"userManual","baseUrl":"https://files.erli.pl","type":"application/pdf",
              "attachedProductIds":[11,12],
              "created":{"time":"2026-07-25T10:00:00+02:00","user":{"userId":"u-1","email":"operator@example.com"}},
              "updated":{"time":"2026-07-25T12:30:00+02:00","user":{"userId":"u-2"}}}]""";

    private static final String MINIMAL_ATTACHMENT_JSON = """
            [{"id":74,"shopId":100007,"version":1,"name":"Karta","originalName":"karta.pdf",
              "filePath":"shop/100007/karta.pdf",
              "created":{"time":"2026-07-25T10:00:00+02:00","user":{"userId":"u-1"}}}]""";

    private static GetAttachmentsResponseInner[] decode(String json) {
        return new JsonCodec().read(json, GetAttachmentsResponseInner[].class);
    }

    private static Attachment first(String json) {
        return AttachmentMapper.toDomain(decode(json)[0]);
    }

    @Test
    void mapsEveryFieldOfAnAttachment() {
        Attachment attachment = first(ATTACHMENT_JSON);

        assertEquals(73L, attachment.id());
        assertEquals(100007L, attachment.shopId());
        assertEquals(2L, attachment.version());
        assertEquals("Instrukcja", attachment.name());
        assertEquals("instrukcja.pdf", attachment.originalName());
        assertEquals("shop/100007/instrukcja.pdf", attachment.filePath());
        assertEquals(AttachmentKind.USER_MANUAL, attachment.kind().orElseThrow());
        assertEquals("https://files.erli.pl", attachment.baseUrl().orElseThrow());
        assertEquals("application/pdf", attachment.type().orElseThrow());
        assertEquals(List.of(11L, 12L), attachment.attachedProductIds());
        assertEquals(OffsetDateTime.parse("2026-07-25T10:00:00+02:00"), attachment.created().time());
        assertEquals("u-1", attachment.created().userId());
        assertEquals(AUDIT_EMAIL, attachment.created().email().orElseThrow());
        assertEquals("u-2", attachment.updated().orElseThrow().userId());
    }

    @Test
    void leavesTheOptionalFieldsEmptyWhenTheApiOmitsThem() {
        Attachment attachment = first(MINIMAL_ATTACHMENT_JSON);

        assertTrue(attachment.kind().isEmpty());
        assertTrue(attachment.baseUrl().isEmpty());
        assertTrue(attachment.type().isEmpty());
        assertTrue(attachment.updated().isEmpty());
        assertTrue(attachment.created().email().isEmpty());
        // Live responses omit attachedProductIds entirely although the spec marks it required.
        assertTrue(attachment.attachedProductIds().isEmpty());
    }

    @Test
    void redactsTheAuditEmailFromToString() {
        String rendered = first(ATTACHMENT_JSON).created().toString();

        assertFalse(rendered.contains(AUDIT_EMAIL), rendered);
        assertTrue(rendered.contains("u-1"), rendered);
    }

    @Test
    void rejectsAnAttachmentMissingASpecRequiredField() {
        GetAttachmentsResponseInner[] raw =
                decode("[{\"id\":73,\"shopId\":1,\"version\":1,\"name\":\"x\",\"originalName\":\"x.pdf\"}]");

        IllegalStateException failure =
                assertThrows(IllegalStateException.class, () -> AttachmentMapper.toDomain(raw[0]));

        assertTrue(failure.getMessage().contains("'filePath'"), failure.getMessage());
    }

    @Test
    void rejectsAnAuditEntryMissingItsUser() {
        GetAttachmentsResponseInner[] raw = decode("""
                [{"id":73,"shopId":1,"version":1,"name":"x","originalName":"x.pdf","filePath":"p",
                  "created":{"time":"2026-07-25T10:00:00+02:00"}}]""");

        IllegalStateException failure =
                assertThrows(IllegalStateException.class, () -> AttachmentMapper.toDomain(raw[0]));

        assertTrue(failure.getMessage().contains("user.userId"), failure.getMessage());
    }

    @Test
    void buildsTheCreateRequestWithEveryMarket() {
        AddAttachmentRequest request = AttachmentMapper.toCreateRequest(NewAttachment.builder()
                .kind(AttachmentKind.ENERGY_LABEL)
                .name("Etykieta")
                .originalName("etykieta.pdf")
                .filePath("shop/100007/etykieta.pdf")
                .markets(List.of(Market.POLAND, Market.GERMANY))
                .build());

        assertEquals(AddAttachmentRequest.KindEnum.ENERGY_LABEL, request.getKind());
        assertEquals("Etykieta", request.getName());
        assertEquals(List.of("pl", "de"), request.getMarkets());
    }

    @Test
    void omitsMarketsFromAPatchThatDidNotSetThem() {
        // An empty array is invalid (minItems: 1) and AttachmentUpdate refuses it, so absent is the
        // only way to say "leave the markets alone" — the key must not appear at all.
        PatchAttachmentRequest request =
                AttachmentMapper.toPatchRequest(AttachmentUpdate.builder(73L).name("Nowa").build());

        assertEquals(73, request.getId());
        assertEquals("Nowa", request.getName());
        assertNull(request.getMarkets());
    }

    @Test
    void sendsMarketsOnAPatchThatSetThem() {
        PatchAttachmentRequest request = AttachmentMapper.toPatchRequest(
                AttachmentUpdate.builder(73L).markets(List.of(Market.GERMANY)).build());

        assertEquals(List.of("de"), request.getMarkets());
    }

    @Test
    void rejectsAnExplicitlyEmptyMarketsListOnAPatch() {
        AttachmentUpdate.Builder builder = AttachmentUpdate.builder(73L).markets(List.of());

        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    void buildsTheAttachAndDetachRequestsWithTheMatchingAction() {
        ManageAttachedProducts attach = AttachmentMapper.toAttachRequest(73L, List.of(11L, 12L));
        ManageAttachedProducts detach = AttachmentMapper.toDetachRequest(73L, List.of(11L));

        assertEquals(ManageAttachedProducts.ActionEnum.ATTACH, attach.getAction());
        assertEquals(List.of(11, 12), attach.getProductIds());
        assertEquals(ManageAttachedProducts.ActionEnum.DETACH, detach.getAction());
        assertEquals(73, detach.getAttachmentId());
    }

    @Test
    void rejectsAnEmptyProductListBeforeBuildingTheRequest() {
        List<Long> noProducts = List.of();

        assertThrows(IllegalArgumentException.class, () -> AttachmentMapper.toAttachRequest(73L, noProducts));
    }

    @Test
    void refusesToTruncateAnIdTheApiCannotRepresent() {
        // Layer 1 declares these ids as int; silently wrapping would address a different attachment.
        long tooLarge = Integer.MAX_VALUE + 1L;
        List<Long> products = List.of(1L);

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> AttachmentMapper.toAttachRequest(tooLarge, products));

        assertTrue(failure.getMessage().contains("32-bit"), failure.getMessage());
    }

    @Test
    void refusesToTruncateAProductIdTheApiCannotRepresent() {
        List<Long> tooLarge = List.of(Integer.MAX_VALUE + 1L);

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> AttachmentMapper.toAttachRequest(1L, tooLarge));

        assertTrue(failure.getMessage().contains("product id"), failure.getMessage());
    }

    @Test
    void refusesToTruncateAnAttachmentIdOnPatch() {
        AttachmentUpdate tooLarge = AttachmentUpdate.builder(Integer.MAX_VALUE + 1L).name("x").build();

        assertThrows(IllegalArgumentException.class, () -> AttachmentMapper.toPatchRequest(tooLarge));
    }

    @Test
    void refusesToTruncateAnAttachmentIdOnDelete() {
        List<Long> tooLarge = List.of(Integer.MAX_VALUE + 1L);

        assertThrows(IllegalArgumentException.class, () -> AttachmentMapper.toDeleteRequest(tooLarge));
    }

    @ParameterizedTest
    @EnumSource(Market.class)
    void resolvesEveryMarketFromItsWireValue(Market market) {
        assertEquals(market, Market.fromWire(market.wireValue()));
    }

    @Test
    void reportsThePerProductFailuresTheApiReturnsWithHttp200() {
        // Observed live: attach answers 200 with ok:false when a product id does not exist.
        ManageAttachedProductsResponse raw = new JsonCodec().read("""
                {"ok":false,"updated":[11],
                 "errors":[{"productId":999999999,"error":"NotFoundFailure: product not found"}]}""",
                ManageAttachedProductsResponse.class);

        ProductAttachmentResult result = AttachmentMapper.toProductAttachmentResult(raw);

        assertFalse(result.ok());
        assertFalse(result.isComplete());
        assertEquals(List.of(11L), result.updatedProductIds());
        assertEquals(1, result.errors().size());
        assertEquals(999999999L, result.errors().get(0).productId().orElseThrow());
        assertTrue(result.errors().get(0).error().contains("NotFoundFailure"), result.errors().get(0).error());
    }

    @Test
    void reportsACleanAttachAsComplete() {
        ManageAttachedProductsResponse raw = new JsonCodec()
                .read("{\"ok\":true,\"updated\":[11,12],\"errors\":[]}", ManageAttachedProductsResponse.class);

        ProductAttachmentResult result = AttachmentMapper.toProductAttachmentResult(raw);

        assertTrue(result.isComplete());
        assertEquals(List.of(11L, 12L), result.updatedProductIds());
    }

    @Test
    void treatsAnOmittedOkFlagWithNoErrorsAsComplete() {
        // The spec documents no body at all, so a response that reports only `updated` is plausible.
        ManageAttachedProductsResponse raw =
                new JsonCodec().read("{\"updated\":[11]}", ManageAttachedProductsResponse.class);

        assertTrue(AttachmentMapper.toProductAttachmentResult(raw).isComplete());
    }

    @Test
    void treatsAnOmittedOkFlagWithErrorsAsIncomplete() {
        ManageAttachedProductsResponse raw = new JsonCodec().read(
                "{\"errors\":[{\"productId\":9,\"error\":\"nope\"}]}", ManageAttachedProductsResponse.class);

        assertFalse(AttachmentMapper.toProductAttachmentResult(raw).isComplete());
    }

    @Test
    void reportsARefusedProductWithoutAnIdAsAnAbsentId() {
        ManageAttachedProductsResponse raw = new JsonCodec()
                .read("{\"ok\":false,\"errors\":[{\"error\":\"nope\"}]}", ManageAttachedProductsResponse.class);

        ProductAttachmentResult result = AttachmentMapper.toProductAttachmentResult(raw);

        assertTrue(result.errors().get(0).productId().isEmpty());
    }

    @Test
    void buildsTheDeleteRequestAsABareArrayOfIds() {
        assertEquals(List.of(73, 74), AttachmentMapper.toDeleteRequest(List.of(73L, 74L)));
    }

    @Test
    void rejectsAnEmptyDeleteRequest() {
        List<Long> noAttachments = List.of();

        assertThrows(IllegalArgumentException.class, () -> AttachmentMapper.toDeleteRequest(noAttachments));
    }

    @Test
    void mapsAPartialRemoval() {
        DeleteAttachmentsResponse raw = new JsonCodec().read(
                "{\"removedAttachments\":[73],\"errors\":[\"attachment 74 is in use\"]}",
                DeleteAttachmentsResponse.class);

        AttachmentRemoval removal = AttachmentMapper.toRemoval(raw);

        assertEquals(List.of(73L), removal.removedAttachmentIds());
        assertEquals(1, removal.errors().size());
        assertFalse(removal.isComplete());
    }
}
