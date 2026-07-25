package io.github.mgrtomaszzurawski.erli.domain.products;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * The set of product fields that are <em>frozen</em> — pinned by the seller so that an integration's
 * subsequent writes cannot overwrite them. A frozen field keeps its current value even when a
 * {@code PATCH} carries a new one, unless the write explicitly overrides the freeze.
 *
 * <p>On the wire this is an object of booleans, one per freezable field. Modelling it as a set of
 * {@link ProductField} keeps the domain honest (the two representations carry exactly the same
 * information) and lets callers ask the question they actually have — {@code frozen.isFrozen(PRICE)}
 * — instead of walking two dozen flags.
 */
public final class FrozenFields {

    private static final FrozenFields NONE = new FrozenFields(EnumSet.noneOf(ProductField.class));

    private final Set<ProductField> fields;

    private FrozenFields(Set<ProductField> fields) {
        this.fields = fields;
    }

    /** Nothing is frozen. */
    public static FrozenFields none() {
        return NONE;
    }

    /** The given fields are frozen. */
    public static FrozenFields of(Set<ProductField> fields) {
        return fields.isEmpty() ? NONE : new FrozenFields(EnumSet.copyOf(fields));
    }

    /** Whether the given field is pinned against overwrites. */
    public boolean isFrozen(ProductField field) {
        return fields.contains(field);
    }

    /** Whether any field at all is pinned. */
    public boolean isEmpty() {
        return fields.isEmpty();
    }

    /** The frozen fields, as an unmodifiable set. */
    public Set<ProductField> fields() {
        return Collections.unmodifiableSet(fields);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FrozenFields frozen && fields.equals(frozen.fields);
    }

    @Override
    public int hashCode() {
        return fields.hashCode();
    }

    @Override
    public String toString() {
        return "FrozenFields" + fields;
    }
}
