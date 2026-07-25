package io.github.mgrtomaszzurawski.erli.domain.products;

import java.util.Optional;

/**
 * A pointer to the same product on another marketplace or comparison site — used by the marketplace for
 * price comparison and catalog matching.
 *
 * @param id     the product's identifier on that site, when supplied
 * @param kind   which site the reference points at
 * @param url    the product URL on that site, when supplied
 * @param source the channel that produced the reference
 */
public record ExternalReference(
        Optional<String> id,
        Optional<ExternalReferenceKind> kind,
        Optional<String> url,
        Optional<ExternalReferenceSource> source) {
}
