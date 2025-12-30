package com.blackmagic.BlackMagic.dtos.kitchenDtos;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class KitchenOrderDTO {
    private String orderId;
    private String orderCode;
    private Integer tableNumber;
    private List<KitchenItemDTO> items;
    private String specialInstructions;
    private LocalDateTime receivedAt;
    private Integer estimatedPrepTime;
    private String priority;
    private String status;

    @Data
    @Builder
    public static class KitchenItemDTO {
        private String itemName;
        private Integer quantity;
        private String station;
        private String specialRequest;
        private String status;
    }
}
