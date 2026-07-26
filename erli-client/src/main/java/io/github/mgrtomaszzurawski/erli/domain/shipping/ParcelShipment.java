package io.github.mgrtomaszzurawski.erli.domain.shipping;

import io.github.mgrtomaszzurawski.erli.core.model.ShippingMethodId;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The carriage details of a parcel: which delivery method carries it, where it is posted, who sends
 * and receives it, and the documents the carrier issued.
 *
 * <p>{@code deliveryMethod} is modelled as the core {@link ShippingMethodId} value rather than an SDK
 * enum: the vocabulary is long, it grows with every carrier deal, and the Dictionaries bucket already
 * exposes the authoritative list keyed by the same id — so a second copy here would only drift. Note
 * this does <em>not</em> make the read tolerant of a method Erli adds later: the generated Layer-1 type
 * is still a closed enum, so an unknown value fails at JSON decode before this record is built. That
 * codec-wide policy is tracked as CORE-12 in {@code BACKLOG.md}.
 *
 * @param deliveryMethod       the Erli delivery method carrying the parcel ({@code typeId} on the wire)
 * @param postingPointId       the seller's posting point, when one is assigned
 * @param additionalInformation free-text note passed to the carrier, when supplied
 * @param sender               the sending party, when the API disclosed it
 * @param receiver             the receiving party — carries buyer personal data
 * @param nonStandard          whether the carrier flagged the parcel as non-standard sized
 * @param registeredAt         when the carrier registered the shipment, once it did
 * @param waybillExpiration    when the issued waybills stop being valid, once issued
 * @param waybills             waybill document links; empty until the carrier issues them
 * @param pickupProtocol       link to the courier pickup protocol, once generated
 */
public record ParcelShipment(
        Optional<ShippingMethodId> deliveryMethod,
        Optional<Long> postingPointId,
        Optional<String> additionalInformation,
        Optional<ShippingParty> sender,
        Optional<ShippingParty> receiver,
        boolean nonStandard,
        Optional<OffsetDateTime> registeredAt,
        Optional<OffsetDateTime> waybillExpiration,
        List<String> waybills,
        Optional<String> pickupProtocol) {

    public ParcelShipment {
        Objects.requireNonNull(deliveryMethod, "deliveryMethod");
        Objects.requireNonNull(postingPointId, "postingPointId");
        Objects.requireNonNull(additionalInformation, "additionalInformation");
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(receiver, "receiver");
        Objects.requireNonNull(registeredAt, "registeredAt");
        Objects.requireNonNull(waybillExpiration, "waybillExpiration");
        Objects.requireNonNull(pickupProtocol, "pickupProtocol");
        waybills = List.copyOf(Objects.requireNonNull(waybills, "waybills"));
    }

    /**
     * Renders the shipment with its personal data withheld. Beyond the two parties — which redact
     * themselves — two things count as personal data here:
     * <ul>
     *   <li>{@code additionalInformation} is free text the seller passes to the courier ("leave with
     *       the neighbour at flat 3, gate code 1234"), so it routinely restates the delivery address.</li>
     *   <li>{@code waybills} and {@code pickupProtocol} are links to the shipping label and pickup
     *       protocol — documents carrying the buyer's name and full address. A link is not the
     *       document, but logging one hands out a retrievable copy, so only the count is disclosed.</li>
     * </ul>
     */
    @Override
    public String toString() {
        return "ParcelShipment[deliveryMethod=" + Redaction.show(deliveryMethod)
                + ", postingPointId=" + Redaction.show(postingPointId)
                + ", additionalInformation=" + Redaction.hide(additionalInformation)
                + ", sender=" + Redaction.show(sender)
                + ", receiver=" + Redaction.show(receiver)
                + ", nonStandard=" + nonStandard
                + ", registeredAt=" + Redaction.show(registeredAt)
                + ", waybillExpiration=" + Redaction.show(waybillExpiration)
                + ", waybills=" + Redaction.hideCount(waybills)
                + ", pickupProtocol=" + Redaction.hide(pickupProtocol)
                + ']';
    }
}
