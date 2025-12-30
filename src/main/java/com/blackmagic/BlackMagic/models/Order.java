package com.blackmagic.BlackMagic.models;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Document(collection = "orders")
public class Order {
    @Id
    private String id;

    @Indexed
    private String sessionId;

    @Indexed(unique = true)
    private String orderCode; // e.g., "ORD-20250101-001"

    @Indexed
    private OrderStatus status;

    @Indexed
    private PaymentStatus paymentStatus;

    private List<OrderItem> items;
    private String specialInstructions;

    private Double subTotal;
    private Double taxAmount;
    private Double discountAmount;
    private Double deliveryCharge;
    private Double total;

    private String assignedToStaffId;
    private Integer estimatedPrepTimeMinutes;

    @Indexed
    private LocalDateTime createdAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime readyAt;
    private LocalDateTime servedAt;
    private LocalDateTime cancelledAt;

    private String cancellationReason;
    private List<OrderModification> modifications;

    @Version
    private Long version;

    public enum OrderStatus {
        PLACED, IN_KITCHEN, PREPARING, READY, SERVED, CANCELLED, REJECTED
    }

    public enum PaymentStatus {
        PENDING, PAID, FAILED, REFUNDED, PARTIALLY_REFUNDED
    }

    @Data
    @Builder
    public static class OrderItem {
        private String menuItemId;
        private String itemName;
        private Double price;
        private Integer quantity;
        private String specialRequest;
        private ItemStatus status;

        public enum ItemStatus {
            PENDING, PREPARING, READY, CANCELLED
        }
    }

    @Data
    @Builder
    public static class OrderModification {
        private LocalDateTime timestamp;
        private String modifiedBy;
        private String modificationType; // ADD_ITEM, REMOVE_ITEM, CANCEL_ORDER
        private String details;
    }
}