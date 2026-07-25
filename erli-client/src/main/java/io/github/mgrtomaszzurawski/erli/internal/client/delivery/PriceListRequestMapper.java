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
import java.math.RoundingMode;
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
    /** Erli prices delivery in grosze; the read side decodes the same way. */
    private static final String CURRENCY_CODE = "PLN";
    /** Grosze per złoty — the scale {@link #CURRENCY_CODE} fixes. */
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

    /**
     * Mirror of {@link PriceListMapper}'s read side: the wire carries grosze, the domain carries
     * {@link Money}. Delivery is priced in {@link #CURRENCY_CODE} only — the price-list schemas carry no
     * currency field at all, and the read side decodes grosze unconditionally — so another currency is
     * refused rather than silently sent at the wrong scale. The two remaining failures are reported
     * apart, because "0.5 grosza" and "more than a billion złoty" need different fixes.
     */
    private static Integer toMinorUnits(Money amount, String fieldName) {
        if (!CURRENCY_CODE.equals(amount.currency().getCurrencyCode())) {
            throw new IllegalArgumentException("'" + fieldName + "' must be in " + CURRENCY_CODE
                    + "; Erli prices delivery in grosze and the read side decodes it as such, got "
                    + amount.currency().getCurrencyCode());
        }
        BigDecimal minorUnits = amount.amount().multiply(MINOR_UNITS_PER_MAJOR);
        BigDecimal whole;
        try {
            whole = minorUnits.setScale(0, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException notAWholeNumber) {
            throw new IllegalArgumentException("'" + fieldName + "' is not a whole number of "
                    + amount.currency().getCurrencyCode() + " minor units: " + amount.amount(),
                    notAWholeNumber);
        }
        try {
            return whole.intValueExact();
        } catch (ArithmeticException tooLarge) {
            throw new IllegalArgumentException("'" + fieldName + "' exceeds the range the API accepts: "
                    + amount.amount() + " " + amount.currency().getCurrencyCode(), tooLarge);
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
