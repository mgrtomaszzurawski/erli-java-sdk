package io.github.mgrtomaszzurawski.erli.internal.client.dictionaries;

import io.github.mgrtomaszzurawski.erli.core.error.ErliTransportException;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.CountryCode;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.NewResponsibleParty;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ResponsibleParty;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ResponsiblePartySource;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ResponsiblePartyUpdate;
import io.github.mgrtomaszzurawski.erli.rest.model.CreateResponsibleSchema;
import io.github.mgrtomaszzurawski.erli.rest.model.ResponsibleSchema;
import io.github.mgrtomaszzurawski.erli.rest.model.UpdateResponsibleSchema;

import java.util.Objects;
import java.util.Optional;

/**
 * Maps between the generated Layer-1 responsible-party schemas and the public
 * {@link ResponsibleParty} / {@link NewResponsibleParty} domain records. Internal: never exported.
 */
final class ResponsiblePartyMapper {

    private ResponsiblePartyMapper() {
    }

    static ResponsibleParty toDomain(ResponsibleSchema rawParty) {
        if (rawParty == null) {
            // An empty body decodes to null; surface it as a transport fault rather than an NPE.
            throw new ErliTransportException("A responsible-party endpoint returned no response body");
        }
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
                        .map(source -> ResponsiblePartySource.fromWire(source.getValue())));
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
        return CountryCode.fromWire(country.getValue());
    }

    /** Builds the Layer-1 patch request; only the fields the caller set are carried over. */
    static UpdateResponsibleSchema toUpdateRequest(ResponsiblePartyUpdate update) {
        Objects.requireNonNull(update, "update");
        UpdateResponsibleSchema request = new UpdateResponsibleSchema();
        if (update.name() != null) {
            request.name(update.name());
        }
        if (update.idempotenceKey() != null) {
            request.idempotenceKey(update.idempotenceKey());
        }
        if (update.properName() != null) {
            request.properName(update.properName());
        }
        if (update.country() != null) {
            request.country(toUpdateCountryEnum(update.country()));
        }
        if (update.address() != null) {
            request.address(update.address());
        }
        if (update.postalCode() != null) {
            request.postalCode(update.postalCode());
        }
        if (update.city() != null) {
            request.city(update.city());
        }
        if (update.email() != null) {
            request.email(update.email());
        }
        if (update.phone() != null) {
            request.phone(update.phone());
        }
        if (update.source() != null) {
            request.source(toUpdateSourceEnum(update.source()));
        }
        return request;
    }

    private static CreateResponsibleSchema.CountryEnum toCountryEnum(CountryCode country) {
        CreateResponsibleSchema.CountryEnum resolved =
                CreateResponsibleSchema.CountryEnum.fromValue(country.wireValue());
        if (resolved == null) {
            throw new IllegalArgumentException(
                    "The API does not accept country code: " + country.wireValue());
        }
        return resolved;
    }

    private static CreateResponsibleSchema.SourceEnum toSourceEnum(ResponsiblePartySource source) {
        CreateResponsibleSchema.SourceEnum resolved =
                CreateResponsibleSchema.SourceEnum.fromValue(source.wireValue());
        if (resolved == null) {
            throw new IllegalArgumentException(
                    "The API does not accept responsible-party source: " + source.wireValue());
        }
        return resolved;
    }

    private static UpdateResponsibleSchema.CountryEnum toUpdateCountryEnum(CountryCode country) {
        UpdateResponsibleSchema.CountryEnum resolved =
                UpdateResponsibleSchema.CountryEnum.fromValue(country.wireValue());
        if (resolved == null) {
            throw new IllegalArgumentException(
                    "The API does not accept country code: " + country.wireValue());
        }
        return resolved;
    }

    private static UpdateResponsibleSchema.SourceEnum toUpdateSourceEnum(ResponsiblePartySource source) {
        UpdateResponsibleSchema.SourceEnum resolved =
                UpdateResponsibleSchema.SourceEnum.fromValue(source.wireValue());
        if (resolved == null) {
            throw new IllegalArgumentException(
                    "The API does not accept responsible-party source: " + source.wireValue());
        }
        return resolved;
    }
}
