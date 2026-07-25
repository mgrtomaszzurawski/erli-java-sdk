package io.github.mgrtomaszzurawski.erli.domain.shop;

import java.util.Optional;

/**
 * The authenticated shop returned by {@code GET /me} — the identity behind the API key.
 *
 * @param id                     the shop's numeric id
 * @param name                   the shop's slug-like name
 * @param active                 whether the shop is active on the marketplace
 * @param company                the legal company, if the shop has one attached
 * @param externalMatchingPolicy the external catalog-matching policy
 */
public record Shop(
        long id,
        String name,
        boolean active,
        Optional<ShopCompany> company,
        MatchingPolicy externalMatchingPolicy) {
}
