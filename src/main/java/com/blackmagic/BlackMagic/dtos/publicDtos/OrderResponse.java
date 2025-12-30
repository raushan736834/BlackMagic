package com.blackmagic.BlackMagic.dtos.publicDtos;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponse {
    private String orderId;
    private String orderCode;
    private String status;
    private String paymentStatus;
    private List<OrderItemDTO> items;
    private Double subTotal;
    private Double taxAmount;
    private Double discountAmount;
    private Double total;
    private Integer estimatedPrepTimeMinutes;
    private LocalDateTime createdAt;
    private String specialInstructions;

    @Data
    @Builder
    public static class OrderItemDTO {
        private String menuItemId;
        private String itemName;
        private Double price;
        private Integer quantity;
        private String specialRequest;
        private String status;
    }
}
