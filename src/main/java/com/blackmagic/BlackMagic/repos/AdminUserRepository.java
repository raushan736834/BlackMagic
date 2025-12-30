package com.blackmagic.BlackMagic.repos;

import com.blackmagic.BlackMagic.models.AdminUser;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdminUserRepository extends MongoRepository<AdminUser, String> {
    Optional<AdminUser> findByUsername(String username);

    Optional<AdminUser> findByEmail(String email);

    List<AdminUser> findByRoleAndActiveTrue(AdminUser.UserRole role);

    List<AdminUser> findByActiveTrue();

    @Query("{ 'assignedTables': { $in: [?0] }, 'active': true }")
    List<AdminUser> findByAssignedTablesContaining(String tableId);
}
