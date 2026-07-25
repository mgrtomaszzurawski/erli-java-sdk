package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

import java.util.Objects;
import java.util.Optional;

/**
 * A request to create a responsible person or producer. Build one with {@link #builder()}; the
 * builder rejects a missing mandatory field at {@code build()} time rather than letting the API
 * return a 400.
 *
 * <p>{@code idempotenceKey} must be unique within the shop — reusing one is answered with HTTP 409
 * (surfaced as {@code ErliValidationException}). Choose a key derived from your own record's
 * identity so a retried create is a no-op rather than a duplicate.
 *
 * <p>Holds personal data; {@link #toString()} redacts address, e-mail and phone.
 */
public record NewResponsibleParty(
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
    private static final String ABSENT = "absent";

    public NewResponsibleParty {
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

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "NewResponsibleParty[name=" + name
                + ", idempotenceKey=" + idempotenceKey
                + ", properName=" + properName
                + ", country=" + country
                + ", address=" + REDACTED
                + ", postalCode=" + postalCode
                + ", city=" + city
                + ", email=" + REDACTED
                + ", phone=" + (phone.isPresent() ? REDACTED : ABSENT)
                + ", source=" + source
                + "]";
    }

    /** Builder for {@link NewResponsibleParty}. Every field the API requires is required here too. */
    public static final class Builder {

        private String name;
        private String idempotenceKey;
        private String properName;
        private CountryCode country;
        private String address;
        private String postalCode;
        private String city;
        private String email;
        private String phone;
        private ResponsiblePartySource source;

        private Builder() {
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

        public Builder country(CountryCode value) {
            this.country = value;
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

        public NewResponsibleParty build() {
            return new NewResponsibleParty(
                    required(name, "name"),
                    required(idempotenceKey, "idempotenceKey"),
                    required(properName, "properName"),
                    requiredCountry(country),
                    required(address, "address"),
                    required(postalCode, "postalCode"),
                    required(city, "city"),
                    required(email, "email"),
                    Optional.ofNullable(phone),
                    Optional.ofNullable(source));
        }

        private static CountryCode requiredCountry(CountryCode value) {
            if (value == null) {
                throw new IllegalArgumentException("country is required");
            }
            return value;
        }

        private static String required(String value, String fieldName) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(fieldName + " is required and must not be blank");
            }
            return value;
        }
    }
}
