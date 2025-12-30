package com.blackmagic.BlackMagic.services;

import com.blackmagic.BlackMagic.dtos.publicDtos.MenuItemDTO;
import com.blackmagic.BlackMagic.dtos.publicDtos.MenuResponse;
import com.blackmagic.BlackMagic.exception.*;
import com.blackmagic.BlackMagic.models.*;
import com.blackmagic.BlackMagic.repos.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
@Slf4j
public class MenuService {
    private final MenuCategoryRepository categoryRepository;
    private final MenuItemRepository itemRepository;

    public MenuResponse getMenu() {
        List<MenuCategory> categories = categoryRepository.findByActiveTrueOrderByDisplayOrder();

        List<MenuResponse.CategoryWithItems> categoryWithItems = categories.stream()
                .map(category -> {
                    List<MenuItem> items = itemRepository.findByCategoryIdAndAvailableTrue(category.getId());

                    List<MenuItemDTO> itemDTOs = items.stream()
                            .map(this::toDTO)
                            .collect(Collectors.toList());

                    return MenuResponse.CategoryWithItems.builder()
                            .categoryId(category.getId())
                            .name(category.getName())
                            .description(category.getDescription())
                            .items(itemDTOs)
                            .build();
                })
                .collect(Collectors.toList());

        return MenuResponse.builder()
                .categories(categoryWithItems)
                .build();
    }

    public MenuItem getMenuItem(String itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
    }

    public List<MenuItem> getPopularItems() {
        return itemRepository.findByTagsInAndAvailableTrue(Arrays.asList("popular"));
    }

    private MenuItemDTO toDTO(MenuItem item) {
        return MenuItemDTO.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .price(item.getPrice())
                .isVeg(item.getIsVeg())
                .available(item.getAvailable())
                .imageUrl(item.getImageUrl())
                .preparationTimeMinutes(item.getPreparationTimeMinutes())
                .allergens(item.getAllergens())
                .spiceLevel(item.getSpiceLevel())
                .tags(item.getTags())
                .build();
    }
}