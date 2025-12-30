package com.blackmagic.BlackMagic.services;

import com.blackmagic.BlackMagic.models.Order;
import com.blackmagic.BlackMagic.models.Payment;
import com.razorpay.*;
import com.blackmagic.BlackMagic.dtos.publicDtos.*;
import com.blackmagic.BlackMagic.dtos.kitchenDtos.*;
import com.blackmagic.BlackMagic.dtos.adminDtos.*;
import com.blackmagic.BlackMagic.dtos.publicDtos.*;
import com.blackmagic.BlackMagic.exception.*;
import com.blackmagic.BlackMagic.models.*;
import com.blackmagic.BlackMagic.repos.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    @Transactional
    public PaymentInitiateResponse initiatePayment(PaymentInitiateRequest request) {
        // Validate order
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {
            throw new BusinessException("Order already paid");
        }

        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new BusinessException("Cannot pay for cancelled order");
        }

        try {
            // Initialize Razorpay client
            RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            // Create Razorpay order
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", (int)(order.getTotal() * 100)); // Amount in paise
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", order.getOrderCode());

            JSONObject notes = new JSONObject();
            notes.put("order_id", order.getId());
            notes.put("order_code", order.getOrderCode());
            orderRequest.put("notes", notes);

            com.razorpay.Order razorpayOrder = razorpay.orders.create(orderRequest);

            // Create payment record
            Payment payment = Payment.builder()
                    .orderId(order.getId())
                    .gateway("RAZORPAY")
                    .method(request.getMethod())
                    .razorpayOrderId(razorpayOrder.get("id"))
                    .amount(order.getTotal())
                    .status(Payment.PaymentStatus.INITIATED)
                    .refundStatus(Payment.RefundStatus.NOT_APPLICABLE)
                    .retryCount(0)
                    .build();

            payment = paymentRepository.save(payment);

            log.info("Initiated payment for order {}, razorpay order: {}",
                    order.getOrderCode(), razorpayOrder.get("id"));

            return PaymentInitiateResponse.builder()
                    .paymentId(payment.getId())
                    .razorpayOrderId(razorpayOrder.get("id"))
                    .razorpayKeyId(razorpayKeyId)
                    .amount(order.getTotal())
                    .currency("INR")
                    .orderId(order.getId())
                    .build();

        } catch (RazorpayException e) {
            log.error("Razorpay error: ", e);
            throw new PaymentException("Failed to initiate payment: " + e.getMessage());
        }
    }

    @Transactional
    public PaymentResponse verifyPayment(PaymentVerificationRequest request) {
        // Find payment by razorpay payment ID
        Payment payment = paymentRepository.findByRazorpayPaymentId(request.getRazorpayPaymentId())
                .orElse(null);

        if (payment == null) {
            // Find by order ID if payment ID lookup fails
            Payment paymentByOrder = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                    .orElse(null);

            if (paymentByOrder != null) {
                payment = paymentByOrder;
            } else {
                // Create payment record if webhook was missed
                payment = Payment.builder()
                        .razorpayOrderId(request.getRazorpayOrderId())
                        .razorpayPaymentId(request.getRazorpayPaymentId())
                        .gateway("RAZORPAY")
                        .status(Payment.PaymentStatus.PENDING)
                        .refundStatus(Payment.RefundStatus.NOT_APPLICABLE)
                        .build();
            }
        }

        // Verify signature
        boolean isValid = verifyRazorpaySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        if (!isValid) {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setFailureReason("Invalid signature");
            paymentRepository.save(payment);
            throw new PaymentException("Payment verification failed");
        }

        // Update payment status
        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpaySignature(request.getRazorpaySignature());
        payment.setStatus(Payment.PaymentStatus.SUCCESS);
        payment = paymentRepository.save(payment);

        // Update order payment status
        Order order = orderRepository.findById(payment.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        order.setPaymentStatus(Order.PaymentStatus.PAID);
        orderRepository.save(order);

        log.info("Payment verified for order {}", order.getOrderCode());

        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .status(payment.getStatus().name())
                .amount(payment.getAmount())
                .transactionId(payment.getRazorpayPaymentId())
                .paidAt(LocalDateTime.now())
                .build();
    }

    @Transactional
    public void processWebhook(String signature, RazorpayWebhookPayload payload) {
        // Verify webhook signature
        if (!verifyWebhookSignature(signature, payload.toString())) {
            log.error("Invalid webhook signature");
            throw new PaymentException("Invalid webhook signature");
        }

        String event = payload.getEvent();

        if ("payment.captured".equals(event)) {
            handlePaymentCaptured(payload);
        } else if ("payment.failed".equals(event)) {
            handlePaymentFailed(payload);
        } else if ("refund.processed".equals(event)) {
            handleRefundProcessed(payload);
        } else if ("refund.failed".equals(event)) {
            handleRefundFailed(payload);
        }
    }

    @Transactional
    public PaymentResponse initiateRefund(String orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getPaymentStatus() != Order.PaymentStatus.PAID) {
            throw new BusinessException("Order is not paid");
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        if (payment.getRefundStatus() == Payment.RefundStatus.COMPLETED) {
            throw new BusinessException("Refund already processed");
        }

        if (payment.getRefundStatus() == Payment.RefundStatus.INITIATED ||
                payment.getRefundStatus() == Payment.RefundStatus.PROCESSING) {
            throw new BusinessException("Refund already in progress");
        }

        try {
            RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            // ✅ CORRECT: Create refund request with payment_id
            JSONObject refundRequest = new JSONObject();
            refundRequest.put("payment_id", payment.getRazorpayPaymentId());
            refundRequest.put("amount", (int)(payment.getAmount() * 100)); // Amount in paise
            refundRequest.put("speed", "normal"); // or "optimum"

            JSONObject notes = new JSONObject();
            notes.put("reason", reason);
            notes.put("order_code", order.getOrderCode());
            notes.put("refund_initiated_by", "SYSTEM");
            refundRequest.put("notes", notes);

            // ✅ CORRECT: Use client.refunds.create() instead of payment.refund()
            Refund refund = razorpay.refunds.create(refundRequest);

            // Update payment record
            payment.setRefundStatus(Payment.RefundStatus.INITIATED);
            payment.setRefundAmount(payment.getAmount());
            payment.setRefundTransactionId(refund.get("id"));
            payment.setRefundInitiatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            // Update order status
            order.setPaymentStatus(Order.PaymentStatus.REFUNDED);
            orderRepository.save(order);

            log.info("Initiated refund for order {}, refund ID: {}",
                    order.getOrderCode(), refund.get("id"));

            return PaymentResponse.builder()
                    .paymentId(payment.getId())
                    .status("REFUND_INITIATED")
                    .amount(payment.getRefundAmount())
                    .transactionId(payment.getRefundTransactionId())
                    .paidAt(payment.getRefundInitiatedAt())
                    .build();

        } catch (RazorpayException e) {
            log.error("Refund failed for order {}: {}", order.getOrderCode(), e.getMessage(), e);
            payment.setRefundStatus(Payment.RefundStatus.FAILED);
            payment.setFailureReason("Refund failed: " + e.getMessage());
            paymentRepository.save(payment);
            throw new PaymentException("Failed to initiate refund: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during refund for order {}", order.getOrderCode(), e);
            payment.setRefundStatus(Payment.RefundStatus.FAILED);
            payment.setFailureReason("Unexpected error: " + e.getMessage());
            paymentRepository.save(payment);
            throw new PaymentException("Failed to initiate refund: " + e.getMessage());
        }
    }

    @Transactional
    public PaymentResponse initiatePartialRefund(String orderId, Double refundAmount, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getPaymentStatus() != Order.PaymentStatus.PAID) {
            throw new BusinessException("Order is not paid");
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        if (refundAmount <= 0 || refundAmount > payment.getAmount()) {
            throw new BusinessException("Invalid refund amount");
        }

        try {
            RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            // ✅ CORRECT: Create partial refund request with payment_id
            JSONObject refundRequest = new JSONObject();
            refundRequest.put("payment_id", payment.getRazorpayPaymentId());
            refundRequest.put("amount", (int)(refundAmount * 100)); // Partial amount in paise
            refundRequest.put("speed", "normal");

            JSONObject notes = new JSONObject();
            notes.put("reason", reason);
            notes.put("order_code", order.getOrderCode());
            notes.put("refund_type", "PARTIAL");
            refundRequest.put("notes", notes);

            // ✅ CORRECT: Use client.refunds.create()
            Refund refund = razorpay.refunds.create(refundRequest);

            payment.setRefundStatus(Payment.RefundStatus.INITIATED);
            payment.setRefundAmount(refundAmount);
            payment.setRefundTransactionId(refund.get("id"));
            payment.setRefundInitiatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            // Update order to partially refunded
            order.setPaymentStatus(Order.PaymentStatus.PARTIALLY_REFUNDED);
            orderRepository.save(order);

            log.info("Initiated partial refund of {} for order {}, refund ID: {}",
                    refundAmount, order.getOrderCode(), refund.get("id"));

            return PaymentResponse.builder()
                    .paymentId(payment.getId())
                    .status("PARTIAL_REFUND_INITIATED")
                    .amount(refundAmount)
                    .transactionId(payment.getRefundTransactionId())
                    .paidAt(payment.getRefundInitiatedAt())
                    .build();

        } catch (RazorpayException e) {
            log.error("Partial refund failed for order {}: {}", order.getOrderCode(), e.getMessage(), e);
            payment.setRefundStatus(Payment.RefundStatus.FAILED);
            payment.setFailureReason("Partial refund failed: " + e.getMessage());
            paymentRepository.save(payment);
            throw new PaymentException("Failed to initiate partial refund: " + e.getMessage());
        }
    }

    private void handlePaymentCaptured(RazorpayWebhookPayload payload) {
        String paymentId = payload.getPayload().getPayment().getEntity().getId();
        String orderId = payload.getPayload().getPayment().getEntity().getOrder_id();

        Payment payment = paymentRepository.findByRazorpayOrderId(orderId)
                .orElse(null);

        if (payment != null) {
            payment.setRazorpayPaymentId(paymentId);
            payment.setStatus(Payment.PaymentStatus.SUCCESS);
            payment.setWebhookReceivedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            // Update order
            Order order = orderRepository.findById(payment.getOrderId()).orElse(null);
            if (order != null) {
                order.setPaymentStatus(Order.PaymentStatus.PAID);
                orderRepository.save(order);
            }

            log.info("Payment captured via webhook: {}", paymentId);
        }
    }

    private void handlePaymentFailed(RazorpayWebhookPayload payload) {
        String orderId = payload.getPayload().getPayment().getEntity().getOrder_id();

        Payment payment = paymentRepository.findByRazorpayOrderId(orderId)
                .orElse(null);

        if (payment != null) {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setWebhookReceivedAt(LocalDateTime.now());
            payment.setRetryCount(payment.getRetryCount() + 1);
            paymentRepository.save(payment);

            log.warn("Payment failed via webhook for order: {}", orderId);
        }
    }

    private void handleRefundProcessed(RazorpayWebhookPayload payload) {
        try {
            // Extract refund details from webhook payload
            String refundId = payload.getPayload().getPayment().getEntity().getId();

            Payment payment = paymentRepository.findByRefundTransactionId(refundId)
                    .orElse(null);

            if (payment != null) {
                payment.setRefundStatus(Payment.RefundStatus.COMPLETED);
                payment.setRefundCompletedAt(LocalDateTime.now());
                payment.setWebhookReceivedAt(LocalDateTime.now());
                paymentRepository.save(payment);

                // Update order status
                Order order = orderRepository.findById(payment.getOrderId()).orElse(null);
                if (order != null && order.getPaymentStatus() == Order.PaymentStatus.REFUNDED) {
                    // Order already marked as refunded, just log
                    log.info("Refund completed via webhook for order: {}", order.getOrderCode());
                }

                log.info("Refund processed via webhook: {}", refundId);
            }
        } catch (Exception e) {
            log.error("Error processing refund webhook", e);
        }
    }

    private void handleRefundFailed(RazorpayWebhookPayload payload) {
        try {
            String refundId = payload.getPayload().getPayment().getEntity().getId();

            Payment payment = paymentRepository.findByRefundTransactionId(refundId)
                    .orElse(null);

            if (payment != null) {
                payment.setRefundStatus(Payment.RefundStatus.FAILED);
                payment.setWebhookReceivedAt(LocalDateTime.now());
                payment.setFailureReason("Refund failed - webhook notification");
                paymentRepository.save(payment);

                log.error("Refund failed via webhook: {}", refundId);
            }
        } catch (Exception e) {
            log.error("Error processing refund failed webhook", e);
        }
    }

    private boolean verifyRazorpaySignature(String orderId, String paymentId, String signature) {
        try {
            String payload = orderId + "|" + paymentId;
            String generatedSignature = calculateHMAC(payload, razorpayKeySecret);
            return generatedSignature.equals(signature);
        } catch (Exception e) {
            log.error("Signature verification failed: ", e);
            return false;
        }
    }

    private boolean verifyWebhookSignature(String signature, String payload) {
        try {
            String generatedSignature = calculateHMAC(payload, webhookSecret);
            return generatedSignature.equals(signature);
        } catch (Exception e) {
            log.error("Webhook signature verification failed: ", e);
            return false;
        }
    }

    private String calculateHMAC(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes());
        return bytesToHex(hash);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}