package io.github.mgrtomaszzurawski.erli.domain.products;

import java.util.List;
import java.util.Optional;

/**
 * A document attached to a product (manual, safety sheet, energy label, …), optionally scoped to a
 * subset of markets.
 *
 * @param id      the attachment id in the shop's dictionary, when supplied
 * @param kind    what kind of document it is
 * @param url     the document URL, when supplied
 * @param markets the markets the attachment applies to; empty means all (defensively copied)
 */
public record ProductAttachment(
        Optional<Integer> id,
        Optional<AttachmentKind> kind,
        Optional<String> url,
        List<Market> markets) {

    public ProductAttachment {
        markets = List.copyOf(markets);
    }
}
