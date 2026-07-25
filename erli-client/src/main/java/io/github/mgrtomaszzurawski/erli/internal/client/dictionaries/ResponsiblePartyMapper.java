package io.github.mgrtomaszzurawski.erli.internal.client.dictionaries;

import io.github.mgrtomaszzurawski.erli.domain.dictionaries.CountryCode;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.NewResponsibleParty;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ResponsibleParty;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ResponsiblePartySource;
import io.github.mgrtomaszzurawski.erli.rest.model.CreateResponsibleSchema;
import io.github.mgrtomaszzurawski.erli.rest.model.ResponsibleSchema;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Maps between the generated Layer-1 responsible-party schemas and the public
 * {@link ResponsibleParty} / {@link NewResponsibleParty} domain records. Internal: never exported.
 */
final class ResponsiblePartyMapper {

    private ResponsiblePartyMapper() {
    }

    static List<ResponsibleParty> toDomainList(ResponsibleSchema[] rawParties) {
        if (rawParties == null) {
            return List.of();
        }
        return Arrays.stream(rawParties).map(ResponsiblePartyMapper::toDomain).toList();
    }

    static ResponsibleParty toDomain(ResponsibleSchema rawParty) {
        Objects.requireNonNull(rawParty, "raw ResponsibleSchema");
        return new ResponsibleParty(
                requireId(rawParty),
                require(rawParty.getName(), "name"),
                require(rawParty.getIdempotenceKey(), "idempotenceKey"),
                require(rawParty.getProperName(), "properName"),
                requireCountry(rawParty),
                require(rawParty.getAddress(), "address"),
                require(rawParty.getPostalCode(), "postalCode"),
                require(rawParty.getCity(), "city"),
                require(rawParty.getEmail(), "email"),
                Optional.ofNullable(rawParty.getPhone()),
                Optional.ofNullable(rawParty.getSource())
                        .map(source -> ResponsiblePartySource.of(source.getValue())));
    }

    /** Builds the Layer-1 create request from the domain record. */
    static CreateResponsibleSchema toCreateRequest(NewResponsibleParty party) {
        Objects.requireNonNull(party, "party");
        CreateResponsibleSchema request = new CreateResponsibleSchema()
                .name(party.name())
                .idempotenceKey(party.idempotenceKey())
                .properName(party.properName())
                .country(toCountryEnum(party.country()))
                .address(party.address())
                .postalCode(party.postalCode())
                .city(party.city())
                .email(party.email());
        party.phone().ifPresent(request::phone);
        party.source().ifPresent(source -> request.source(toSourceEnum(source)));
        return request;
    }

    private static long requireId(ResponsibleSchema rawParty) {
        Integer id = rawParty.getId();
        if (id == null) {
            throw new IllegalStateException("ResponsibleParty is missing the required 'id' field");
        }
        return id.longValue();
    }

    private static String require(String value, String fieldName) {
        if (value == null) {
            throw new IllegalStateException(
                    "ResponsibleParty is missing the required '" + fieldName + "' field");
        }
        return value;
    }

    private static CountryCode requireCountry(ResponsibleSchema rawParty) {
        ResponsibleSchema.CountryEnum country = rawParty.getCountry();
        if (country == null) {
            throw new IllegalStateException("ResponsibleParty is missing the required 'country' field");
        }
        return CountryCode.of(country.getValue());
    }

    private static CreateResponsibleSchema.CountryEnum toCountryEnum(CountryCode country) {
        CreateResponsibleSchema.CountryEnum resolved =
                CreateResponsibleSchema.CountryEnum.fromValue(country.value());
        if (resolved == null) {
            throw new IllegalArgumentException(
                    "The API does not accept country code: " + country.value());
        }
        return resolved;
    }

    private static CreateResponsibleSchema.SourceEnum toSourceEnum(ResponsiblePartySource source) {
        CreateResponsibleSchema.SourceEnum resolved =
                CreateResponsibleSchema.SourceEnum.fromValue(source.value());
        if (resolved == null) {
            throw new IllegalArgumentException(
                    "The API does not accept responsible-party source: " + source.value());
        }
        return resolved;
    }
}
