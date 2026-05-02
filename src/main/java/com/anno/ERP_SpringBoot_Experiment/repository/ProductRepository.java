package com.anno.ERP_SpringBoot_Experiment.repository;

import com.anno.ERP_SpringBoot_Experiment.model.entity.Product;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM product " +
            "WHERE deleted_at IS NOT NULL " +
            "AND deleted_at < SYSDATE", nativeQuery = true)
    void deleteAllExpiredProducts();

    @Modifying
    @Query("UPDATE Product c SET c.auditInfo.deletedAt = :deletedAt, c.auditInfo.deletedBy = :deletedBy WHERE c.id IN :ids")
    void softDeleteAllByIds(
            @Param("ids") List<Long> ids,
            @Param("deletedBy") String deletedBy,
            @Param("deletedAt") LocalDateTime deletedAt);

    @Transactional
    default void softDeleteAllByIds(List<Long> ids, String deletedBy) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        LocalDateTime deletionTime = LocalDateTime.now().plusDays(30);
        softDeleteAllByIds(ids, deletedBy, deletionTime);
    }

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.viewCount = COALESCE(p.viewCount, 0) + 1 WHERE p.id = :id")
    void updateViewCount(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.totalSoldQuantity = COALESCE(p.totalSoldQuantity, 0) + :quantity WHERE p.id = :id")
    void updateTotalSoldQuantity(@Param("id") Long id, @Param("quantity") Integer quantity);

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.totalOrders = COALESCE(p.totalOrders, 0) + 1 WHERE p.id = :id")
    void updateTotalOrders(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("update Product p SET p.totalRevenue = COALESCE(p.totalRevenue, 0) + :price WHERE p.id = :id")
    void updateTotalRevenue(@Param("id") Long id, @Param("price") BigDecimal price);

    Optional<Product> findProductByName(String name);

    Optional<Product> findProductBySkuInfo_Sku(String skuInfoSku);
}
