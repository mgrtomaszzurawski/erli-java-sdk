package io.github.mgrtomaszzurawski.erli.domain.shipping;

import io.github.mgrtomaszzurawski.erli.core.model.ParcelId;

import java.util.List;

/**
 * Access to shipping: the parcels a seller hands to Erli's carrier integration, the externally shipped
 * parcels tracked alongside them, and the posting points and pickup protocols around both. Reached via
 * {@code client.shipping()}.
 *
 * <p>Public surface — consumers import only this package. Delivery pricing lives in
 * {@code domain.delivery}.
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

    /**
     * Hand one or more parcels to Erli's carrier integration ({@code POST /shipping/parcels/}).
     *
     * <p>All-or-nothing: the API refuses the whole batch if any order is cancelled, or if a second
     * parcel for an order gives a receiver address differing from the first.
     *
     * @param drafts the parcels to create; must not be empty
     * @return the created parcels, in the order requested
     * @throws io.github.mgrtomaszzurawski.erli.core.error.ErliValidationException if the batch is refused
     */
    List<Parcel> createParcels(List<ParcelDraft> drafts);

    /**
     * Search parcels ({@code POST /shipping/parcels/_search}).
     *
     * <p>Erli's search body carries exactly one filter — there is no array and no and/or wrapper — so
     * this takes one rather than a list a caller could over-fill and have silently truncated.
     *
     * @param filter the filter to apply
     * @return the matching parcels
     */
    List<Parcel> searchParcels(ParcelFilter filter);

    /**
     * Cancel a parcel ({@code DELETE /shipping/parcels/{id}}).
     *
     * <p>Cancelling a parcel whose courier pickup is already booked is refused; cancel the pickup first.
     *
     * @param parcelId the parcel to cancel
     * @return the parcel in its cancelled state
     */
    Parcel cancelParcel(ParcelId parcelId);

    /**
     * Register parcels shipped outside Erli ({@code POST /shipping/external}).
     *
     * <p>Per-entry, not all-or-nothing: each result is either the created parcel or the refused entry
     * with its reasons.
     *
     * @param drafts the parcels to register; must not be empty
     * @return one result per entry, in the order requested
     */
    List<ExternalParcelResult> registerExternalParcels(List<ExternalParcelDraft> drafts);

    /**
     * Fetch an external parcel by id ({@code GET /shipping/external/{id}}).
     *
     * @param parcelId the parcel to fetch
     * @return the external parcel
     * @throws io.github.mgrtomaszzurawski.erli.core.error.ErliNotFoundException if it does not exist
     */
    ExternalParcel externalParcel(ParcelId parcelId);

    /**
     * Update an external parcel ({@code PATCH /shipping/external/{id}}).
     *
     * @param parcelId the parcel to update
     * @param update   the new carriage description
     * @return the updated parcel
     */
    ExternalParcel updateExternalParcel(ParcelId parcelId, ExternalParcelUpdate update);

    /**
     * Delete an external parcel ({@code DELETE /shipping/external/{id}}).
     *
     * @param parcelId the parcel to delete
     */
    void deleteExternalParcel(ParcelId parcelId);

    /**
     * Get the link to the courier pickup confirmations for a set of parcels
     * ({@code GET /shipping/pickupProtocols}).
     *
     * @param parcelIds the parcels to include; must not be empty
     * @return where to download the protocol document
     */
    PickupProtocols pickupProtocols(List<ParcelId> parcelIds);

    /**
     * List the shop's posting points ({@code GET /shipping/postingPoints}).
     *
     * @param query filters to apply; {@link PostingPointQuery#none()} for all of them
     * @return the matching posting points
     */
    List<PostingPoint> postingPoints(PostingPointQuery query);
}
