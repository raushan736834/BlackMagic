package com.blackmagic.BlackMagic.repos;

import com.blackmagic.BlackMagic.models.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {
    Optional<Payment> findByOrderId(String orderId);

    Optional<Payment> findByTransactionId(String transactionId);

    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    List<Payment> findByStatusAndCreatedAtBefore(
            Payment.PaymentStatus status,
            LocalDateTime timestamp
    );

    List<Payment> findByRefundStatus(Payment.RefundStatus refundStatus);

    Optional<Payment> findByRefundTransactionId(String refundTransactionId);

    Optional<Payment> findByRazorpayOrderId(String orderId);
}
