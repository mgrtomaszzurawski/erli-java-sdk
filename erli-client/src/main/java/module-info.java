/**
 * The Erli Marketplace SDK. Only {@code sdk.core} (auth, errors, retry, shared value/ID types) and
 * the {@code sdk.domain.*} facades are exported; the {@code internal} transport and the generated
 * {@code *Raw} models (a separate module) are never exposed. Consumers import {@code io.github…erli}
 * plus {@code core.*} and the domain packages they use.
 *
 * <p>This module declaration is the third shared append point: each domain bucket adds its
 * {@code exports io.github.mgrtomaszzurawski.erli.domain.<area>;} line under its reserved block so
 * appends never collide (see {@code context/FANOUT-PLAN.md}).
 */
module io.github.mgrtomaszzurawski.erli {

    requires java.net.http;
    // Internal-only dependencies (never re-exported): transport JSON and the generated Layer-1 models.
    requires com.fasterxml.jackson.databind;
    // Datatype modules the generated models need: date-time properties and JsonNullable fields.
    requires com.fasterxml.jackson.datatype.jsr310;
    requires org.openapitools.jackson.nullable;
    requires io.github.mgrtomaszzurawski.erli.rest.models;

    // Entry point.
    exports io.github.mgrtomaszzurawski.erli;

    // Core (public shared surface).
    exports io.github.mgrtomaszzurawski.erli.core.auth;
    exports io.github.mgrtomaszzurawski.erli.core.error;
    exports io.github.mgrtomaszzurawski.erli.core.model;
    exports io.github.mgrtomaszzurawski.erli.core.retry;

    // Domain facades — Core M1 ships the shop slice; buckets append theirs below.
    exports io.github.mgrtomaszzurawski.erli.domain.shop;
    // --- APPEND BLOCK: bucket A Products ---------------------------------------------------------
    // --- APPEND BLOCK: bucket B Orders ----------------------------------------------------------
    exports io.github.mgrtomaszzurawski.erli.domain.orders;
    // --- APPEND BLOCK: bucket C Shipping & Delivery ---------------------------------------------
    exports io.github.mgrtomaszzurawski.erli.domain.shipping;
    exports io.github.mgrtomaszzurawski.erli.domain.delivery;
    // --- APPEND BLOCK: bucket D Dictionaries ----------------------------------------------------
    exports io.github.mgrtomaszzurawski.erli.domain.dictionaries;
    // The attach/detach response is undeclared by the spec, so bucket D hand-writes a DTO for it and
    // Jackson binds that reflectively. Without this the two operations decode fine on the classpath
    // (so every unit test passes) and fail on the module path — the same trap payments hit below.
    // `opens` grants reflective access only: the package stays unexported and uncompilable against.
    opens io.github.mgrtomaszzurawski.erli.internal.client.dictionaries to com.fasterxml.jackson.databind;
    // --- APPEND BLOCK: bucket E Finance ---------------------------------------------------------
    exports io.github.mgrtomaszzurawski.erli.domain.billing;
    exports io.github.mgrtomaszzurawski.erli.domain.campaigns;
    exports io.github.mgrtomaszzurawski.erli.domain.commissions;
    exports io.github.mgrtomaszzurawski.erli.domain.payments;
    // The payments search body is hand-written (the generated models cannot express the live
    // contract), so unlike Layer 1 — an automatic, therefore open, module — Jackson cannot reflect
    // on it unless this package is opened. Without this, every payments search fails at runtime on
    // the module path with InaccessibleObjectException while passing on the classpath.
    // `opens` grants reflective access only: the package stays unexported and uncompilable against.
    opens io.github.mgrtomaszzurawski.erli.internal.client.payments to com.fasterxml.jackson.databind;
    // --- APPEND BLOCK: bucket F Comms & Automation ----------------------------------------------
    exports io.github.mgrtomaszzurawski.erli.domain.hooks;
    exports io.github.mgrtomaszzurawski.erli.domain.inbox;
}
