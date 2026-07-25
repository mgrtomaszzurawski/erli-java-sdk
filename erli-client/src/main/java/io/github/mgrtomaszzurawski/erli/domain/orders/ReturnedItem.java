package io.github.mgrtomaszzurawski.erli.domain.orders;

/**
 * One line of a {@link OrderReturn}: how much of which order line came back.
 *
 * <p>{@link #index()} is a position in {@link Order#items()}, not an item id — resolve it against that
 * list to learn which product was returned.
 *
 * @param index    the zero-based position in {@link Order#items()} that was returned
 * @param quantity how many units of that line came back
 */
public record ReturnedItem(int index, int quantity) {
}
