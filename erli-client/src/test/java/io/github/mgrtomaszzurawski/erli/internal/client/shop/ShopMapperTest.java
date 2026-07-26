package io.github.mgrtomaszzurawski.erli.internal.client.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.erli.domain.shop.MatchingPolicy;
import io.github.mgrtomaszzurawski.erli.domain.shop.Shop;
import io.github.mgrtomaszzurawski.erli.rest.model.ShopResponse;
import io.github.mgrtomaszzurawski.erli.rest.model.ShopResponseCompany;
import org.junit.jupiter.api.Test;

class ShopMapperTest {

    private static final long SHOP_ID = 100007L;
    private static final String SHOP_NAME = "test-shop";

    @Test
    void mapsAllFieldsIncludingCompany() {
        ShopResponse raw = new ShopResponse()
                .id((int) SHOP_ID)
                .name(SHOP_NAME)
                .active(true)
                .company(new ShopResponseCompany().nip("1234567890").name("ACME Sp. z o.o."))
                .externalMatchingPolicy(ShopResponse.ExternalMatchingPolicyEnum.ENABLED);

        Shop shop = ShopMapper.toDomain(raw);

        assertEquals(SHOP_ID, shop.id());
        assertEquals(SHOP_NAME, shop.name());
        assertTrue(shop.active());
        assertEquals(MatchingPolicy.ENABLED, shop.externalMatchingPolicy());
        assertTrue(shop.company().isPresent());
        assertEquals("1234567890", shop.company().get().nip());
        assertEquals("ACME Sp. z o.o.", shop.company().get().name());
    }

    @Test
    void mapsAbsentCompanyToEmptyOptional() {
        ShopResponse raw = new ShopResponse()
                .id((int) SHOP_ID)
                .name(SHOP_NAME)
                .active(false)
                .externalMatchingPolicy(ShopResponse.ExternalMatchingPolicyEnum.DISABLED);

        Shop shop = ShopMapper.toDomain(raw);

        assertFalse(shop.company().isPresent());
        assertFalse(shop.active());
        assertEquals(MatchingPolicy.DISABLED, shop.externalMatchingPolicy());
    }

    @Test
    void rejectsResponseMissingRequiredId() {
        ShopResponse raw = new ShopResponse()
                .name(SHOP_NAME)
                .active(true)
                .externalMatchingPolicy(ShopResponse.ExternalMatchingPolicyEnum.ENABLED);

        assertThrows(IllegalStateException.class, () -> ShopMapper.toDomain(raw));
    }

    @Test
    void rejectsResponseMissingRequiredActive() {
        ShopResponse raw = new ShopResponse()
                .id((int) SHOP_ID)
                .name(SHOP_NAME)
                .externalMatchingPolicy(ShopResponse.ExternalMatchingPolicyEnum.ENABLED);

        assertThrows(IllegalStateException.class, () -> ShopMapper.toDomain(raw));
    }
}
