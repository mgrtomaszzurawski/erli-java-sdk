package io.github.mgrtomaszzurawski.erli.domain.inbox;

import java.util.Objects;
import java.util.Optional;

/**
 * The pickup point the buyer chose, when the delivery method is a pickup rather than a courier. Every
 * component is optional: the API declares no required property on this object.
 *
 * @param id          the marketplace's id for the point
 * @param externalId  the operator's own id for the point
 * @param heading     the point's headline, as shown to the buyer
 * @param type        the operator's type code for the point
 * @param provider    which operator runs the point
 * @param name        the point's name
 * @param description free-form directions, e.g. opening hours or how to find it
 * @param address     the street address
 * @param city        the city
 * @param country     the country, as a free-form code (unlike order addresses, not an enum)
 * @param open24h     whether the point is open around the clock
 * @param zip         the postal code, formatted {@code NN-NNN}
 */
public record PickupPlace(
        Optional<Long> id,
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

    public PickupPlace {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(externalId, "externalId");
        Objects.requireNonNull(heading, "heading");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(address, "address");
        Objects.requireNonNull(city, "city");
        Objects.requireNonNull(country, "country");
        Objects.requireNonNull(open24h, "open24h");
        Objects.requireNonNull(zip, "zip");
    }
}
