package com.blackmagic.BlackMagic.dtos.publicDtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class OrderModifyRequest {
    @NotEmpty
    private List<OrderItemRequest> items;

    @Data
    public static class OrderItemRequest {
        @NotBlank
        private String menuItemId;

        @Min(0)
        private Integer quantity; // 0 to remove
    }
}
