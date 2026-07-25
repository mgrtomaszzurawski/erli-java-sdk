package io.github.mgrtomaszzurawski.erli.domain.products;

import java.util.List;
import java.util.Optional;

/**
 * A document attached to a product (manual, safety sheet, energy label, …), optionally scoped to a
 * subset of markets.
 *
 * <p>The market scope is split in two. {@link #markets()} holds the markets this SDK version can name;
 * {@link #unrecognisedMarkets()} holds the raw wire values of any it cannot. Failing the whole product
 * read over one unfamiliar market code would be worse than useless — and on a search it would fail the
 * whole page — but silently dropping the entry would quietly narrow the attachment's scope. Keeping the
 * raw value does neither: the true scope is the two lists together, and a read-modify-write puts back
 * exactly what it received.
 *
 * <p>There is deliberately no shorter constructor that omits {@code unrecognisedMarkets}: it would be a
 * one-line way to drop precisely what this split exists to keep. Building an attachment from scratch
 * passes {@link java.util.List#of()}; copying one that was read carries its list through.
 *
 * <p>Order is not significant — a market scope is a set — and the write path emits the named markets
 * first, then the unrecognised ones.
 *
 * @param id                  the attachment id in the shop's dictionary, when supplied
 * @param kind                what kind of document it is
 * @param url                 the document URL, when supplied
 * @param markets             the markets the attachment applies to, as far as this SDK can name them;
 *                            both lists empty means all markets (defensively copied)
 * @param unrecognisedMarkets wire values for markets this SDK version does not know, preserved verbatim
 *                            so they survive a round trip (defensively copied)
 */
public record ProductAttachment(
        Optional<Integer> id,
        Optional<AttachmentKind> kind,
        Optional<String> url,
        List<Market> markets,
        List<String> unrecognisedMarkets) {

    public ProductAttachment {
        markets = List.copyOf(markets);
        unrecognisedMarkets = List.copyOf(unrecognisedMarkets);
    }

    /** How many markets the attachment is scoped to, known and unknown together. */
    public int marketCount() {
        return markets.size() + unrecognisedMarkets.size();
    }
}
