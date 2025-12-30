package com.blackmagic.BlackMagic.dtos.publicDtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequest {
    @NotBlank(message = "Session code is required")
    private String sessionCode;

    @NotEmpty(message = "Order items cannot be empty")
    private List<OrderItemRequest> items;

    private String specialInstructions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequest {
        @NotBlank
        private String menuItemId;

        @Min(1)
        private Integer quantity;

        private String specialRequest;
    }
}
