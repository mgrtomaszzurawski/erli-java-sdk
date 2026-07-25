package io.github.mgrtomaszzurawski.erli.internal.client.delivery;

import io.github.mgrtomaszzurawski.erli.core.model.Money;
import io.github.mgrtomaszzurawski.erli.domain.delivery.DeliveryMethodRef;
import io.github.mgrtomaszzurawski.erli.domain.delivery.DeliveryPrice;
import io.github.mgrtomaszzurawski.erli.domain.delivery.DeliveryTime;
import io.github.mgrtomaszzurawski.erli.domain.delivery.PackingLimit;
import io.github.mgrtomaszzurawski.erli.domain.delivery.PriceListDraft;
import io.github.mgrtomaszzurawski.erli.domain.delivery.PriceListUpdate;
import io.github.mgrtomaszzurawski.erli.domain.delivery.SizeLimit;
import io.github.mgrtomaszzurawski.erli.rest.model.CreatePriceListSchema;
import io.github.mgrtomaszzurawski.erli.rest.model.CreatePriceListSchemaPricesInner;
import io.github.mgrtomaszzurawski.erli.rest.model.CreatePriceListSchemaPricesInnerDeliveryMethod;
import io.github.mgrtomaszzurawski.erli.rest.model.CreatePriceListSchemaPricesInnerDeliveryMethodDeliveryTime;
import io.github.mgrtomaszzurawski.erli.rest.model.UpdatePriceListSchema;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps the public price-list request records onto the generated request models. The reverse direction
 * of {@link PriceListMapper}; kept internal for the same reason.
 */
final class PriceListRequestMapper {

    private static final String DIMENSION_KEY = "dimension";
    private static final String LIMIT_KEY = "limit";
    /** Grosze per złoty — the scale that turns a {@link Money} back into the integer the API wants. */
    private static final BigDecimal MINOR_UNITS_PER_MAJOR = new BigDecimal("100");

    private PriceListRequestMapper() {
    }

    static CreatePriceListSchema toRaw(PriceListDraft draft) {
        CreatePriceListSchema raw = new CreatePriceListSchema();
        raw.setName(draft.name());
        raw.setPrices(toRawPrices(draft.prices()));
        raw.setErliProEnabled(draft.erliProEnabled());
        raw.setNextDayDeliveryEnabled(draft.nextDayDeliveryEnabled());
        return raw;
    }

    static UpdatePriceListSchema toRaw(PriceListUpdate update) {
        UpdatePriceListSchema raw = new UpdatePriceListSchema();
        raw.setPrices(toRawPrices(update.prices()));
        raw.setErliProEnabled(update.erliProEnabled());
        raw.setNextDayDeliveryEnabled(update.nextDayDeliveryEnabled());
        return raw;
    }

    private static List<CreatePriceListSchemaPricesInner> toRawPrices(List<DeliveryPrice> prices) {
        List<CreatePriceListSchemaPricesInner> rawPrices = new ArrayList<>(prices.size());
        for (DeliveryPrice price : prices) {
            rawPrices.add(toRawPrice(price));
        }
        return rawPrices;
    }

    private static CreatePriceListSchemaPricesInner toRawPrice(DeliveryPrice price) {
        CreatePriceListSchemaPricesInner raw = new CreatePriceListSchemaPricesInner();
        raw.setDeliveryMethod(toRawMethod(price.deliveryMethod()));
        raw.setBasePrice(toMinorUnits(price.basePrice(), "basePrice"));
        raw.setNextItemPrice(toMinorUnits(price.nextItemPrice(), "nextItemPrice"));
        price.limit().ifPresent(limit -> raw.setLimit(toRawLimit(limit)));
        raw.setNextDayDeliveryOption(price.nextDayDeliveryOption());
        return raw;
    }

    private static CreatePriceListSchemaPricesInnerDeliveryMethod toRawMethod(DeliveryMethodRef method) {
        CreatePriceListSchemaPricesInnerDeliveryMethod raw =
                new CreatePriceListSchemaPricesInnerDeliveryMethod();
        raw.setId(toRawMethodId(method));
        method.deliveryTime().ifPresent(deliveryTime -> raw.setDeliveryTime(toRawDeliveryTime(deliveryTime)));
        return raw;
    }

    /**
     * The generated id is a closed enum, so a method id the vendored spec does not know is rejected
     * here rather than sent and refused by the server. An {@link IllegalArgumentException} rather than
     * an {@code Erli*} exception on purpose: no request was made, this is the caller's input being
     * wrong, and the message names the value and where to look up a valid one.
     */
    private static CreatePriceListSchemaPricesInnerDeliveryMethod.IdEnum toRawMethodId(
            DeliveryMethodRef method) {
        String wireValue = method.id().value();
        try {
            return CreatePriceListSchemaPricesInnerDeliveryMethod.IdEnum.fromValue(wireValue);
        } catch (IllegalArgumentException unknownMethod) {
            throw new IllegalArgumentException(
                    "Unknown delivery method id '" + wireValue
                            + "'; check client.dictionaries().deliveryMethods() for the current list",
                    unknownMethod);
        }
    }

    private static CreatePriceListSchemaPricesInnerDeliveryMethodDeliveryTime toRawDeliveryTime(
            DeliveryTime deliveryTime) {
        CreatePriceListSchemaPricesInnerDeliveryMethodDeliveryTime raw =
                new CreatePriceListSchemaPricesInnerDeliveryMethodDeliveryTime();
        raw.setUnit(CreatePriceListSchemaPricesInnerDeliveryMethodDeliveryTime.UnitEnum
                .fromValue(deliveryTime.unit().wireValue()));
        raw.setMinPeriod(deliveryTime.minPeriod());
        raw.setMaxPeriod(deliveryTime.maxPeriod());
        return raw;
    }

    /** Mirrors {@link PriceListMapper}'s read side: the wire carries grosze, the domain carries Money. */
    private static Integer toMinorUnits(Money amount, String fieldName) {
        BigDecimal minorUnits = amount.amount().multiply(MINOR_UNITS_PER_MAJOR);
        try {
            return minorUnits.intValueExact();
        } catch (ArithmeticException notAWholeNumber) {
            throw new IllegalArgumentException(
                    "'" + fieldName + "' must be a whole number of grosze, got " + amount.amount(),
                    notAWholeNumber);
        }
    }

    private static Object toRawLimit(PackingLimit limit) {
        // Java 17 has sealed types but not switch patterns, so the exhaustiveness the sealed interface
        // guarantees is spelled out here; a third permitted subtype would fall through to the throw.
        if (limit instanceof PackingLimit.Total total) {
            return total.maxItems();
        }
        if (limit instanceof PackingLimit.PerSize perSize) {
            return toRawSizeLimits(perSize.limits());
        }
        throw new IllegalArgumentException("Unsupported PackingLimit type: " + limit.getClass().getName());
    }

    private static List<Map<String, Object>> toRawSizeLimits(List<SizeLimit> limits) {
        List<Map<String, Object>> rows = new ArrayList<>(limits.size());
        for (SizeLimit sizeLimit : limits) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put(DIMENSION_KEY, sizeLimit.dimension().wireValue());
            row.put(LIMIT_KEY, sizeLimit.limit());
            rows.add(row);
        }
        return rows;
    }
}
