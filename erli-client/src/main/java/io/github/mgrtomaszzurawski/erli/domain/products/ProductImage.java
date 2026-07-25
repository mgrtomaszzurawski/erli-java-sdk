package io.github.mgrtomaszzurawski.erli.domain.products;

import java.util.Optional;

/**
 * A product image. Sellers supply {@code url} (plus the two role flags); the remaining fields are filled
 * in by the marketplace after it ingests and possibly transforms the image, and are read-only.
 *
 * @param url                   the image URL
 * @param isVariantImage        whether this image distinguishes this variant from its siblings
 * @param isLifestyleImage      whether this is a lifestyle (in-context) shot rather than a plain one
 * @param isFrozenImage         whether the marketplace has frozen this image against overwrites
 * @param originalExternalUrl   the URL the image was originally fetched from
 * @param appliedTransformation the transformation the marketplace applied, if any
 * @param internalUrl           the marketplace-hosted URL actually served to buyers
 */
public record ProductImage(
        String url,
        Optional<Boolean> isVariantImage,
        Optional<Boolean> isLifestyleImage,
        Optional<Boolean> isFrozenImage,
        Optional<String> originalExternalUrl,
        Optional<ImageTransformation> appliedTransformation,
        Optional<String> internalUrl) {

    /** A plain product image: neither a variant nor a lifestyle shot. */
    public static ProductImage of(String url) {
        return new ProductImage(url, Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty());
    }
}
