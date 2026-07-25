package io.github.mgrtomaszzurawski.erli.internal.client.dictionaries;

import io.github.mgrtomaszzurawski.erli.domain.dictionaries.CountryCode;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.NewResponsibleParty;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ResponsibleParty;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ResponsiblePartySource;
import io.github.mgrtomaszzurawski.erli.internal.JsonCodec;
import io.github.mgrtomaszzurawski.erli.rest.model.CreateResponsibleSchema;
import io.github.mgrtomaszzurawski.erli.rest.model.ResponsibleSchema;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ResponsiblePartyUpdate;
import io.github.mgrtomaszzurawski.erli.rest.model.UpdateResponsibleSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponsiblePartyMapperTest {

    private static final String ADDRESS = "ul. Przykładowa 1";
    private static final String EMAIL = "kontakt@example.com";
    private static final String PHONE = "+48123456789";

    /**
     * Shaped from the {@code ResponsibleSchema} spec: the sandbox shop has no responsible persons, so
     * this fixture is schema-derived rather than observed, and the field names are pinned against the
     * generated model.
     */
    private static final String RESPONSIBLE_PARTY_JSON = """
            [{"id":7,"name":"Importer PL","idempotenceKey":"imp-001","properName":"Importer Sp. z o.o.",
              "country":"pl","address":"ul. Przykładowa 1","postalCode":"00-001","city":"Warszawa",
              "phone":"+48123456789","email":"kontakt@example.com","source":"api"}]""";

    private static ResponsibleSchema[] decode(String json) {
        return new JsonCodec().read(json, ResponsibleSchema[].class);
    }

    private static NewResponsibleParty.Builder validParty() {
        return NewResponsibleParty.builder()
                .name("Importer PL")
                .idempotenceKey("imp-001")
                .properName("Importer Sp. z o.o.")
                .country(CountryCode.PL)
                .address(ADDRESS)
                .postalCode("00-001")
                .city("Warszawa")
                .email(EMAIL);
    }

    private static List<ResponsibleParty> mapAll(String json) {
        return Arrays.stream(decode(json)).map(ResponsiblePartyMapper::toDomain).toList();
    }

    @Test
    void mapsEveryFieldOfAResponsibleParty() {
        ResponsibleParty party = mapAll(RESPONSIBLE_PARTY_JSON).get(0);

        assertEquals(7L, party.id());
        assertEquals("Importer PL", party.name());
        assertEquals("imp-001", party.idempotenceKey());
        assertEquals("Importer Sp. z o.o.", party.properName());
        assertEquals(CountryCode.PL, party.country());
        assertEquals(ADDRESS, party.address());
        assertEquals("00-001", party.postalCode());
        assertEquals("Warszawa", party.city());
        assertEquals(EMAIL, party.email());
        assertEquals(PHONE, party.phone().orElseThrow());
        assertEquals(ResponsiblePartySource.API, party.source().orElseThrow());
    }

    @Test
    void redactsPersonalDataFromToString() {
        ResponsibleParty party = mapAll(RESPONSIBLE_PARTY_JSON).get(0);

        String rendered = party.toString();

        assertFalse(rendered.contains(ADDRESS), rendered);
        assertFalse(rendered.contains(EMAIL), rendered);
        assertFalse(rendered.contains(PHONE), rendered);
        // Identifying, non-sensitive fields stay readable so a log line is still useful.
        assertTrue(rendered.contains("Importer PL"), rendered);
        assertTrue(rendered.contains("id=7"), rendered);
    }

    @Test
    void rejectsAPartyMissingASpecRequiredField() {
        ResponsibleSchema[] raw = decode("[{\"id\":7,\"name\":\"Importer PL\"}]");

        IllegalStateException failure =
                assertThrows(IllegalStateException.class, () -> ResponsiblePartyMapper.toDomain(raw[0]));

        assertTrue(failure.getMessage().contains("idempotenceKey"), failure.getMessage());
    }

    @Test
    void buildsTheCreateRequestFromTheDomainRecord() {
        NewResponsibleParty party = validParty().phone(PHONE).source(ResponsiblePartySource.API).build();

        CreateResponsibleSchema request = ResponsiblePartyMapper.toCreateRequest(party);

        assertEquals("Importer PL", request.getName());
        assertEquals("imp-001", request.getIdempotenceKey());
        assertEquals("Importer Sp. z o.o.", request.getProperName());
        assertEquals(CreateResponsibleSchema.CountryEnum.PL, request.getCountry());
        assertEquals(ADDRESS, request.getAddress());
        assertEquals("00-001", request.getPostalCode());
        assertEquals("Warszawa", request.getCity());
        assertEquals(EMAIL, request.getEmail());
        assertEquals(PHONE, request.getPhone());
        assertEquals(CreateResponsibleSchema.SourceEnum.API, request.getSource());
    }

    @Test
    void omitsTheOptionalFieldsWhenTheyWereNotSupplied() {
        CreateResponsibleSchema request = ResponsiblePartyMapper.toCreateRequest(validParty().build());

        assertNull(request.getPhone());
        assertNull(request.getSource());
    }

    @Test
    void builderRejectsAMissingMandatoryFieldBeforeTheRequestIsSent() {
        NewResponsibleParty.Builder missingEmail = NewResponsibleParty.builder()
                .name("Importer PL")
                .idempotenceKey("imp-001")
                .properName("Importer Sp. z o.o.")
                .country(CountryCode.PL)
                .address(ADDRESS)
                .postalCode("00-001")
                .city("Warszawa");

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, missingEmail::build);

        assertTrue(failure.getMessage().contains("email"), failure.getMessage());
    }

    /**
     * The real invariant behind the mapper's country guard: every domain {@link CountryCode} must
     * resolve to a generated one. Both are derived from the same vendored spec, so this fails only if
     * the hand-written enum drifts. Parameterised so a drifted constant is reported by name and the
     * remaining 248 still run — with a loop the first mismatch would hide every other one.
     */
    @ParameterizedTest
    @EnumSource(CountryCode.class)
    void everyDomainCountryIsAcceptedByTheGeneratedRequestModel(CountryCode country) {
        NewResponsibleParty party = validParty().country(country).build();

        CreateResponsibleSchema request = ResponsiblePartyMapper.toCreateRequest(party);

        assertEquals(country.wireValue(), request.getCountry().getValue());
    }

    /** Same invariant for the integration-source enum. */
    @ParameterizedTest
    @EnumSource(ResponsiblePartySource.class)
    void everyDomainSourceIsAcceptedByTheGeneratedRequestModel(ResponsiblePartySource source) {
        NewResponsibleParty party = validParty().source(source).build();

        CreateResponsibleSchema request = ResponsiblePartyMapper.toCreateRequest(party);

        assertEquals(source.wireValue(), request.getSource().getValue());
    }

    @Test
    void buildsTheUpdateRequestFromEveryFieldOfTheDomainRecord() {
        UpdateResponsibleSchema request = ResponsiblePartyMapper.toUpdateRequest(
                ResponsiblePartyUpdate.builder(CountryCode.DE)
                        .name("Nowy importer")
                        .idempotenceKey("imp-002")
                        .properName("Nowy Importer GmbH")
                        .address("Musterstr. 2")
                        .postalCode("10115")
                        .city("Berlin")
                        .email("kontakt@example.de")
                        .phone("+49301234567")
                        .source(ResponsiblePartySource.MANUAL)
                        .build());

        assertEquals("Nowy importer", request.getName());
        assertEquals("imp-002", request.getIdempotenceKey());
        assertEquals("Nowy Importer GmbH", request.getProperName());
        assertEquals(UpdateResponsibleSchema.CountryEnum.DE, request.getCountry());
        assertEquals("Musterstr. 2", request.getAddress());
        assertEquals("10115", request.getPostalCode());
        assertEquals("Berlin", request.getCity());
        assertEquals("kontakt@example.de", request.getEmail());
        assertEquals("+49301234567", request.getPhone());
        assertEquals(UpdateResponsibleSchema.SourceEnum.MANUAL, request.getSource());
    }

    @Test
    void sendsOnlyTheUpdateFieldsThatWereSetPlusTheMandatoryCountry() {
        UpdateResponsibleSchema request = ResponsiblePartyMapper.toUpdateRequest(
                ResponsiblePartyUpdate.builder(CountryCode.PL).city("Kraków").build());

        assertEquals("Kraków", request.getCity());
        assertEquals(UpdateResponsibleSchema.CountryEnum.PL, request.getCountry());
        assertNull(request.getName());
        assertNull(request.getEmail());
        assertNull(request.getPhone());
    }

    @Test
    void rejectsAnUpdateWithoutACountry() {
        // The API answers 400 "country is required" even for a one-field patch.
        assertThrows(IllegalArgumentException.class, () -> ResponsiblePartyUpdate.builder(null));
    }

    @Test
    void redactsPersonalDataFromAnUpdateToString() {
        String rendered = ResponsiblePartyUpdate.builder(CountryCode.PL)
                .address(ADDRESS)
                .email(EMAIL)
                .phone(PHONE)
                .build()
                .toString();

        assertFalse(rendered.contains(ADDRESS), rendered);
        assertFalse(rendered.contains(EMAIL), rendered);
        assertFalse(rendered.contains(PHONE), rendered);
    }

    @Test
    void mapsAnEmptyPayloadToAnEmptyList() {
        assertTrue(mapAll("[]").isEmpty());
    }
}
