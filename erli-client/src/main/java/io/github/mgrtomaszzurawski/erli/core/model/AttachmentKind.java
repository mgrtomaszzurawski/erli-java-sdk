package io.github.mgrtomaszzurawski.erli.core.model;

import io.github.mgrtomaszzurawski.erli.core.error.ErliTransportException;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * What an attachment document is — a user manual, an energy label, a safety data sheet and so on.
 * Several kinds are required by EU product regulations, which is why the list grows; mapped by wire
 * string via {@link #fromWire} per the fleet enum guideline.
 */
public enum AttachmentKind {

    GUIDE("guide"),
    PROMOTION_RULES("promotionRules"),
    CONTEST_RULES("contestRules"),
    BOOK_SNIPPET("bookSnippet"),
    USER_MANUAL("userManual"),
    ASSEMBLY_INSTRUCTIONS("assemblyInstructions"),
    GAME_INSTRUCTIONS("gameInstructions"),
    SAFETY_GUIDE("safetyGuide"),
    ENERGY_LABEL("energyLabel"),
    PRODUCT_CARD("productCard"),
    TIRE_LABEL("tireLabel"),
    DATA_PROCESSING_SOFTWARE("dataProcessingSoftware"),
    DATA_PROCESSING_HARDWARE("dataProcessingHardware"),
    SAFETY_DATA_SHEET("safetyDataSheet"),
    PLANT_PROTECTION_LICENSE("plantProtectionLicense"),
    RECYCLING_INFO("recyclingInfo");

    private static final Map<String, AttachmentKind> BY_WIRE = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(AttachmentKind::wireValue, Function.identity()));

    private final String wireValue;

    AttachmentKind(String wireValue) {
        this.wireValue = wireValue;
    }

    /** The exact string this kind is sent as on the wire. */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolve a wire string to an attachment kind.
     *
     * <p>Since CORE-12 the codec decodes an unrecognised wire value to {@code null} rather than
     * throwing, so this method never sees one: the caller maps the {@code null} itself. It therefore
     * fires only if this domain enum drifts out of sync with the generated one.
     *
     * @throws ErliTransportException if no domain constant maps the given wire value (enum drift)
     */
    public static AttachmentKind fromWire(String wireValue) {
        AttachmentKind kind = BY_WIRE.get(wireValue);
        if (kind == null) {
            throw new ErliTransportException(
                    "No AttachmentKind constant maps wire value '" + wireValue
                            + "'; this domain enum is out of sync with the generated model");
        }
        return kind;
    }
}
