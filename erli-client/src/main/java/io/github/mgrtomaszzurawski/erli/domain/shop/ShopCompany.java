package io.github.mgrtomaszzurawski.erli.domain.shop;

/**
 * The legal company behind a shop, as returned by {@code GET /me}. Optional on the shop; when present
 * either field may still be absent.
 *
 * @param nip  the Polish tax identifier (NIP), or {@code null}
 * @param name the registered company name, or {@code null}
 */
public record ShopCompany(String nip, String name) {
}
