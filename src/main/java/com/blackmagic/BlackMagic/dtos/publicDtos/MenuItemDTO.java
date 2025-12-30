package com.blackmagic.BlackMagic.dtos.publicDtos;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class MenuItemDTO {
    private String id;
    private String name;
    private String description;
    private Double price;
    private Boolean isVeg;
    private Boolean available;
    private String imageUrl;
    private Integer preparationTimeMinutes;
    private List<String> allergens;
    private Integer spiceLevel;
    private List<String> tags;
}
