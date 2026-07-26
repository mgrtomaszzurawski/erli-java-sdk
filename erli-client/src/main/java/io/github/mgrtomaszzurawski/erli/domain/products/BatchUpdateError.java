package io.github.mgrtomaszzurawski.erli.domain.products;

import io.github.mgrtomaszzurawski.erli.core.model.TraceId;
import java.util.Optional;

/**
 * Why one entry of a batch update was rejected.
 *
 * <p>This is deliberately a value, not an exception: the surrounding call succeeded, and the other
 * entries in the batch may well have been applied. Throwing here would discard those results.
 *
 * @param name          the marketplace's error name
 * @param message       the English message
 * @param polishMessage the Polish message, when supplied
 * @param failureType   the marketplace's failure classification, when supplied
 * @param details       additional detail from the error payload, when supplied
 * @param traceId       the trace id for support, when supplied
 * @param spanId        the span id for support, when supplied
 */
public record BatchUpdateError(
        String name,
        String message,
        Optional<String> polishMessage,
        Optional<String> failureType,
        Optional<String> details,
        Optional<TraceId> traceId,
        Optional<String> spanId) {
}
