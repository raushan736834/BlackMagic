package com.blackmagic.BlackMagic.models;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Document(collection = "menu_items")
public class MenuItem {
    @Id
    private String id;

    private String name;
    private String description;
    private Double price;
    private Boolean isVeg;
    private Boolean available;

    @Indexed
    private String categoryId;

    private String imageUrl;
    private Integer preparationTimeMinutes;
    private List<String> allergens;
    private Integer spiceLevel; // 0-5
    private List<String> tags; // "popular", "chef-special", etc.

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Version
    private Long version;
}

