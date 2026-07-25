package io.github.mgrtomaszzurawski.erli.domain.products;

import java.util.Optional;

/**
 * A reference to a GPSR responsible person or producer registered in the shop's dictionaries, identified
 * by the seller's own external id. Used for both {@code externalResponsiblePerson} and
 * {@code externalResponsibleProducer}, which share this shape.
 *
 * @param externalId the seller-assigned id of the dictionary entry, when supplied
 * @param source     the channel that supplied the reference
 */
public record ExternalResponsibleEntity(
        Optional<String> externalId,
        Optional<ResponsibleEntitySource> source) {
}
