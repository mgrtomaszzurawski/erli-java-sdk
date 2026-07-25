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
    // --- APPEND BLOCK: bucket C Shipping & Delivery ---------------------------------------------
    // --- APPEND BLOCK: bucket D Dictionaries ----------------------------------------------------
    exports io.github.mgrtomaszzurawski.erli.domain.dictionaries;
    // --- APPEND BLOCK: bucket E Finance ---------------------------------------------------------
    exports io.github.mgrtomaszzurawski.erli.domain.commissions;
    // --- APPEND BLOCK: bucket F Comms & Automation ----------------------------------------------
}
