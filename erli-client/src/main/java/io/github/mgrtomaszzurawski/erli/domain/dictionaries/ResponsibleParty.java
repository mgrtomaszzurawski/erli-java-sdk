package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

import java.util.Objects;
import java.util.Optional;

/**
 * A party legally responsible for a product being placed on the market — either the person who
 * introduced it ({@code responsiblePersons}) or its producer ({@code responsibleProducers}). Both
 * dictionaries share this shape.
 *
 * <p><strong>This record holds personal data</strong> (postal address, e-mail, phone). Its
 * {@link #toString()} redacts those three fields so an entry cannot leak into a log line by accident;
 * read them through the accessors when you genuinely need them.
 *
 * @param id the entry identifier
 * @param name the entry's display name in the shop panel
 * @param idempotenceKey the caller-supplied key that makes creation idempotent; unique per shop
 * @param properName the legal name of the responsible party
 * @param country the country of the registered address
 * @param address the street address
 * @param postalCode the postal code
 * @param city the city
 * @param email the contact e-mail
 * @param phone the contact phone number, when given
 * @param source where the entry came from, when stated
 */
public record ResponsibleParty(
        long id,
        String name,
        String idempotenceKey,
        String properName,
        CountryCode country,
        String address,
        String postalCode,
        String city,
        String email,
        Optional<String> phone,
        Optional<ResponsiblePartySource> source) {

    private static final String REDACTED = "***";

    public ResponsibleParty {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(idempotenceKey, "idempotenceKey");
        Objects.requireNonNull(properName, "properName");
        Objects.requireNonNull(country, "country");
        Objects.requireNonNull(address, "address");
        Objects.requireNonNull(postalCode, "postalCode");
        Objects.requireNonNull(city, "city");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(phone, "phone");
        Objects.requireNonNull(source, "source");
    }

    /** Redacts address, e-mail and phone; the remaining fields identify the entry without exposing it. */
    @Override
    public String toString() {
        return "ResponsibleParty[id=" + id
                + ", name=" + name
                + ", idempotenceKey=" + idempotenceKey
                + ", properName=" + properName
                + ", country=" + country
                + ", address=" + REDACTED
                + ", postalCode=" + postalCode
                + ", city=" + city
                + ", email=" + REDACTED
                + ", phone=" + (phone.isPresent() ? REDACTED : Optional.empty())
                + ", source=" + source
                + "]";
    }
}
