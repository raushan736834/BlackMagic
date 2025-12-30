package com.blackmagic.BlackMagic.repos;


import com.blackmagic.BlackMagic.models.Table;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TableRepository extends MongoRepository<Table, String> {
    Optional<Table> findByQrToken(String qrToken);
    Optional<Table> findByTableNumber(Integer tableNumber);
    List<Table> findByActiveTrue();
    List<Table> findByLocationAndActiveTrue(String location);
}