package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

/**
 * A partial update of a responsible person or producer
 * ({@code PATCH /dictionaries/responsible{Persons,Producers}/{id}}). Only the fields set on the
 * builder are sent; an unset field is left untouched by the API rather than cleared.
 *
 * <p><strong>{@code country} must always be supplied</strong>, even when it is not what you are
 * changing. The published schema marks every field optional, but the live API answers a body without
 * {@code country} with {@code 400 "country is required"} — observed on the sandbox, 2026-07-25. The
 * builder therefore requires it, so the mistake surfaces as an {@link IllegalArgumentException}
 * before the request is sent rather than as a puzzling validation error.
 *
 * <p>Holds personal data; {@link #toString()} redacts address, e-mail and phone.
 *
 * @param name the new display name, or {@code null} to leave it
 * @param idempotenceKey the new idempotence key, or {@code null} to leave it
 * @param properName the new legal name, or {@code null} to leave it
 * @param country the country; always sent, never {@code null}
 * @param address the new street address, or {@code null} to leave it
 * @param postalCode the new postal code, or {@code null} to leave it
 * @param city the new city, or {@code null} to leave it
 * @param email the new contact e-mail, or {@code null} to leave it
 * @param phone the new contact phone, or {@code null} to leave it
 * @param source the new source, or {@code null} to leave it
 */
public record ResponsiblePartyUpdate(
        String name,
        String idempotenceKey,
        String properName,
        CountryCode country,
        String address,
        String postalCode,
        String city,
        String email,
        String phone,
        ResponsiblePartySource source) {

    private static final String REDACTED = "***";
    private static final String COUNTRY_REQUIRED_MESSAGE =
            "country is required on every responsible-party update: the API rejects a body without it, "
                    + "even when the country is not the field being changed";

    public ResponsiblePartyUpdate {
        if (country == null) {
            throw new IllegalArgumentException(COUNTRY_REQUIRED_MESSAGE);
        }
    }

    /**
     * Start an update for an entry in the given country.
     *
     * @param country the entry's country; the API requires it on every update
     */
    public static Builder builder(CountryCode country) {
        return new Builder(country);
    }

    @Override
    public String toString() {
        return "ResponsiblePartyUpdate[name=" + name
                + ", idempotenceKey=" + idempotenceKey
                + ", properName=" + properName
                + ", country=" + country
                + ", address=" + redact(address)
                + ", postalCode=" + postalCode
                + ", city=" + city
                + ", email=" + redact(email)
                + ", phone=" + redact(phone)
                + ", source=" + source
                + "]";
    }

    private static String redact(String value) {
        return value == null ? null : REDACTED;
    }

    /** Builder for {@link ResponsiblePartyUpdate}. */
    public static final class Builder {

        private final CountryCode country;
        private String name;
        private String idempotenceKey;
        private String properName;
        private String address;
        private String postalCode;
        private String city;
        private String email;
        private String phone;
        private ResponsiblePartySource source;

        private Builder(CountryCode country) {
            if (country == null) {
                throw new IllegalArgumentException(COUNTRY_REQUIRED_MESSAGE);
            }
            this.country = country;
        }

        public Builder name(String value) {
            this.name = value;
            return this;
        }

        public Builder idempotenceKey(String value) {
            this.idempotenceKey = value;
            return this;
        }

        public Builder properName(String value) {
            this.properName = value;
            return this;
        }

        public Builder address(String value) {
            this.address = value;
            return this;
        }

        public Builder postalCode(String value) {
            this.postalCode = value;
            return this;
        }

        public Builder city(String value) {
            this.city = value;
            return this;
        }

        public Builder email(String value) {
            this.email = value;
            return this;
        }

        public Builder phone(String value) {
            this.phone = value;
            return this;
        }

        public Builder source(ResponsiblePartySource value) {
            this.source = value;
            return this;
        }

        public ResponsiblePartyUpdate build() {
            return new ResponsiblePartyUpdate(
                    name, idempotenceKey, properName, country, address, postalCode, city, email, phone, source);
        }
    }
}
