package io.github.mgrtomaszzurawski.erli.internal.client.shipping;

/**
 * JSON fixtures for the shipping bucket.
 *
 * <p><strong>Provenance.</strong> These payloads are <em>spec-derived</em>, not observed on the wire:
 * the sandbox shop is empty and inactive, so no real parcel exists to capture yet (see
 * {@code KNOWN-SERVER-BEHAVIORS.md} and {@code TESTING.md} → fixture provenance). Every property,
 * nesting level and enum spelling is taken from {@code openapi/swagger.json} schema {@code Parcel},
 * and {@code orderId} follows the spec pattern {@code ^\d{6}x\d{4,}$}. They must be replaced with a
 * captured payload during the Phase 3 live write→read sweep, once bucket A/D have seeded data.
 */
final class ParcelFixtures {

    /** A fully populated parcel: every mapped field present, including both parties and a history entry. */
    static final String FULL_PARCEL_JSON = """
            {
              "id": 55123,
              "type": "internal",
              "orderId": "100007x1234",
              "erliPro": true,
              "dimensions": { "width": 20.5, "height": 10, "length": 30.25, "weight": 1500 },
              "errors": [ { "errorCode": 1201, "errorMessage": "Blad walidacji przesylki" } ],
              "status": "onTheWay",
              "statusHistory": [
                { "status": "preparing", "changed": "2026-07-20T08:15:00Z" },
                { "status": "sent", "changed": "2026-07-21T09:30:00Z" }
              ],
              "shipping": {
                "typeId": "erliKurier24InPost10kg",
                "postingPointId": 4471,
                "additionalInformation": "Leave at reception",
                "sender": {
                  "firstName": "Anna",
                  "lastName": "Kowalska",
                  "companyName": "Test Shop",
                  "street": "Przemyslowa",
                  "buildingNumber": "12",
                  "flatNumber": "3",
                  "city": "Poznan",
                  "zip": "61-001",
                  "country": "pl",
                  "phoneNumber": "500100200",
                  "email": "shop@example.test",
                  "pickupType": "courier",
                  "pointCode": null
                },
                "receiver": {
                  "firstName": "Jan",
                  "lastName": "Nowak",
                  "street": "Kwiatowa",
                  "buildingNumber": "7",
                  "city": "Warszawa",
                  "zip": "00-950",
                  "country": "pl",
                  "phoneNumber": "600300400",
                  "email": "buyer@example.test",
                  "pickupType": "point",
                  "pointCode": "WAW01A"
                },
                "nonStandard": false,
                "registeredAt": "2026-07-21T09:29:00Z",
                "waybillExpiration": "2026-08-21T09:29:00Z",
                "waybills": [ "https://erli.pl/waybill/55123.pdf" ],
                "pickupProtocol": "https://erli.pl/protocol/55123.pdf"
              },
              "trackingNumber": "6200000012345",
              "createdAt": "2026-07-20T08:14:00Z",
              "updatedAt": "2026-07-21T09:30:00Z"
            }
            """;

    /** The same parcel reduced to the spec-required fields only — every optional one absent. */
    static final String MINIMAL_PARCEL_JSON = """
            {
              "type": "internal",
              "dimensions": { "width": 10, "height": 10, "length": 10, "weight": 500 },
              "status": "preparing",
              "shipping": { "typeId": "erliPaczkomat" },
              "createdAt": "2026-07-20T08:14:00Z",
              "updatedAt": "2026-07-20T08:14:00Z"
            }
            """;

    private ParcelFixtures() {
    }
}
