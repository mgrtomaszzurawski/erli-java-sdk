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
    static ProductUpdateResult toUpdateResult(ProductUpdateResponse raw) {
        Objects.requireNonNull(raw, "raw ProductUpdateResponse");
        Set<ProductField> known = EnumSet.noneOf(ProductField.class);
        Set<String> unrecognised = new LinkedHashSet<>();
        collectFields(raw.getUpdatedFields(), known, unrecognised);
        return new ProductUpdateResult(known, unrecognised);
    }

    private static void collectFields(Collection<String> wireNames, Set<ProductField> known,
            Set<String> unrecognised) {
        if (wireNames == null) {
            return;
        }
        for (String wireName : wireNames) {
            ProductField field = ProductFieldNames.fromWireName(wireName);
            if (field == null) {
                unrecognised.add(wireName);
            } else {
                known.add(field);
            }
        }
    }

    static BatchUpdateOutcome toBatchOutcome(ProductBatchResponseInner raw) {
        Objects.requireNonNull(raw, "raw ProductBatchResponseInner");
        Set<ProductField> known = EnumSet.noneOf(ProductField.class);
        Set<String> unrecognised = new LinkedHashSet<>();
        Optional<ProductUpdateResult> result = Optional.ofNullable(raw.getResult())
                .map(entry -> {
                    collectFields(entry.getUpdatedFields(), known, unrecognised);
                    return new ProductUpdateResult(known, unrecognised);
                });
        return new BatchUpdateOutcome(
                ProductExternalId.of(require(raw.getExternalId(), "batch entry externalId")),
                require(raw.getStatus(), "batch entry status").intValue(),
                result,
                Optional.ofNullable(raw.getError()).map(ProductResultMapper::toBatchError));
    }

    private static BatchUpdateError toBatchError(ProductBatchResponseInnerError raw) {
        return new BatchUpdateError(
                raw.getName(),
                raw.getMessage(),
                Optional.ofNullable(raw.getPolishMessage()),
                Optional.ofNullable(raw.getFailureType()),
                Optional.ofNullable(raw.getPayload()).map(payload -> payload.getDetails()),
                Optional.ofNullable(raw.getTraceId()).map(TraceId::of),
                Optional.ofNullable(raw.getSpanId()));
    }

    static CreateDiscount toCreateDiscount(DiscountRequest request) {
        CreateDiscount raw = new CreateDiscount();
        raw.setNewPrice(ProductValues.toMinorUnits(request.newPrice()));
        raw.setStartAt(request.startAt());
        raw.setRestoreAt(request.restoreAt());
        raw.setUnfreezeAfterwards(request.unfreezeAfterwards());
        return raw;
    }

    static Discount toDiscount(io.github.mgrtomaszzurawski.erli.rest.model.Discount raw) {
        Objects.requireNonNull(raw, "raw Discount");
        return new Discount(
                ProductExternalId.of(require(raw.getExternalId(), "discount externalId")),
                require(raw.getShopId(), "discount shopId").longValue(),
                ProductValues.toMoney(require(raw.getNewPrice(), "discount newPrice")),
                require(raw.getStartAt(), "discount startAt"),
                require(raw.getRestoreAt(), "discount restoreAt"),
                Boolean.TRUE.equals(raw.getUnfreezeAfterwards()));
    }

    private static <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalStateException("Erli response is missing the required '" + field + "' field");
        }
        return value;
    }
}
