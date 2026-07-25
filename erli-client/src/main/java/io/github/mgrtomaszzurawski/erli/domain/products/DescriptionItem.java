package io.github.mgrtomaszzurawski.erli.domain.products;

import java.util.Optional;

/**
 * One block inside a {@link DescriptionSection}. A {@link DescriptionItemType#TEXT} item carries
 * {@code content}; a {@link DescriptionItemType#IMAGE} item carries {@code url}. Exactly one of the two
 * is meaningful for a given type, so both are optional here and validated on the wire by the marketplace.
 *
 * @param type    the block kind
 * @param content the text/HTML body, present for {@link DescriptionItemType#TEXT}
 * @param url     the image URL, present for {@link DescriptionItemType#IMAGE}
 */
public record DescriptionItem(DescriptionItemType type, Optional<String> content, Optional<String> url) {

    /** A text block. */
    public static DescriptionItem text(String content) {
        return new DescriptionItem(DescriptionItemType.TEXT, Optional.of(content), Optional.empty());
    }

    /** An image block. */
    public static DescriptionItem image(String url) {
        return new DescriptionItem(DescriptionItemType.IMAGE, Optional.empty(), Optional.of(url));
    }
}
