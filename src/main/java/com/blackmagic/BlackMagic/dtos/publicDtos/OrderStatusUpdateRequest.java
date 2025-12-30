package com.blackmagic.BlackMagic.dtos.publicDtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrderStatusUpdateRequest {
    @NotBlank
    private String status; // IN_KITCHEN, PREPARING, READY, SERVED

    private String staffId;
}
