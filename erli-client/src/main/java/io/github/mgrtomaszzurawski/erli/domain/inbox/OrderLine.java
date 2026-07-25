package io.github.mgrtomaszzurawski.erli.domain.inbox;

import io.github.mgrtomaszzurawski.erli.core.model.Money;
import io.github.mgrtomaszzurawski.erli.core.model.ProductExternalId;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/**
 * One purchased position of the order carried by an {@link OrderEvent}. Prices arrive as integer minor
 * units and are exposed as {@link Money} in the order's currency.
 *
 * @param id                    the marketplace's id for this position
 * @param externalId            the product's id in the shop's own system
 * @param quantity              how many units were bought
 * @param weight                the unit weight in kilograms, when the product declares one
 * @param unitPrice             the price actually paid per unit
 * @param unitPriceBeforeRebate the per-unit price before any rebate, when a rebate applied
 * @param name                  the product name at purchase time
 * @param slug                  the marketplace URL slug at purchase time
 * @param ean                   the product's EAN, if it has one
 * @param sku                   the shop's SKU, if it has one
 * @param taxRate               the VAT rate that applied, when the API reports one this SDK version
 *                              recognises — an unrecognised rate decodes to absent (CORE-12)
 */
public record OrderLine(
        long id,
        ProductExternalId externalId,
        int quantity,
        Optional<BigDecimal> weight,
        Money unitPrice,
        Optional<Money> unitPriceBeforeRebate,
        String name,
        String slug,
        Optional<String> ean,
        Optional<String> sku,
        Optional<TaxRate> taxRate) {

    public OrderLine {
        Objects.requireNonNull(externalId, "externalId");
        Objects.requireNonNull(weight, "weight");
        Objects.requireNonNull(unitPrice, "unitPrice");
        Objects.requireNonNull(unitPriceBeforeRebate, "unitPriceBeforeRebate");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(slug, "slug");
        Objects.requireNonNull(ean, "ean");
        Objects.requireNonNull(sku, "sku");
        Objects.requireNonNull(taxRate, "taxRate");
    }
}
