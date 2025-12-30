package com.blackmagic.BlackMagic.dtos.publicDtos;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class MenuResponse {
    private List<CategoryWithItems> categories;

    @Data
    @Builder
    public static class CategoryWithItems {
        private String categoryId;
        private String name;
        private String description;
        private List<MenuItemDTO> items;
    }
}
