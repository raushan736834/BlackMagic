package com.blackmagic.BlackMagic.models;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Builder
@Document(collection = "payments")
public class Payment {
    @Id
    private String id;

    @Indexed
    private String orderId;

    private String gateway; // RAZORPAY, UPI, CASH
    private String method; // UPI, CARD, NETBANKING, WALLET

    @Indexed
    private String transactionId;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;

    private Double amount;

    @Indexed
    private PaymentStatus status;

    private RefundStatus refundStatus;
    private Double refundAmount;
    private String refundTransactionId;
    private LocalDateTime refundInitiatedAt;
    private LocalDateTime refundCompletedAt;

    private LocalDateTime webhookReceivedAt;
    private Integer retryCount;
    private String failureReason;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    public enum RefundStatus {
        NOT_APPLICABLE, INITIATED, PROCESSING, COMPLETED, FAILED
    }

    public enum PaymentStatus {
        INITIATED, PENDING, SUCCESS, FAILED, EXPIRED
    }
}
