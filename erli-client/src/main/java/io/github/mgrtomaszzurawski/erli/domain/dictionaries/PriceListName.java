package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

/**
 * The name of a shop price list, used to scope the delivery-method dictionary to one price list
 * ({@code GET /dictionaries/deliveryMethods/{priceList}}).
 *
 * @param value the non-blank price-list name
 */
public record PriceListName(String value) {

    public PriceListName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("PriceListName must not be null or blank");
        }
        value = value.trim();
    }

    public static PriceListName of(String value) {
        return new PriceListName(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
