package io.github.mgrtomaszzurawski.erli.core.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Tracking information for the order's parcel.
 *
 * <p>The API declares two alternative shapes for this object: one carrying a tracking <em>URL</em>,
 * one carrying a <em>carrier plus tracking number</em>. Both always carry the status, so the SDK
 * flattens them into a single record with optional components rather than making callers switch over
 * two near-identical types.
 *
 * <p>{@link #vendor()} is the core-owned {@link DeliveryVendor}, shared with every other bucket that
 * names a carrier.
 *
 * <p><strong>Telling an absent carrier from an unrecognised one.</strong> The API declares the
 * carrier shape as requiring {@code status}, {@code vendor} and {@code trackingNumber} together. So a
 * present {@link #trackingNumber()} with an empty {@link #vendor()} means the carrier arrived but is
 * newer than this SDK's vendored spec — regenerate Layer 1 and add the constant to
 * {@link DeliveryVendor}. An empty {@code vendor} <em>and</em> an empty {@code trackingNumber} is the
 * other shape: the API simply reported a URL, or nothing, and no carrier was ever sent.
 *
 * @param status         where the parcel is
 * @param trackingUrl    a page the buyer can follow the parcel on
 * @param vendor         the carrier, when the API identified one <em>and</em> this SDK version
 *                       recognises it — an unrecognised carrier decodes to absent rather than failing
 *                       the response (CORE-12). The two causes are distinguishable: see the class
 *                       javadoc
 * @param trackingNumber the carrier's tracking number, when the API reports one
 */
public record DeliveryTracking(
        TrackingStatus status,
        Optional<String> trackingUrl,
        Optional<DeliveryVendor> vendor,
        Optional<String> trackingNumber) {

    public DeliveryTracking {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(trackingUrl, "trackingUrl");
        Objects.requireNonNull(vendor, "vendor");
        Objects.requireNonNull(trackingNumber, "trackingNumber");
    }
}
