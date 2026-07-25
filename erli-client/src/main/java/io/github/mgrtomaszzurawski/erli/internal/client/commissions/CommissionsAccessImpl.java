package io.github.mgrtomaszzurawski.erli.internal.client.commissions;

import io.github.mgrtomaszzurawski.erli.domain.commissions.CommissionEstimate;
import io.github.mgrtomaszzurawski.erli.domain.commissions.CommissionEstimateRequest;
import io.github.mgrtomaszzurawski.erli.domain.commissions.CommissionsAccess;
import io.github.mgrtomaszzurawski.erli.internal.ApiPaths;
import io.github.mgrtomaszzurawski.erli.internal.HttpRuntime;
import io.github.mgrtomaszzurawski.erli.rest.model.EstimateCommissionResponse;

import java.util.Objects;

/**
 * {@link CommissionsAccess} implementation over the shared {@link HttpRuntime}. Internal.
 */
public final class CommissionsAccessImpl implements CommissionsAccess {

    private final HttpRuntime runtime;

    public CommissionsAccessImpl(HttpRuntime runtime) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
    }

    @Override
    public CommissionEstimate estimate(CommissionEstimateRequest request) {
        EstimateCommissionResponse raw = runtime.post(ApiPaths.COMMISSIONS_ESTIMATE, CommissionMapper.toRaw(request), EstimateCommissionResponse.class);
        return CommissionMapper.toDomain(raw);
    }
}
