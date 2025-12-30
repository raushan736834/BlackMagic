package com.blackmagic.BlackMagic.repos;

import com.blackmagic.BlackMagic.models.MenuItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MenuItemRepository extends MongoRepository<MenuItem, String> {
    List<MenuItem> findByCategoryIdAndAvailableTrue(String categoryId);

    List<MenuItem> findByAvailableTrueOrderByName();

    @Query("{ 'tags': { $in: ?0 }, 'available': true }")
    List<MenuItem> findByTagsInAndAvailableTrue(List<String> tags);

    List<MenuItem> findByIsVegAndAvailableTrue(Boolean isVeg);
}

