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

    static Shop toDomain(ShopResponse raw) {
        Objects.requireNonNull(raw, "raw ShopResponse");
        return new Shop(
                requireId(raw),
                raw.getName(),
                Boolean.TRUE.equals(raw.getActive()),
                Optional.ofNullable(raw.getCompany()).map(ShopMapper::toCompany),
                toMatchingPolicy(raw));
    }

    private static long requireId(ShopResponse raw) {
        Integer id = raw.getId();
        if (id == null) {
            throw new IllegalStateException("ShopResponse is missing the required 'id' field");
        }
        return id.longValue();
    }

    private static ShopCompany toCompany(ShopResponseCompany company) {
        return new ShopCompany(company.getNip(), company.getName());
    }

    private static MatchingPolicy toMatchingPolicy(ShopResponse raw) {
        ShopResponse.ExternalMatchingPolicyEnum policy = raw.getExternalMatchingPolicy();
        if (policy == null) {
            throw new IllegalStateException(
                    "ShopResponse is missing the required 'externalMatchingPolicy' field");
        }
        return MatchingPolicy.valueOf(policy.name());
    }
}
