package io.github.mgrtomaszzurawski.erli.internal.client.hooks;

import io.github.mgrtomaszzurawski.erli.domain.hooks.Hook;
import io.github.mgrtomaszzurawski.erli.domain.hooks.HookKind;
import io.github.mgrtomaszzurawski.erli.rest.model.HookResponseInner;
import io.github.mgrtomaszzurawski.erli.rest.model.HookSave;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
