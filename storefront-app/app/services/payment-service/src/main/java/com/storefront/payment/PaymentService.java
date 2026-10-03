package com.storefront.payment;

import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    public record PaymentResult(String orderRef, String status, String providerRef, BigDecimal amount, String currency) {
        static PaymentResult from(Payment p) {
            return new PaymentResult(p.getOrderRef(), p.getStatus().name(), p.getProviderRef(), p.getAmount(), p.getCurrency());
        }
    }

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository repository;
    private final FakePaymentProvider provider;

    public PaymentService(PaymentRepository repository, FakePaymentProvider provider) {
        this.repository = repository;
        this.provider = provider;
    }

    /**
     * Idempotent by orderRef: a retried request returns the first result
     * instead of charging the customer twice.
     */
    @Transactional
    public PaymentResult charge(String orderRef, BigDecimal amount, String currency, String paymentToken) {
        return repository.findByOrderRef(orderRef).map(PaymentResult::from).orElseGet(() -> {
            FakePaymentProvider.Result result = provider.charge(orderRef, amount, currency, paymentToken);
            Payment.Status status = result.approved() ? Payment.Status.APPROVED : Payment.Status.DECLINED;
            log.info("Payment for {} {} {} -> {}", orderRef, amount, currency, status);
            return PaymentResult.from(repository.save(new Payment(orderRef, amount, currency, status, result.providerRef())));
        });
    }
}
