package com.blackmagic.BlackMagic.scheduler;

import com.blackmagic.BlackMagic.models.*;
import com.blackmagic.BlackMagic.repos.*;
import com.blackmagic.BlackMagic.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class HealthCheckScheduler {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    /**
     * Monitor system health metrics
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void checkSystemHealth() {
        try {
            // Check active orders
            long activeOrders = orderRepository.countByStatusAndCreatedAtAfter(
                    Order.OrderStatus.IN_KITCHEN,
                    LocalDateTime.now().minusHours(1)
            );

            // Check pending payments
            List<Payment> pendingPayments = paymentRepository
                    .findByStatusAndCreatedAtBefore(
                            Payment.PaymentStatus.PENDING,
                            LocalDateTime.now().minusMinutes(5)
                    );

            if (pendingPayments.size() > 10) {
                log.warn("High number of pending payments: {}", pendingPayments.size());
            }

            if (activeOrders > 50) {
                log.warn("High number of active orders: {}", activeOrders);
            }

        } catch (Exception e) {
            log.error("Health check failed", e);
        }
    }
}
