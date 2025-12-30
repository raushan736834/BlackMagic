package com.blackmagic.BlackMagic.dtos.publicDtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentInitiateRequest {
    @NotBlank
    private String orderId;

    @NotBlank
    private String method; // UPI, CARD, NETBANKING, WALLET
}
