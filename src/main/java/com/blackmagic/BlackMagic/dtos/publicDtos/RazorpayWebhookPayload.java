package com.blackmagic.BlackMagic.dtos.publicDtos;

import lombok.Data;

@Data
public class RazorpayWebhookPayload {
    private String event;
    private PaymentEntity payload;

    @Data
    public static class PaymentEntity {
        private Payment payment;

        @Data
        public static class Payment {
            private Entity entity;

            @Data
            public static class Entity {
                private String id;
                private String order_id;
                private String status;
                private Integer amount;
                private String method;
            }
        }
    }
}
