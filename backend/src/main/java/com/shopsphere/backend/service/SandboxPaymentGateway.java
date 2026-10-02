package com.shopsphere.backend.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Sandbox payment gateway (Phase 5) — the default {@link PaymentGateway}
 * implementation (active unless {@code app.payment.provider} selects another).
 * Approves charges and returns a synthetic {@code sbx_...} reference. A charge
 * is declined when the request metadata carries {@code forceFailure=true}
 * (test hook) or the environment sets {@code app.payment.sandbox.force-failure}
 * to true. No credentials are involved; a real provider implementation would
 * read them from environment configuration.
 */
@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "sandbox", matchIfMissing = true)
public class SandboxPaymentGateway implements PaymentGateway {

    static final String FORCE_FAILURE_KEY = "forceFailure";

    /** Prefix shared by every sandbox reference. */
    private static final String REFERENCE_PREFIX = "sbx_";
    /**
     * Prefix for references the sandbox declined. It deliberately extends
     * {@link #REFERENCE_PREFIX}, so {@link #verify} has to reject it explicitly
     * — a declined charge must never be reported as verified.
     */
    private static final String DECLINED_REFERENCE_PREFIX = "sbx_decl_";

    private final boolean forceFailure;

    public SandboxPaymentGateway(
            @Value("${app.payment.sandbox.force-failure:false}") boolean forceFailure) {
        this.forceFailure = forceFailure;
    }

    @Override
    public String providerName() {
        return "sandbox";
    }

    @Override
    public Result charge(Charge charge) {
        boolean declined = forceFailure
                || Boolean.parseBoolean(charge.metadata().getOrDefault(FORCE_FAILURE_KEY, "false"));
        String reference = (declined ? DECLINED_REFERENCE_PREFIX : REFERENCE_PREFIX) + UUID.randomUUID();
        if (declined) {
            return new Result(false, reference, "Payment declined by sandbox provider");
        }
        return new Result(true, reference, "Payment approved by sandbox provider");
    }

    @Override
    public Result verify(String providerPaymentId) {
        if (providerPaymentId == null || !providerPaymentId.startsWith(REFERENCE_PREFIX)) {
            return new Result(false, providerPaymentId, "Unknown sandbox payment reference");
        }
        // A declined reference also starts with the common prefix, so it has to
        // be rejected here or a failed payment would verify as successful.
        if (providerPaymentId.startsWith(DECLINED_REFERENCE_PREFIX)) {
            return new Result(false, providerPaymentId, "Payment was declined by sandbox provider");
        }
        return new Result(true, providerPaymentId, "Payment reference verified by sandbox provider");
    }
}
