package io.github.mgrtomaszzurawski.erli.domain.shipping;

import io.github.mgrtomaszzurawski.erli.core.model.ParcelId;

/**
 * Access to shipping: the parcels a seller creates for Erli's carrier integration, the externally
 * shipped parcels tracked alongside them, and the posting points and pickup protocols around them.
 * Reached via {@code client.shipping()}.
 *
 * <p>Public surface — consumers import only this package. Operations are added here as the bucket
 * fills; {@link #parcel(ParcelId)} is the first.
 */
public interface ShippingAccess {

    /**
     * Fetch a single parcel by id ({@code GET /shipping/parcels/{id}}).
     *
     * @param parcelId the parcel to fetch; must not be null
     * @return the parcel, including its carriage details and status history
     * @throws io.github.mgrtomaszzurawski.erli.core.error.ErliNotFoundException if no parcel with that
     *         id belongs to the authenticated shop
     */
    Parcel parcel(ParcelId parcelId);
}
