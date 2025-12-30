package com.blackmagic.BlackMagic.dtos.publicDtos;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private String paymentId;
    private String status;
    private Double amount;
    private String transactionId;
    private LocalDateTime paidAt;
}
