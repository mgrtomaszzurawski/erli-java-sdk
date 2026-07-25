package io.github.mgrtomaszzurawski.erli.internal.client.delivery;

import io.github.mgrtomaszzurawski.erli.core.model.DeliveryMethodId;
import io.github.mgrtomaszzurawski.erli.core.model.Money;
import io.github.mgrtomaszzurawski.erli.domain.delivery.DeliveryMethodRef;
import io.github.mgrtomaszzurawski.erli.domain.delivery.DeliveryPrice;
import io.github.mgrtomaszzurawski.erli.domain.delivery.DeliveryTime;
import io.github.mgrtomaszzurawski.erli.domain.delivery.DeliveryTimeUnit;
import io.github.mgrtomaszzurawski.erli.domain.delivery.PackingLimit;
import io.github.mgrtomaszzurawski.erli.domain.delivery.ParcelSize;
import io.github.mgrtomaszzurawski.erli.domain.delivery.PriceList;
import io.github.mgrtomaszzurawski.erli.domain.delivery.PriceListSummary;
import io.github.mgrtomaszzurawski.erli.domain.delivery.SizeLimit;
import io.github.mgrtomaszzurawski.erli.rest.model.CreatePriceListSchemaPricesInner;
import io.github.mgrtomaszzurawski.erli.rest.model.CreatePriceListSchemaPricesInnerDeliveryMethod;
import io.github.mgrtomaszzurawski.erli.rest.model.CreatePriceListSchemaPricesInnerDeliveryMethodDeliveryTime;
import io.github.mgrtomaszzurawski.erli.rest.model.PriceListDetailsSchema;
import io.github.mgrtomaszzurawski.erli.rest.model.PriceListListItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Maps the generated price-list models onto the public {@code domain.delivery} records. Internal:
 * never exported, so no {@code *Raw} type reaches an exported signature.
 */
final class PriceListMapper {

    /** Erli states every delivery amount in grosze, the minor unit of the marketplace currency. */
    private static final String CURRENCY_CODE = "PLN";
    private static final String DIMENSION_KEY = "dimension";
    private static final String LIMIT_KEY = "limit";

    private PriceListMapper() {
    }

    static PriceListSummary toSummary(PriceListListItem rawItem) {
        Objects.requireNonNull(rawItem, "raw PriceListListItem");
        return new PriceListSummary(
                requireField(rawItem.getId(), "id").longValue(), requireField(rawItem.getName(), "name"));
    }

    static PriceList toDomain(PriceListDetailsSchema rawPriceList) {
        Objects.requireNonNull(rawPriceList, "raw PriceListDetailsSchema");
        return new PriceList(
                requireField(rawPriceList.getId(), "id").longValue(),
                requireField(rawPriceList.getName(), "name"),
                toPrices(requireField(rawPriceList.getPrices(), "prices")),
                Boolean.TRUE.equals(rawPriceList.getErliProEnabled()),
                Boolean.TRUE.equals(rawPriceList.getNextDayDeliveryEnabled()),
                requireField(rawPriceList.getCreatedAt(), "createdAt"),
                Optional.ofNullable(rawPriceList.getUpdatedAt()));
    }

    private static <T> T requireField(T value, String fieldName) {
        if (value == null) {
            throw new IllegalStateException("Price list is missing the required '" + fieldName + "' field");
        }
        return value;
    }

    private static List<DeliveryPrice> toPrices(List<CreatePriceListSchemaPricesInner> rawPrices) {
        List<DeliveryPrice> prices = new ArrayList<>(rawPrices.size());
        for (int index = 0; index < rawPrices.size(); index++) {
            CreatePriceListSchemaPricesInner rawPrice = rawPrices.get(index);
            if (rawPrice == null) {
                throw new IllegalStateException("Price list field 'prices' has a null element at index " + index);
            }
            prices.add(toPrice(rawPrice));
        }
        return List.copyOf(prices);
    }

    private static DeliveryPrice toPrice(CreatePriceListSchemaPricesInner rawPrice) {
        return new DeliveryPrice(
                toMethodRef(requireField(rawPrice.getDeliveryMethod(), "prices[].deliveryMethod")),
                toMoney(requireField(rawPrice.getBasePrice(), "prices[].basePrice")),
                toMoney(requireField(rawPrice.getNextItemPrice(), "prices[].nextItemPrice")),
                toPackingLimit(rawPrice.getLimit()),
                Boolean.TRUE.equals(rawPrice.getNextDayDeliveryOption()));
    }

    private static Money toMoney(Integer grosze) {
        return Money.ofMinorUnits(grosze.longValue(), CURRENCY_CODE);
    }

    private static DeliveryMethodRef toMethodRef(CreatePriceListSchemaPricesInnerDeliveryMethod rawMethod) {
        return new DeliveryMethodRef(
                DeliveryMethodId.of(
                        requireField(rawMethod.getId(), "prices[].deliveryMethod.id").getValue()),
                Optional.ofNullable(rawMethod.getDeliveryTime()).map(PriceListMapper::toDeliveryTime));
    }

    private static DeliveryTime toDeliveryTime(
            CreatePriceListSchemaPricesInnerDeliveryMethodDeliveryTime rawTime) {
        return new DeliveryTime(
                DeliveryTimeUnit.fromWire(requireField(rawTime.getUnit(), "deliveryTime.unit").getValue()),
                requireField(rawTime.getMinPeriod(), "deliveryTime.minPeriod"),
                requireField(rawTime.getMaxPeriod(), "deliveryTime.maxPeriod"));
    }

    /**
     * Re-type the {@code limit} branch the generator could not express.
     *
     * <p>The spec states it as a {@code oneOf}: a plain count, or a three-row table keyed by InPost
     * locker size. The build-time spec normalisation collapses that to a free-form object, so Layer 1
     * hands over an untyped value and the domain restores the distinction (see ADR-001).
     */
    private static Optional<PackingLimit> toPackingLimit(Object rawLimit) {
        if (rawLimit == null) {
            return Optional.empty();
        }
        if (rawLimit instanceof Number totalItems) {
            return Optional.of(new PackingLimit.Total(totalItems.intValue()));
        }
        if (rawLimit instanceof List<?> rawRows) {
            List<SizeLimit> limits = new ArrayList<>(rawRows.size());
            for (Object rawRow : rawRows) {
                limits.add(toSizeLimit(rawRow));
            }
            return Optional.of(new PackingLimit.PerSize(limits));
        }
        throw new IllegalStateException(
                "Price list field 'prices[].limit' decoded to an unexpected shape: " + rawLimit.getClass().getName());
    }

    private static SizeLimit toSizeLimit(Object rawRow) {
        if (!(rawRow instanceof Map<?, ?> row)) {
            throw new IllegalStateException("Price list field 'prices[].limit' has a non-object row: " + rawRow);
        }
        Object dimension = row.get(DIMENSION_KEY);
        Object limit = row.get(LIMIT_KEY);
        if (!(dimension instanceof String dimensionText) || !(limit instanceof Number limitValue)) {
            throw new IllegalStateException(
                    "Price list field 'prices[].limit' row is missing 'dimension' or 'limit': " + row);
        }
        return new SizeLimit(ParcelSize.fromWire(dimensionText), limitValue.intValue());
    }
}
