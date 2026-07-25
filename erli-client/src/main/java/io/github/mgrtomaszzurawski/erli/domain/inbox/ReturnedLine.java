package io.github.mgrtomaszzurawski.erli.domain.inbox;

/**
 * One returned position, identified by its place in the order's line list.
 *
 * @param lineIndex zero-based position in {@link OrderEvent#lines()}
 * @param quantity  how many units of that line came back
 */
public record ReturnedLine(int lineIndex, int quantity) {

    public ReturnedLine {
        if (lineIndex < 0) {
            throw new IllegalArgumentException("lineIndex must not be negative");
        }
    }
}
