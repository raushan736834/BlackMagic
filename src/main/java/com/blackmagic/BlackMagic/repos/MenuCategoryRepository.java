package com.blackmagic.BlackMagic.repos;

import com.blackmagic.BlackMagic.models.MenuCategory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuCategoryRepository extends MongoRepository<MenuCategory, String> {
    List<MenuCategory> findByActiveTrueOrderByDisplayOrder();
}