package io.github.mgrtomaszzurawski.erli.internal.client.shop;

import io.github.mgrtomaszzurawski.erli.domain.shop.MatchingPolicy;
import io.github.mgrtomaszzurawski.erli.domain.shop.Shop;
import io.github.mgrtomaszzurawski.erli.domain.shop.ShopCompany;
import io.github.mgrtomaszzurawski.erli.rest.model.ShopResponse;
import io.github.mgrtomaszzurawski.erli.rest.model.ShopResponseCompany;
import java.util.Objects;
import java.util.Optional;

/**
 * Maps the generated Layer-1 {@link ShopResponse} to the public {@link Shop} domain record. Kept in
 * an internal package so the {@code *Raw} type never appears in an exported signature. Internal.
 */
final class ShopMapper {

    private ShopMapper() {
    }

    static Shop toDomain(ShopResponse rawShop) {
        Objects.requireNonNull(rawShop, "raw ShopResponse");
        return new Shop(
                requireId(rawShop),
                rawShop.getName(),
                requireActive(rawShop),
                Optional.ofNullable(rawShop.getCompany()).map(ShopMapper::toCompany),
                toMatchingPolicy(rawShop));
    }

    private static long requireId(ShopResponse rawShop) {
        Integer shopId = rawShop.getId();
        if (shopId == null) {
            throw new IllegalStateException("ShopResponse is missing the required 'id' field");
        }
        return shopId.longValue();
    }

    private static boolean requireActive(ShopResponse rawShop) {
        Boolean active = rawShop.getActive();
        if (active == null) {
            throw new IllegalStateException("ShopResponse is missing the required 'active' field");
        }
        return active;
    }

    private static ShopCompany toCompany(ShopResponseCompany company) {
        return new ShopCompany(company.getNip(), company.getName());
    }

    private static MatchingPolicy toMatchingPolicy(ShopResponse rawShop) {
        ShopResponse.ExternalMatchingPolicyEnum policy = rawShop.getExternalMatchingPolicy();
        if (policy == null) {
            throw new IllegalStateException(
                    "ShopResponse is missing the required 'externalMatchingPolicy' field");
        }
        // Explicit mapping (not valueOf(name())): decouples our enum from the generated constant
        // names, and the exhaustive switch makes a future upstream enum value a compile error here
        // rather than a runtime surprise — a deliberate signal to re-map when the spec grows.
        return switch (policy) {
            case ENABLED -> MatchingPolicy.ENABLED;
            case DISABLED -> MatchingPolicy.DISABLED;
        };
    }
}
