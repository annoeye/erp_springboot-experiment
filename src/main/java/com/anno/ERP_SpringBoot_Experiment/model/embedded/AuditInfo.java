package com.anno.ERP_SpringBoot_Experiment.model.embedded;

import com.anno.ERP_SpringBoot_Experiment.config.converter.AuditEntryListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EntityListeners;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Embeddable
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditInfo {

    /*
     * ============================ 📅 Created (khởi tạo 1 lần duy nhất)
     * ============================
     */

    /**
     * Thời điểm khởi tạo — chỉ được set 1 lần duy nhất.
     *
     * @en Creation timestamp — set only once.
     */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    /**
     * Người khởi tạo — chỉ được set 1 lần duy nhất.
     *
     * @en Created by — set only once.
     */
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    String createdBy;

    /*
     * ============================ 📝 Updated (single field + JSON history)
     * ============================
     */

    /**
     * Thời điểm chỉnh sửa gần nhất — dùng để search / index.
     *
     * @en Latest update timestamp — used for search / indexing.
     */
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    /**
     * Lịch sử chỉnh sửa chi tiết, lưu dạng JSON append-only.
     * Ví dụ DB: [{"action":"Tạo đơn hàng","updatedBy":"admin","updatedAt":"..."},
     *            {"action":"Cập nhật trạng thái vận chuyển","updatedBy":"shipper","updatedAt":"..."}]
     *
     * @en Detailed update history, stored as append-only JSON.
     */
    @Convert(converter = AuditEntryListConverter.class)
    @Column(name = "update_history", columnDefinition = "CLOB")
    @Builder.Default
    List<AuditEntry> updateHistory = new ArrayList<>();

    /*
     * ============================ 🗑️ Soft Delete
     * ============================
     */

    /**
     * Thời điểm sẽ bị xóa vĩnh viễn (hoặc đã xóa).
     * - Xóa sau 30 ngày: deletedAt = now + 30 days
     * - Xóa luôn: deletedAt = now
     *
     * @en Scheduled or immediate deletion timestamp.
     */
    @Column(name = "deleted_at")
    LocalDateTime deletedAt;

    /**
     * Người thực hiện xóa.
     *
     * @en Deleted by user.
     */
    @Column(name = "deleted_by")
    String deletedBy;

    /*
     * ============================ 🔧 Helper Methods
     * ============================
     */

    /**
     * Thêm một bản ghi chỉnh sửa mới vào lịch sử.
     * Đồng thời cập nhật updatedAt để search/index vẫn hoạt động.
     *
     * @param action    Mô tả hành động, ví dụ "Cập nhật trạng thái vận chuyển"
     * @param updatedBy Người thực hiện
     *
     * @en Append a new update entry to the history.
     * Also updates updatedAt for search/indexing.
     */
    public void addUpdateEntry(String action, String updatedBy) {
        if (this.updateHistory == null) this.updateHistory = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        this.updateHistory.add(AuditEntry.builder()
                .action(action)
                .updatedBy(updatedBy)
                .updatedAt(now)
                .build());
        this.updatedAt = now; // Đồng bộ cho search/index
    }

    /**
     * Lấy bản ghi chỉnh sửa gần nhất.
     *
     * @en Get the latest update entry.
     */
    public AuditEntry getLatestUpdate() {
        if (updateHistory == null || updateHistory.isEmpty()) return null;
        return updateHistory.getLast();
    }

    /**
     * Xóa mềm — hẹn xóa vĩnh viễn sau 30 ngày.
     *
     * @en Soft delete — schedule permanent deletion after 30 days.
     */
    public void markDeletedAfter30Days(String deletedByUser) {
        this.deletedAt = LocalDateTime.now().plusDays(30);
        this.deletedBy = deletedByUser;
    }

    /**
     * Xóa mềm — đánh dấu xóa luôn (ngay lập tức).
     *
     * @en Soft delete — mark for immediate deletion.
     */
    public void markDeletedNow(String deletedByUser) {
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedByUser;
    }

    /**
     * Khôi phục bản ghi đã xóa mềm.
     *
     * @en Restore a soft-deleted record.
     */
    public void restore() {
        this.deletedAt = null;
        this.deletedBy = null;
    }

    /**
     * Kiểm tra bản ghi đã bị đánh dấu xóa chưa.
     *
     * @en Check if the record is marked as deleted.
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }
}
