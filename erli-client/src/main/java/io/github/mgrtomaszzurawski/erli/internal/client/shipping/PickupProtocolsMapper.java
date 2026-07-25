package io.github.mgrtomaszzurawski.erli.internal.client.shipping;

import io.github.mgrtomaszzurawski.erli.domain.shipping.PickupProtocols;

import java.util.Objects;

/** Maps the generated pickup-protocols payload onto its domain record. Internal. */
final class PickupProtocolsMapper {

    private PickupProtocolsMapper() {
    }

    static PickupProtocols toDomain(io.github.mgrtomaszzurawski.erli.rest.model.PickupProtocols rawProtocols) {
        Objects.requireNonNull(rawProtocols, "raw PickupProtocols");
        String url = rawProtocols.getUrl();
        if (url == null) {
            throw new IllegalStateException("Pickup protocols response is missing the required 'url' field");
        }
        return new PickupProtocols(url);
    }
}
