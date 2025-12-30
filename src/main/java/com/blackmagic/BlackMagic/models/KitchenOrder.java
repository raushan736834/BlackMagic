package com.blackmagic.BlackMagic.models;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class KitchenOrder {
    private String orderId;
    private String orderCode;
    private Integer tableNumber;
    private List<KitchenItem> items;
    private String specialInstructions;
    private LocalDateTime receivedAt;
    private Integer estimatedPrepTime;
    private KitchenPriority priority;

    public enum KitchenPriority {
        LOW, NORMAL, HIGH, URGENT
    }

    @Data
    @Builder
    public static class KitchenItem {
        private String itemName;
        private Integer quantity;
        private String station; // GRILL, SALAD, DESSERT, BEVERAGE
        private String specialRequest;
    }
}
