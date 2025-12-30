package com.blackmagic.BlackMagic.services;

import com.blackmagic.BlackMagic.dtos.publicDtos.*;
import com.blackmagic.BlackMagic.exception.*;
import com.blackmagic.BlackMagic.models.*;
import com.blackmagic.BlackMagic.repos.*;
import com.razorpay.RazorpayException;
import com.razorpay.Refund;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final RazorpayHelper razorpayHelper;

    /**
     * Process automatic refund when order is cancelled
     */
    @Transactional
    public void processAutomaticRefund(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getPaymentStatus() != Order.PaymentStatus.PAID) {
            log.info("Order {} not paid, skipping refund", order.getOrderCode());
            return;
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        try {
            processRefund(order, payment, payment.getAmount(), "Order cancelled by customer");
        } catch (Exception e) {
            log.error("Automatic refund failed for order {}", order.getOrderCode(), e);
            // Don't throw exception - order is already cancelled
        }
    }

    /**
     * Process manual refund (initiated by admin)
     */
    @Transactional
    public PaymentResponse processManualRefund(String orderId, Double amount, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        validateRefundRequest(order, payment, amount);

        return processRefund(order, payment, amount, reason);
    }

    /**
     * Get refund status
     */
    public RefundStatus getRefundStatus(String orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        if (payment.getRefundTransactionId() == null) {
            return new RefundStatus("NOT_INITIATED", null, null);
        }

        try {
            // Fetch latest status from Razorpay
            Refund refund = razorpayHelper.fetchRefund(payment.getRefundTransactionId());

            return new RefundStatus(
                    refund.get("status"),
                    payment.getRefundAmount(),
                    payment.getRefundTransactionId()
            );
        } catch (RazorpayException e) {
            log.error("Failed to fetch refund status", e);
            return new RefundStatus(
                    payment.getRefundStatus().name(),
                    payment.getRefundAmount(),
                    payment.getRefundTransactionId()
            );
        }
    }

    private void validateRefundRequest(Order order, Payment payment, Double amount) {
        if (order.getPaymentStatus() != Order.PaymentStatus.PAID) {
            throw new BusinessException("Order is not paid");
        }

        if (payment.getRefundStatus() == Payment.RefundStatus.COMPLETED) {
            throw new BusinessException("Refund already completed");
        }

        if (payment.getRefundStatus() == Payment.RefundStatus.INITIATED ||
                payment.getRefundStatus() == Payment.RefundStatus.PROCESSING) {
            throw new BusinessException("Refund already in progress");
        }

        if (amount != null && (amount <= 0 || amount > payment.getAmount())) {
            throw new BusinessException("Invalid refund amount");
        }
    }

    private PaymentResponse processRefund(Order order, Payment payment, Double amount, String reason) {
        try {
            Refund refund;
            boolean isFullRefund = amount == null || amount.equals(payment.getAmount());

            if (isFullRefund) {
                refund = razorpayHelper.processFullRefund(
                        payment.getRazorpayPaymentId(),
                        reason,
                        order.getOrderCode()
                );
                amount = payment.getAmount();
            } else {
                refund = razorpayHelper.processPartialRefund(
                        payment.getRazorpayPaymentId(),
                        (int)(amount * 100),
                        reason,
                        order.getOrderCode()
                );
            }

            // Update payment record
            payment.setRefundStatus(Payment.RefundStatus.INITIATED);
            payment.setRefundAmount(amount);
            payment.setRefundTransactionId(refund.get("id"));
            payment.setRefundInitiatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            // Update order status
            if (isFullRefund) {
                order.setPaymentStatus(Order.PaymentStatus.REFUNDED);
            } else {
                order.setPaymentStatus(Order.PaymentStatus.PARTIALLY_REFUNDED);
            }
            orderRepository.save(order);

            log.info("Refund initiated: {} for order {}, amount: {}",
                    refund.get("id"), order.getOrderCode(), amount);

            return PaymentResponse.builder()
                    .paymentId(payment.getId())
                    .status("REFUND_INITIATED")
                    .amount(amount)
                    .transactionId(refund.get("id"))
                    .paidAt(LocalDateTime.now())
                    .build();

        } catch (RazorpayException e) {
            log.error("Refund failed for order {}: {}", order.getOrderCode(), e.getMessage(), e);
            payment.setRefundStatus(Payment.RefundStatus.FAILED);
            payment.setFailureReason("Refund failed: " + e.getMessage());
            paymentRepository.save(payment);
            throw new PaymentException("Failed to process refund: " + e.getMessage());
        }
    }

    /**
     * Retry failed refund
     */
    @Transactional
    public PaymentResponse retryFailedRefund(String orderId, String reason) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        if (payment.getRefundStatus() != Payment.RefundStatus.FAILED) {
            throw new BusinessException("Refund is not in failed state");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // Reset refund status
        payment.setRefundStatus(Payment.RefundStatus.NOT_APPLICABLE);
        payment.setRefundTransactionId(null);
        payment.setFailureReason(null);
        paymentRepository.save(payment);

        return processRefund(order, payment, payment.getRefundAmount(), reason);
    }
}