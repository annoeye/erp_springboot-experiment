package com.anno.ERP_SpringBoot_Experiment.repository;

import com.anno.ERP_SpringBoot_Experiment.model.entity.Category;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID>, JpaSpecificationExecutor<Category> {
    Optional<Category> findCategoriesBySkuInfo_SKU(String skuInfoSKU);

    @Modifying(clearAutomatically = true)
    @Query(
            value = "DELETE FROM category " +
                    "WHERE deleted_at IS NOT NULL " +
                    "AND deleted_at < SYSDATE",
            nativeQuery = true
    )
    void deleteAllExpiredCategories();


    /**
     * Cập nhật (soft delete) một danh sách các Category bằng cách
     * set trường deletedAt và deletedBy.
     *
     * @param ids         Danh sách ID của các category cần xóa mềm
     * @param deletedBy   Tên người dùng/email thực hiện việc xóa
     * @param deletedAt   Thời điểm thực hiện việc xóa (thường là LocalDateTime.now())
     */
    @Modifying
    @Query("UPDATE Category c SET c.auditInfo.deletedAt = :deletedAt, c.auditInfo.deletedBy = :deletedBy WHERE c.id IN :ids")
    void softDeleteAllByIds(
            @Param("ids") List<UUID> ids,
            @Param("deletedBy") String deletedBy,
            @Param("deletedAt") LocalDateTime deletedAt
    );

    @Transactional
    default void softDeleteAllByIds(List<UUID> ids, String deletedBy) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        LocalDateTime deletionTime = LocalDateTime.now().plusDays(30);
        softDeleteAllByIds(ids, deletedBy, deletionTime);
    }
}
