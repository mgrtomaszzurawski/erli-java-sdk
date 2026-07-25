package io.github.mgrtomaszzurawski.erli.internal.client.dictionaries;

import io.github.mgrtomaszzurawski.erli.domain.dictionaries.CountryCode;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.NewResponsibleParty;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ResponsibleParty;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ResponsiblePartySource;
import io.github.mgrtomaszzurawski.erli.internal.JsonCodec;
import io.github.mgrtomaszzurawski.erli.rest.model.CreateResponsibleSchema;
import io.github.mgrtomaszzurawski.erli.rest.model.ResponsibleSchema;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
                .country(CountryCode.POLAND)
                .address(ADDRESS)
                .postalCode("00-001")
                .city("Warszawa")
                .email(EMAIL);
    }

    @Test
    void mapsEveryFieldOfAResponsibleParty() {
        ResponsibleParty party = ResponsiblePartyMapper.toDomainList(decode(RESPONSIBLE_PARTY_JSON)).get(0);

        assertEquals(7L, party.id());
        assertEquals("Importer PL", party.name());
        assertEquals("imp-001", party.idempotenceKey());
        assertEquals("Importer Sp. z o.o.", party.properName());
        assertEquals(CountryCode.POLAND, party.country());
        assertEquals(ADDRESS, party.address());
        assertEquals("00-001", party.postalCode());
        assertEquals("Warszawa", party.city());
        assertEquals(EMAIL, party.email());
        assertEquals(PHONE, party.phone().orElseThrow());
        assertEquals(ResponsiblePartySource.API, party.source().orElseThrow());
    }

    @Test
    void redactsPersonalDataFromToString() {
        ResponsibleParty party = ResponsiblePartyMapper.toDomainList(decode(RESPONSIBLE_PARTY_JSON)).get(0);

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
                assertThrows(IllegalStateException.class, () -> ResponsiblePartyMapper.toDomainList(raw));

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

        assertEquals(null, request.getPhone());
        assertEquals(null, request.getSource());
    }

    @Test
    void builderRejectsAMissingMandatoryFieldBeforeTheRequestIsSent() {
        NewResponsibleParty.Builder missingEmail = NewResponsibleParty.builder()
                .name("Importer PL")
                .idempotenceKey("imp-001")
                .properName("Importer Sp. z o.o.")
                .country(CountryCode.POLAND)
                .address(ADDRESS)
                .postalCode("00-001")
                .city("Warszawa");

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, missingEmail::build);

        assertTrue(failure.getMessage().contains("email"), failure.getMessage());
    }

    @Test
    void rejectsACountryTheApiDoesNotAccept() {
        NewResponsibleParty party = validParty().country(CountryCode.of("zz")).build();

        assertThrows(IllegalArgumentException.class, () -> ResponsiblePartyMapper.toCreateRequest(party));
    }

    @Test
    void mapsEmptyAndNullToAnEmptyList() {
        assertTrue(ResponsiblePartyMapper.toDomainList(decode("[]")).isEmpty());
        assertTrue(ResponsiblePartyMapper.toDomainList(null).isEmpty());
    }
}
