package com.blackmagic.BlackMagic.services;

import com.razorpay.*;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Helper class for Razorpay operations
 * Provides convenient methods for payment and refund operations
 */
@Component
@Slf4j
public class RazorpayHelper {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    /**
     * Create Razorpay client instance
     */
    public RazorpayClient getClient() throws RazorpayException {
        return new RazorpayClient(keyId, keySecret);
    }

    /**
     * Create a new order in Razorpay
     */
    public Order createOrder(String receiptId, Integer amountInPaise, String orderId, String orderCode)
            throws RazorpayException {
        RazorpayClient client = getClient();

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", receiptId);

        JSONObject notes = new JSONObject();
        notes.put("order_id", orderId);
        notes.put("order_code", orderCode);
        orderRequest.put("notes", notes);

        log.debug("Creating Razorpay order: {}", orderRequest);

        return client.orders.create(orderRequest);
    }

    /**
     * Fetch payment details from Razorpay
     */
    public Payment fetchPayment(String paymentId) throws RazorpayException {
        RazorpayClient client = getClient();
        return client.payments.fetch(paymentId);
    }

    /**
     * Process full refund
     */
    public Refund processFullRefund(String paymentId, String reason, String orderCode)
            throws RazorpayException {
        RazorpayClient client = getClient();

        JSONObject refundRequest = new JSONObject();
        refundRequest.put("payment_id", paymentId);
        // Don't specify amount for full refund - Razorpay will refund the full amount
        refundRequest.put("speed", "normal");

        JSONObject notes = new JSONObject();
        notes.put("reason", reason);
        notes.put("order_code", orderCode);
        notes.put("refund_type", "FULL");
        refundRequest.put("notes", notes);

        log.debug("Processing full refund for payment {}: {}", paymentId, refundRequest);

        return client.refunds.create(refundRequest);
    }

    /**
     * Process partial refund
     */
    public Refund processPartialRefund(String paymentId, Integer amountInPaise,
                                       String reason, String orderCode) throws RazorpayException {
        RazorpayClient client = getClient();

        JSONObject refundRequest = new JSONObject();
        refundRequest.put("payment_id", paymentId);
        refundRequest.put("amount", amountInPaise);
        refundRequest.put("speed", "normal");

        JSONObject notes = new JSONObject();
        notes.put("reason", reason);
        notes.put("order_code", orderCode);
        notes.put("refund_type", "PARTIAL");
        refundRequest.put("notes", notes);

        log.debug("Processing partial refund of {} for payment {}", amountInPaise, paymentId);

        return client.refunds.create(refundRequest);
    }

    /**
     * Fetch refund details
     */
    public Refund fetchRefund(String refundId) throws RazorpayException {
        RazorpayClient client = getClient();
        return client.refunds.fetch(refundId);
    }

    /**
     * Get all refunds for a payment
     */
    public List<Refund> getAllRefunds(String paymentId) throws RazorpayException {
        RazorpayClient client = getClient();

        JSONObject options = new JSONObject();
        options.put("payment_id", paymentId);

        return client.refunds.fetchAll(options);
    }

    /**
     * Verify payment signature
     */
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        try {
            String payload = orderId + "|" + paymentId;
            return Utils.verifyPaymentSignature(
                    new JSONObject()
                            .put("order_id", orderId)
                            .put("payment_id", paymentId)
                            .put("signature", signature),
                    keySecret
            );
        } catch (RazorpayException e) {
            log.error("Signature verification failed", e);
            return false;
        }
    }

    /**
     * Verify webhook signature
     */
    public boolean verifyWebhookSignature(String payload, String signature, String secret) {
        try {
            return Utils.verifyWebhookSignature(payload, signature, secret);
        } catch (RazorpayException e) {
            log.error("Webhook signature verification failed", e);
            return false;
        }
    }
}