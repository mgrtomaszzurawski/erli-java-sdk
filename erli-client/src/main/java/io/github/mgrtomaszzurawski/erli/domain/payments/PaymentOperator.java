package io.github.mgrtomaszzurawski.erli.domain.payments;

/**
 * The payment provider settling the operation. Erli uses a single operator today.
 *
 * <p>Carries an {@link #UNRECOGNIZED} sentinel per the SDK's unknown-enum policy (CORE-12). The list
 * of operators is Erli's to grow, and it grows without a spec release — so a provider added after
 * this SDK's spec snapshot must not fail the read of every payment and payout the shop has. Compare
 * against the named constants; treat {@code UNRECOGNIZED} as "settled by a provider this SDK does not
 * know yet", not as an error.
 */
public enum PaymentOperator {

    /** PayU, Erli's payment operator. */
    PAYU,

    /**
     * A provider this SDK's spec snapshot does not know — or, less often, an operation the API sent
     * without an operator at all. The two are indistinguishable by the time the mapper runs, because
     * an unrecognised enum value reaches it as {@code null} (CORE-12). Everything else about the
     * operation is still mapped; only the provider's identity is unavailable.
     */
    UNRECOGNIZED
}
