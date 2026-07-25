package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

import io.github.mgrtomaszzurawski.erli.core.error.ErliTransportException;

/**
 * A marketplace storefront an {@link Attachment} applies to. Small and closed — ERLI runs a Polish
 * and a German storefront — so this one is matched with an explicit {@code switch} rather than a
 * wire map, per the fleet enum guideline.
 */
public enum Market {

    /** The Polish storefront, {@code pl}. */
    POLAND("pl"),

    /** The German storefront, {@code de}. */
    GERMANY("de");

    private final String wireValue;

    Market(String wireValue) {
        this.wireValue = wireValue;
    }

    /** The exact string this market is sent as on the wire. */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolve a wire string to a market.
     *
     * @throws ErliTransportException if the value is neither documented storefront (enum drift)
     */
    public static Market fromWire(String wireValue) {
        return switch (wireValue) {
            case "pl" -> POLAND;
            case "de" -> GERMANY;
            default -> throw new ErliTransportException(
                    "No Market constant maps wire value '" + wireValue
                            + "'; this domain enum is out of sync with the generated model");
        };
    }
}
