package io.github.mgrtomaszzurawski.erli.domain.orders;

import io.github.mgrtomaszzurawski.erli.core.model.Money;
import io.github.mgrtomaszzurawski.erli.core.model.ProductExternalId;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * One purchased line of an order.
 *
 * <p>Prices are per unit, not per line: multiply by {@link #quantity()} for the line total. Erli sends
 * them as integer minor units (grosze); they are exposed here as {@link Money} in the order's currency.
 *
 * @param id                    Erli's numeric id of the order line
 * @param externalId            the seller's own product id, as supplied in the catalogue
 * @param quantity              how many units were bought
 * @param weight                the unit weight as Erli reports it, when known
 * @param unitPrice             the price actually charged per unit
 * @param unitPriceBeforeRebate the pre-rebate price per unit, present when a {@link Rebate} applied
 * @param name                  the product name at the time of purchase
 * @param slug                  the product's URL slug at the time of purchase
 * @param ean                   the product's EAN barcode, when the offer carried one
 * @param sku                   the seller's stock-keeping unit, when the offer carried one
 * @param taxRate               the VAT rate that applied, when Erli reports it
 */
public record OrderItem(
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
}
