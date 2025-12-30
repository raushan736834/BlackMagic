package com.blackmagic.BlackMagic.dtos.publicDtos;

import lombok.Data;

@Data
public class OrderItem {
    private String itemId;
    private String name;
    private Double price;
    private Integer qty;
    private Double lineTotal;
}
