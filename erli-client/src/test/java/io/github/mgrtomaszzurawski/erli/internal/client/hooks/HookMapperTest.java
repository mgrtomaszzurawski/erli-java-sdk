package io.github.mgrtomaszzurawski.erli.internal.client.hooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.erli.core.error.ErliTransportException;
import io.github.mgrtomaszzurawski.erli.domain.hooks.Hook;
import io.github.mgrtomaszzurawski.erli.domain.hooks.HookKind;
import io.github.mgrtomaszzurawski.erli.rest.model.HookResponseInner;
import io.github.mgrtomaszzurawski.erli.rest.model.HookSave;
import java.net.URI;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class HookMapperTest {

    private static final String HOOK_URL = "https://shop.example/check-buyability";
    private static final String ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiIs";

    @Test
    void mapsEveryHookNameInBothDirections() {
        // A round trip over the whole enum: a name this SDK knows must survive read -> write -> read.
        Stream.of(HookKind.values()).forEach(kind -> {
            HookSave written = HookMapper.toRaw(Hook.of(kind, URI.create(HOOK_URL)));
            assertEquals(kind.wireValue(), written.getHookName().getValue());
            HookResponseInner read = new HookResponseInner()
                    .hookName(HookResponseInner.HookNameEnum.fromValue(kind.wireValue()))
                    .url(HOOK_URL);
            assertEquals(kind, HookMapper.toDomain(read).kind());
        });
    }

    @Test
    void mapsTheAccessTokenWhenPresentAndLeavesItUnsetOtherwise() {
        assertEquals(ACCESS_TOKEN,
                HookMapper.toRaw(Hook.of(HookKind.ORDER_CREATED, URI.create(HOOK_URL), ACCESS_TOKEN))
                        .getAccessToken());
        assertNull(HookMapper.toRaw(Hook.of(HookKind.ORDER_CREATED, URI.create(HOOK_URL))).getAccessToken());
        assertTrue(HookMapper.toDomain(new HookResponseInner()
                .hookName(HookResponseInner.HookNameEnum.ORDER_CREATED)
                .url(HOOK_URL)).accessToken().isEmpty());
    }

    /**
     * The API does not require {@code https} — a shop may already have registered an {@code http}
     * endpoint through the panel. Reading it back must work, or {@code list()} would fail wholesale
     * exactly when the shop needs to see the insecure subscription in order to replace it.
     */
    @Test
    void readsBackAnInsecureSubscriptionTheApiAlreadyHolds() {
        HookResponseInner storedOverHttp = new HookResponseInner()
                .hookName(HookResponseInner.HookNameEnum.ORDER_CREATED)
                .url("http://legacy.example/order-created");

        Hook hook = HookMapper.toDomain(storedOverHttp);

        assertEquals(URI.create("http://legacy.example/order-created"), hook.url());
    }

    /** …but the same subscription must not be registrable, which is what {@code save} checks. */
    @Test
    void refusesToRegisterAnInsecureSubscription() {
        Hook insecure = HookMapper.toDomain(new HookResponseInner()
                .hookName(HookResponseInner.HookNameEnum.ORDER_CREATED)
                .url("http://legacy.example/order-created"));

        assertThrows(IllegalArgumentException.class, insecure::requireRegisterable);
    }

    /**
     * A stored subscription the SDK cannot represent must surface inside the documented exception
     * taxonomy, not as a bare {@link IllegalArgumentException} escaping {@code list()}.
     */
    @Test
    void translatesAnUnusableStoredSubscriptionIntoTheSdkTaxonomy() {
        HookResponseInner overlongUrl = new HookResponseInner()
                .hookName(HookResponseInner.HookNameEnum.ORDER_CREATED)
                .url("https://shop.example/" + "x".repeat(Hook.MAX_URL_LENGTH));

        ErliTransportException thrown =
                assertThrows(ErliTransportException.class, () -> HookMapper.toDomain(overlongUrl));

        assertTrue(thrown.getMessage().contains("orderCreated"), thrown.getMessage());
        assertInstanceOf(IllegalArgumentException.class, thrown.getCause());
    }

    @Test
    void rejectsAHookMissingItsUrl() {
        HookResponseInner withoutUrl = new HookResponseInner()
                .hookName(HookResponseInner.HookNameEnum.ORDER_CREATED);

        IllegalStateException thrown =
                assertThrows(IllegalStateException.class, () -> HookMapper.toDomain(withoutUrl));

        assertTrue(thrown.getMessage().contains("url"), thrown.getMessage());
    }

    @Test
    void rejectsAHookMissingItsName() {
        HookResponseInner withoutName = new HookResponseInner().url(HOOK_URL);

        IllegalStateException thrown =
                assertThrows(IllegalStateException.class, () -> HookMapper.toDomain(withoutName));

        assertTrue(thrown.getMessage().contains("hookName"), thrown.getMessage());
    }
}
