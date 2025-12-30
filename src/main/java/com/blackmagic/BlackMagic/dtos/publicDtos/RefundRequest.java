package com.blackmagic.BlackMagic.dtos.publicDtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefundRequest {
    @NotBlank(message = "Reason is required")
    private String reason;

    private Double refundAmount; // If null, full refund
}
