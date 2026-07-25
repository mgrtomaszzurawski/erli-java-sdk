package io.github.mgrtomaszzurawski.erli.domain.shipping;

import java.util.Objects;

/**
 * Where to download the courier pickup confirmations for a set of parcels.
 *
 * <p>The API returns a single link covering every parcel asked for, not one per parcel. The document
 * behind it lists the parcels and their destinations, so treat the URL as it were the document.
 *
 * @param url link to the generated pickup-protocol document
 */
public record PickupProtocols(String url) {

    public PickupProtocols {
        Objects.requireNonNull(url, "url");
    }
}
