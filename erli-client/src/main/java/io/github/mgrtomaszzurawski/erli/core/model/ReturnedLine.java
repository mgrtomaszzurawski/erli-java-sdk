package io.github.mgrtomaszzurawski.erli.core.model;

/**
 * One returned position within an order return: which order line it refers to and how many units of
 * it came back.
 *
 * @param lineIndex the zero-based index of the order line being returned
 * @param quantity  how many units of that line were returned
 */
public record ReturnedLine(int lineIndex, int quantity) {

    public ReturnedLine {
        if (lineIndex < 0) {
            throw new IllegalArgumentException("lineIndex must not be negative");
        }
    }
}
