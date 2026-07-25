/**
 * A modular consumer of the SDK. Its sole purpose is to fail the build if the SDK's encapsulation
 * breaks: it requires only {@code io.github.mgrtomaszzurawski.erli} and touches only the exported
 * surface. Because it compiles on the module path, referencing an {@code internal} or {@code *Raw}
 * type would not resolve — so a green compile here is the JPMS encapsulation gate.
 */
module io.github.mgrtomaszzurawski.erli.jpms.consumer {
    requires io.github.mgrtomaszzurawski.erli;
}
