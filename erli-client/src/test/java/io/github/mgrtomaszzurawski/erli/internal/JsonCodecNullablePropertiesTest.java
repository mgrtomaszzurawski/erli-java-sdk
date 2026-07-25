package io.github.mgrtomaszzurawski.erli.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.erli.rest.model.CheckBuyabilityResponseInner;
import io.github.mgrtomaszzurawski.erli.rest.model.HookSave;
import org.junit.jupiter.api.Test;

/**
 * Regression guard for two codec behaviours the generated Layer-1 models depend on and that a bare
 * {@code ObjectMapper} does not provide. Both were found by the Comms bucket, which is the first to
 * decode a {@code nullable: true} property and to send a write body with an unset optional field; the
 * spec declares 100 nullable properties, most of them on the product schemas.
 *
 * <p>Kept separate from the core-owned {@code JsonCodecTest} so the two suites do not collide.
 */
class JsonCodecNullablePropertiesTest {

    private static final String STATUS_PRESENT = "{\"productId\":\"555\",\"status\":\"active\",\"stock\":100}";
    private static final String STATUS_EXPLICIT_NULL = "{\"productId\":\"555\",\"status\":null}";
    private static final String STATUS_ABSENT = "{\"productId\":\"555\"}";
    private static final String HOOK_URL = "https://shop.example/hook";

    private static final int EXPECTED_STOCK = 100;

    private final JsonCodec codec = new JsonCodec();

    /** Without {@code JsonNullableModule} this throws: the property cannot be decoded at all. */
    @Test
    void decodesANullablePropertyThatCarriesAValue() {
        CheckBuyabilityResponseInner decoded = codec.read(STATUS_PRESENT, CheckBuyabilityResponseInner.class);

        assertEquals(CheckBuyabilityResponseInner.StatusEnum.ACTIVE, decoded.getStatus());
        assertEquals(EXPECTED_STOCK, decoded.getStock().intValue());
    }

    /** An explicit JSON null is a stated "unknown", distinct from an absent field. */
    @Test
    void decodesAnExplicitNullAndAnAbsentPropertyWithoutThrowing() {
        CheckBuyabilityResponseInner explicitNull =
                codec.read(STATUS_EXPLICIT_NULL, CheckBuyabilityResponseInner.class);
        CheckBuyabilityResponseInner absent = codec.read(STATUS_ABSENT, CheckBuyabilityResponseInner.class);

        assertTrue(explicitNull.getStatus_JsonNullable().isPresent(),
                "an explicit null must decode as present-but-null, not as absent");
        assertNull(explicitNull.getStatus());
        assertFalse(absent.getStatus_JsonNullable().isPresent(),
                "an absent property must stay absent, distinct from an explicit null");
        assertNull(absent.getStatus());
    }

    /**
     * The API's request schemas are {@code additionalProperties: false} and treat a null as a value to
     * store, so an unset optional field must be omitted from the body rather than sent as null.
     */
    @Test
    void omitsUnsetOptionalFieldsFromWriteBodies() {
        String body = codec.write(new HookSave()
                .hookName(HookSave.HookNameEnum.CHECK_BUYABILITY)
                .url(HOOK_URL));

        assertEquals("{\"hookName\":\"checkBuyability\",\"url\":\"" + HOOK_URL + "\"}", body);
    }
}
