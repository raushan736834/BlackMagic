package com.blackmagic.BlackMagic.models;


import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Builder
@Document(collection = "menu_categories")
public class MenuCategory {
    @Id
    private String id;

    private String name;
    private String description;
    private Integer displayOrder;
    private Boolean active;
    private String imageUrl;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
