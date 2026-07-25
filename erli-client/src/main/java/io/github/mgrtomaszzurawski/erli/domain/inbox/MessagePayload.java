package io.github.mgrtomaszzurawski.erli.domain.inbox;

/**
 * The body of an inbox message. Which implementation arrives is decided by {@link Message#type()}:
 * the three {@code ORDER_*} types carry an {@link OrderEvent}, {@code PRODUCTS_NEED_SYNC} carries a
 * {@link ProductsSyncEvent}.
 *
 * <p>Sealed, so these two implementations are the whole domain and a consumer can branch over them
 * without a default case:
 * <pre>{@code
 * if (payload instanceof OrderEvent order) {
 *     handle(order);
 * } else if (payload instanceof ProductsSyncEvent sync) {
 *     resync(sync.productIds());
 * }
 * }</pre>
 * On Java 21 and later the same branch reads as an exhaustive {@code switch} pattern; the SDK itself
 * compiles against Java 17, where that is still a preview feature.
 */
public sealed interface MessagePayload permits OrderEvent, ProductsSyncEvent {
}
