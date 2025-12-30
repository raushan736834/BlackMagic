package com.blackmagic.BlackMagic.scheduler;

import com.blackmagic.BlackMagic.models.*;
import com.blackmagic.BlackMagic.repos.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;


@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentTimeoutScheduler {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Value("${order.payment.timeout.minutes}")
    private Integer paymentTimeoutMinutes;

    /**
     * Mark expired pending payments every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    @Transactional
    public void expireStalePayments() {
        log.info("Checking for expired payments...");

        LocalDateTime expiryThreshold = LocalDateTime.now()
                .minusMinutes(paymentTimeoutMinutes);

        List<Payment> pendingPayments = paymentRepository
                .findByStatusAndCreatedAtBefore(
                        Payment.PaymentStatus.PENDING,
                        expiryThreshold
                );

        for (Payment payment : pendingPayments) {
            payment.setStatus(Payment.PaymentStatus.EXPIRED);
            paymentRepository.save(payment);

            // Optionally cancel the order
            Order order = orderRepository.findById(payment.getOrderId()).orElse(null);
            if (order != null && order.getStatus() == Order.OrderStatus.PLACED) {
                order.setStatus(Order.OrderStatus.CANCELLED);
                order.setCancellationReason("Payment timeout");
                order.setCancelledAt(LocalDateTime.now());
                orderRepository.save(order);

                log.info("Cancelled order {} due to payment timeout", order.getOrderCode());
            }
        }

        log.info("Expired {} stale payments", pendingPayments.size());
    }
}
