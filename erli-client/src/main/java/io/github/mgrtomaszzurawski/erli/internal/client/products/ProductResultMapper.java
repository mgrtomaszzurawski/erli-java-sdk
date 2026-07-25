package io.github.mgrtomaszzurawski.erli.internal.client.products;

import io.github.mgrtomaszzurawski.erli.core.model.ProductExternalId;
import io.github.mgrtomaszzurawski.erli.core.model.TraceId;
import io.github.mgrtomaszzurawski.erli.domain.products.BatchUpdateError;
import io.github.mgrtomaszzurawski.erli.domain.products.BatchUpdateOutcome;
import io.github.mgrtomaszzurawski.erli.domain.products.Discount;
import io.github.mgrtomaszzurawski.erli.domain.products.DiscountRequest;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductField;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductUpdateResult;
import io.github.mgrtomaszzurawski.erli.rest.model.CreateDiscount;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductBatchResponseInner;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductBatchResponseInnerError;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductUpdateResponse;

import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Maps the smaller products payloads: update results, batch outcomes and discounts.
 * Internal: never exported.
 */
final class ProductResultMapper {

    private ProductResultMapper() {
    }

    /**
     * The {@code updatedFields} list, split into fields this SDK knows and wire names it does not.
     * An unknown name is kept rather than dropped or thrown on: the API may add fields at any time, and
     * a successful update must not become a failure because the response mentioned a newer one.
     */
    static ProductUpdateResult toUpdateResult(ProductUpdateResponse rawResult) {
        Objects.requireNonNull(rawResult, "ProductUpdateResponse");
        Set<ProductField> known = EnumSet.noneOf(ProductField.class);
        Set<String> unrecognised = new LinkedHashSet<>();
        collectFields(rawResult.getUpdatedFields(), known, unrecognised);
        return new ProductUpdateResult(known, unrecognised);
    }

    private static void collectFields(Collection<String> wireNames, Set<ProductField> known,
            Set<String> unrecognised) {
        if (wireNames == null) {
            return;
        }
        for (String wireName : wireNames) {
            ProductField field = ProductEnums.toProductFieldOrNull(wireName);
            if (field == null) {
                unrecognised.add(wireName);
            } else {
                known.add(field);
            }
        }
    }

    static BatchUpdateOutcome toBatchOutcome(ProductBatchResponseInner rawResult) {
        Objects.requireNonNull(rawResult, "ProductBatchResponseInner");
        Set<ProductField> known = EnumSet.noneOf(ProductField.class);
        Set<String> unrecognised = new LinkedHashSet<>();
        Optional<ProductUpdateResult> result = Optional.ofNullable(rawResult.getResult())
                .map(entry -> {
                    collectFields(entry.getUpdatedFields(), known, unrecognised);
                    return new ProductUpdateResult(known, unrecognised);
                });
        return new BatchUpdateOutcome(
                ProductExternalId.of(require(rawResult.getExternalId(), "batch entry externalId")),
                require(rawResult.getStatus(), "batch entry status").intValue(),
                result,
                Optional.ofNullable(rawResult.getError()).map(ProductResultMapper::toBatchError));
    }

    private static BatchUpdateError toBatchError(ProductBatchResponseInnerError rawResult) {
        return new BatchUpdateError(
                rawResult.getName(),
                rawResult.getMessage(),
                Optional.ofNullable(rawResult.getPolishMessage()),
                Optional.ofNullable(rawResult.getFailureType()),
                Optional.ofNullable(rawResult.getPayload()).map(payload -> payload.getDetails()),
                Optional.ofNullable(rawResult.getTraceId()).map(TraceId::of),
                Optional.ofNullable(rawResult.getSpanId()));
    }

    static CreateDiscount toCreateDiscount(DiscountRequest request) {
        CreateDiscount rawResult = new CreateDiscount();
        rawResult.setNewPrice(ProductValues.toMinorUnits(request.newPrice()));
        rawResult.setStartAt(request.startAt());
        rawResult.setRestoreAt(request.restoreAt());
        rawResult.setUnfreezeAfterwards(request.unfreezeAfterwards());
        return rawResult;
    }

    static Discount toDiscount(io.github.mgrtomaszzurawski.erli.rest.model.Discount rawResult) {
        Objects.requireNonNull(rawResult, "Discount");
        return new Discount(
                ProductExternalId.of(require(rawResult.getExternalId(), "discount externalId")),
                require(rawResult.getShopId(), "discount shopId").longValue(),
                ProductValues.toMoney(require(rawResult.getNewPrice(), "discount newPrice")),
                require(rawResult.getStartAt(), "discount startAt"),
                require(rawResult.getRestoreAt(), "discount restoreAt"),
                Boolean.TRUE.equals(rawResult.getUnfreezeAfterwards()));
    }

    private static <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalStateException("Erli response is missing the required '" + field + "' field");
        }
        return value;
    }
}
