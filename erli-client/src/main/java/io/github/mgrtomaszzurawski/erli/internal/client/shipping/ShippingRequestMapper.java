package io.github.mgrtomaszzurawski.erli.internal.client.shipping;

import io.github.mgrtomaszzurawski.erli.domain.shipping.ExternalParcelDraft;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ExternalParcelUpdate;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ParcelDimensions;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ParcelDraft;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ParcelFilter;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ParcelStatus;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ShippingParty;
import io.github.mgrtomaszzurawski.erli.rest.model.CreateExternalParcelInner;
import io.github.mgrtomaszzurawski.erli.rest.model.CreateExternalParcelInnerTrackingNumber;
import io.github.mgrtomaszzurawski.erli.rest.model.CreateExternalParcelInnerTypeId;
import io.github.mgrtomaszzurawski.erli.rest.model.CreateParcelsInner;
import io.github.mgrtomaszzurawski.erli.rest.model.CreateParcelsInnerDimensions;
import io.github.mgrtomaszzurawski.erli.rest.model.CreateParcelsInnerShipping;
import io.github.mgrtomaszzurawski.erli.rest.model.CreateParcelsInnerShippingReceiver;
import io.github.mgrtomaszzurawski.erli.rest.model.CreateParcelsInnerShippingReceiverZip;
import io.github.mgrtomaszzurawski.erli.rest.model.EditExternalParcel;
import io.github.mgrtomaszzurawski.erli.rest.model.SearchParcels;
import io.github.mgrtomaszzurawski.erli.rest.model.SearchParcelsFilter;
import io.github.mgrtomaszzurawski.erli.rest.model.SearchParcelsFilterAnyOf;
import io.github.mgrtomaszzurawski.erli.rest.model.SearchParcelsFilterAnyOf1;
import io.github.mgrtomaszzurawski.erli.rest.model.SearchParcelsFilterAnyOfValue;
import java.util.ArrayList;
import java.util.List;

/**
 * Maps the public shipping request records onto the generated request models. Internal.
 *
 * <p>Where an enum value comes from the caller rather than the wire, an unknown one is rejected here
 * with {@link IllegalArgumentException} — no request has been made, so it is the caller's input that
 * is wrong, and failing before the call gives a better message than the server's rejection would.
 */
final class ShippingRequestMapper {

    private ShippingRequestMapper() {
    }

    static List<CreateParcelsInner> toRawParcels(List<ParcelDraft> drafts) {
        List<CreateParcelsInner> rawDrafts = new ArrayList<>(drafts.size());
        for (ParcelDraft draft : drafts) {
            rawDrafts.add(toRawParcel(draft));
        }
        return rawDrafts;
    }

    private static CreateParcelsInner toRawParcel(ParcelDraft draft) {
        CreateParcelsInner raw = new CreateParcelsInner();
        raw.setOrderId(draft.orderId().value());
        raw.setDimensions(toRawDimensions(draft.dimensions()));
        raw.setShipping(toRawShipping(draft));
        return raw;
    }

    private static CreateParcelsInnerDimensions toRawDimensions(ParcelDimensions dimensions) {
        CreateParcelsInnerDimensions raw = new CreateParcelsInnerDimensions();
        raw.setWidth(dimensions.width());
        raw.setHeight(dimensions.height());
        raw.setLength(dimensions.length());
        raw.setWeight(dimensions.weight());
        return raw;
    }

    private static CreateParcelsInnerShipping toRawShipping(ParcelDraft draft) {
        CreateParcelsInnerShipping raw = new CreateParcelsInnerShipping();
        raw.setTypeId(deliveryMethod(draft.deliveryMethod().value()));
        raw.setReceiver(toRawReceiver(draft.receiver()));
        draft.postingPointId().ifPresent(postingPointId -> raw.setPostingPointId(toInt(postingPointId)));
        draft.additionalInformation().ifPresent(raw::setAdditionalInformation);
        if (draft.nonStandard()) {
            raw.setNonStandard(true);
        }
        return raw;
    }

    private static CreateParcelsInnerShipping.TypeIdEnum deliveryMethod(String wireValue) {
        try {
            return CreateParcelsInnerShipping.TypeIdEnum.fromValue(wireValue);
        } catch (IllegalArgumentException unknownMethod) {
            throw new IllegalArgumentException("Unknown delivery method '" + wireValue
                    + "'; check client.dictionaries().deliveryMethods() for the current list", unknownMethod);
        }
    }

    private static CreateParcelsInnerShippingReceiver toRawReceiver(ShippingParty receiver) {
        CreateParcelsInnerShippingReceiver raw = new CreateParcelsInnerShippingReceiver();
        receiver.firstName().ifPresent(raw::setFirstName);
        receiver.lastName().ifPresent(raw::setLastName);
        receiver.companyName().ifPresent(raw::setCompanyName);
        receiver.street().ifPresent(raw::setStreet);
        receiver.buildingNumber().ifPresent(raw::setBuildingNumber);
        receiver.flatNumber().ifPresent(raw::setFlatNumber);
        receiver.city().ifPresent(raw::setCity);
        receiver.zip().ifPresent(zip -> raw.setZip(new CreateParcelsInnerShippingReceiverZip(zip)));
        receiver.country().ifPresent(country -> raw.setCountry(
                CreateParcelsInnerShippingReceiver.CountryEnum.fromValue(country.wireValue())));
        receiver.phoneNumber().ifPresent(raw::setPhoneNumber);
        receiver.email().ifPresent(raw::setEmail);
        receiver.pickupType().ifPresent(pickupType -> raw.setPickupType(
                CreateParcelsInnerShippingReceiver.PickupTypeEnum.fromValue(pickupType.wireValue())));
        receiver.pointCode().ifPresent(raw::setPointCode);
        return raw;
    }

    static List<CreateExternalParcelInner> toRawExternalParcels(List<ExternalParcelDraft> drafts) {
        List<CreateExternalParcelInner> rawDrafts = new ArrayList<>(drafts.size());
        for (ExternalParcelDraft draft : drafts) {
            CreateExternalParcelInner raw = new CreateExternalParcelInner();
            raw.setOrderId(draft.orderId().value());
            raw.setVendor(CreateExternalParcelInner.VendorEnum.fromValue(draft.vendor().wireValue()));
            draft.status().ifPresent(status -> raw.setStatus(externalStatus(status)));
            draft.trackingNumber().ifPresent(
                    trackingNumber -> raw.setTrackingNumber(new CreateExternalParcelInnerTrackingNumber(trackingNumber)));
            draft.deliveryMethod().ifPresent(
                    deliveryMethod -> raw.setTypeId(new CreateExternalParcelInnerTypeId(deliveryMethod.value())));
            rawDrafts.add(raw);
        }
        return rawDrafts;
    }

    static EditExternalParcel toRaw(ExternalParcelUpdate update) {
        EditExternalParcel raw = new EditExternalParcel();
        raw.setVendor(EditExternalParcel.VendorEnum.fromValue(update.vendor().wireValue()));
        update.status().ifPresent(status -> raw.setStatus(editableStatus(status)));
        update.trackingNumber().ifPresent(
                trackingNumber -> raw.setTrackingNumber(new CreateExternalParcelInnerTrackingNumber(trackingNumber)));
        update.deliveryMethod().ifPresent(
                deliveryMethod -> raw.setTypeId(new CreateExternalParcelInnerTypeId(deliveryMethod.value())));
        return raw;
    }

    /**
     * External endpoints accept only the statuses a seller can set by hand — Erli derives the rest from
     * carrier tracking. Rejecting the others here names the problem better than the server's 400 would.
     */
    private static CreateExternalParcelInner.StatusEnum externalStatus(ParcelStatus status) {
        try {
            return CreateExternalParcelInner.StatusEnum.fromValue(status.wireValue());
        } catch (IllegalArgumentException notSettable) {
            throw new IllegalArgumentException(
                    "Status " + status + " cannot be set on an external parcel", notSettable);
        }
    }

    private static EditExternalParcel.StatusEnum editableStatus(ParcelStatus status) {
        try {
            return EditExternalParcel.StatusEnum.fromValue(status.wireValue());
        } catch (IllegalArgumentException notSettable) {
            throw new IllegalArgumentException(
                    "Status " + status + " cannot be set on an external parcel", notSettable);
        }
    }

    /**
     * Build the {@code _search} body. The API states {@code filter} as an {@code anyOf} over two
     * shapes — scalar comparison and membership — which the generated model exposes as two branch
     * types. The sealed {@link ParcelFilter} already guarantees the operator and value agree, so each
     * domain filter maps straight onto its matching branch.
     */
    static SearchParcels toRawSearch(ParcelFilter filter) {
        SearchParcels raw = new SearchParcels();
        raw.filter(toRawFilter(filter));
        return raw;
    }

    private static SearchParcelsFilter toRawFilter(ParcelFilter filter) {
        if (filter instanceof ParcelFilter.Comparison comparison) {
            SearchParcelsFilterAnyOf raw = new SearchParcelsFilterAnyOf();
            raw.setField(SearchParcelsFilterAnyOf.FieldEnum.fromValue(comparison.field().wireValue()));
            raw.setOperator(SearchParcelsFilterAnyOf.OperatorEnum.fromValue(comparison.operator().wireValue()));
            raw.setValue(new SearchParcelsFilterAnyOfValue(comparison.value()));
            return new SearchParcelsFilter(raw);
        }
        if (filter instanceof ParcelFilter.Membership membership) {
            SearchParcelsFilterAnyOf1 raw = new SearchParcelsFilterAnyOf1();
            raw.setField(SearchParcelsFilterAnyOf1.FieldEnum.fromValue(membership.field().wireValue()));
            raw.setOperator(SearchParcelsFilterAnyOf1.OperatorEnum.fromValue(membership.operator().wireValue()));
            raw.setValue(membership.values());
            return new SearchParcelsFilter(raw);
        }
        throw new IllegalArgumentException("Unsupported ParcelFilter type: " + filter.getClass().getName());
    }

    private static int toInt(long value) {
        return Math.toIntExact(value);
    }
}
