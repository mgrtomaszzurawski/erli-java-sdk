package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * A file attached to a product (for example a specification sheet). Erli carries only the URL.
 *
 * @param url the file URL
 */
public record ProductFile(String url) {
}
