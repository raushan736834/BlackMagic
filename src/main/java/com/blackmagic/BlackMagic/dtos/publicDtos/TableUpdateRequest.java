package com.blackmagic.BlackMagic.dtos.publicDtos;

import lombok.Data;

@Data
public class TableUpdateRequest {
    private Boolean active;
    private Integer capacity;
    private String location;
}
