package com.blackmagic.BlackMagic.dtos.publicDtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentInitiateResponse {
    private String paymentId;
    private String razorpayOrderId;
    private String razorpayKeyId;
    private Double amount;
    private String currency;
    private String orderId;
}
