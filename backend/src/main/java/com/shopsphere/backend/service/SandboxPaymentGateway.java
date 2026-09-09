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
        String reference = (declined ? "sbx_decl_" : "sbx_") + UUID.randomUUID();
        if (declined) {
            return new Result(false, reference, "Payment declined by sandbox provider");
        }
        return new Result(true, reference, "Payment approved by sandbox provider");
    }

    @Override
    public Result verify(String providerPaymentId) {
        if (providerPaymentId == null || !providerPaymentId.startsWith("sbx_")) {
            return new Result(false, providerPaymentId, "Unknown sandbox payment reference");
        }
        return new Result(true, providerPaymentId, "Payment reference verified by sandbox provider");
    }
}
