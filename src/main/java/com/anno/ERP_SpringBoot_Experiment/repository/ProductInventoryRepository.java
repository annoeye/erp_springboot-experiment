package com.anno.ERP_SpringBoot_Experiment.repository;

import com.anno.ERP_SpringBoot_Experiment.model.entity.ProductInventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductInventoryRepository extends JpaRepository<ProductInventory, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM ProductInventory i WHERE i.sku = :sku")
    Optional<ProductInventory> findBySkuWithLock(@Param("sku") String sku);
    
    Optional<ProductInventory> findBySku(String sku);
}
