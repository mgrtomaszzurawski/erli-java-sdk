package io.github.mgrtomaszzurawski.erli.domain.orders;

import java.util.Optional;
import java.util.OptionalLong;

/**
 * The collection point chosen for a pickup-style delivery (a parcel locker, a partner shop, …).
 *
 * <p>Erli marks every property of this object optional, so each accessor is optional here too — a
 * pickup place can arrive with little more than a provider and a name.
 *
 * @param id          Erli's numeric id of the point
 * @param externalId  the provider's own id for the point
 * @param heading     the short headline the buyer saw when choosing the point
 * @param type        the provider-specific point type (e.g. a locker versus a staffed counter)
 * @param provider    the operator of the point
 * @param name        the point's name
 * @param description free-text directions, e.g. where in a building the point sits
 * @param address     the street address
 * @param city        the city
 * @param country     the country, as the provider reports it
 * @param open24h     whether the point is accessible around the clock
 * @param zip         the postal code, in Polish {@code NN-NNN} form
 */
public record PickupPlace(
        OptionalLong id,
        Optional<String> externalId,
        Optional<String> heading,
        Optional<String> type,
        Optional<PickupProvider> provider,
        Optional<String> name,
        Optional<String> description,
        Optional<String> address,
        Optional<String> city,
        Optional<String> country,
        Optional<Boolean> open24h,
        Optional<String> zip) {
}
