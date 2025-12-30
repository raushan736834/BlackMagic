package com.blackmagic.BlackMagic.dtos.publicDtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RefundStatus {
    private String status;
    private Double amount;
    private String transactionId;
}
