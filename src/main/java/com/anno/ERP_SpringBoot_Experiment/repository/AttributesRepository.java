
package com.anno.ERP_SpringBoot_Experiment.repository;

import com.anno.ERP_SpringBoot_Experiment.model.entity.Attributes;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Product;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttributesRepository extends JpaRepository<Attributes, Long>, JpaSpecificationExecutor<Attributes> {

        Optional<Attributes> findAttributesBySku_sku(String skuSku);

        Optional<Attributes> findAttributesBySku_skuAndAuditInfo_DeletedAtIsNull(String skuSku);

        Optional<Attributes> findAttributesById(Long id);

        @Query("SELECT a FROM Attributes a WHERE a.id IN :ids")
        List<Attributes> getQuantityAttributesById(@Param("ids") List<Long> ids);

        List<Attributes> findAllByProduct(Product product);

        List<Attributes> findAllByProductAndAuditInfo_DeletedAtIsNull(Product product);

        List<Attributes> findAllByProduct_Id(Long productId);

        List<Attributes> findAllByProduct_IdAndAuditInfo_DeletedAtIsNull(Long productId);

        boolean existsBySku_sku(String skuSku);

        long countByProduct(Product product);

        long countByProductAndAuditInfo_DeletedAtIsNull(Product product);

        List<Attributes> findAllByNameContainingIgnoreCaseAndAuditInfo_DeletedAtIsNull(String name);

        List<Attributes> findAllByPriceBetweenAndAuditInfo_DeletedAtIsNull(Double minPrice, Double maxPrice);

        @Query("SELECT a FROM Attributes a WHERE a.stockQuantity < :threshold AND a.auditInfo.deletedAt IS NULL")
        List<Attributes> findLowStockAttributes(@Param("threshold") Integer threshold);

        @Query("SELECT a FROM Attributes a WHERE a.salePrice IS NOT NULL AND a.salePrice > 0 AND a.auditInfo.deletedAt IS NULL")
        List<Attributes> findAllOnSale();

        @Modifying(clearAutomatically = true)
        @Query(value = "DELETE FROM attributes " +
                        "WHERE deleted_at IS NOT NULL " +
                        "AND deleted_at < SYSDATE", nativeQuery = true)
        void deleteAllExpiredAttributes();

        @Modifying
        @Query("UPDATE Attributes a SET a.stockQuantity = :quantity, a.auditInfo.updatedAt = CURRENT_TIMESTAMP WHERE a.sku.sku = :sku")
        void updateStockQuantity(@Param("sku") String sku, @Param("quantity") Integer quantity);

        @Modifying
        @Query("UPDATE Attributes a SET a.stockQuantity = a.stockQuantity + :amount, a.auditInfo.updatedAt = CURRENT_TIMESTAMP WHERE a.sku.sku = :sku")
        void increaseStockQuantity(@Param("sku") String sku, @Param("amount") Integer amount);

        @Modifying
        @Query("UPDATE Attributes a SET a.stockQuantity = a.stockQuantity - :amount, a.auditInfo.updatedAt = CURRENT_TIMESTAMP WHERE a.sku.sku = :sku AND a.stockQuantity >= :amount")
        int decreaseStockQuantity(@Param("sku") String sku, @Param("amount") Integer amount);

        @Modifying
        @Query("UPDATE Attributes a SET a.salePrice = :salePrice, a.auditInfo.updatedAt = CURRENT_TIMESTAMP WHERE a.sku.sku = :sku")
        void updateSalePrice(@Param("sku") String sku, @Param("salePrice") Double salePrice);

        @Modifying
        @Transactional
        @Query("UPDATE Attributes a SET a.soldQuantity = COALESCE(a.soldQuantity, 0) + :quantity WHERE a.id = :id")
        void updateSoldQuantity(@Param("id") Long id, @Param("quantity") Integer quantity);

        @Modifying
        @Transactional
        @Query("UPDATE Attributes a SET a.totalOrders = COALESCE(a.totalOrders, 0) + 1 WHERE a.id = :id")
        void updateTotalOrders(@Param("id") Long id);

}
