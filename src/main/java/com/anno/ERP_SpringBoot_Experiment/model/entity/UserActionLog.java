package com.anno.ERP_SpringBoot_Experiment.model.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_action_logs")
@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class UserActionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Enumerated(EnumType.STRING)
    @JoinColumn(name = "action_type")
    ActionType actionType;

    @Column(name = "target_id")
    Long targetId;

    String description;

    @Column(name = "target_type")
    String targetType;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    public enum ActionType {
        // --- Đăng nhập / tài khoản ---
        LOGIN,
        LOGOUT,
        REGISTER,
        RESET_PASSWORD,
        VERIFY_EMAIL,
        CHANGE_PASSWORD,

        // --- Người dùng ---
        BLOCK_USER,
        UNBLOCK_USER,
        DELETE_USER,
        UPDATE_USER_PROFILE,
        ASSIGN_ROLE,

        // --- Hệ thống / bảo trì ---
        STOP_WORK,
        RESUME_WORK,
        SYSTEM_MAINTENANCE,
        SYSTEM_UPDATE,

        // --- Sản phẩm ---
        CREATE_PRODUCT,
        UPDATE_PRODUCT,
        DELETE_PRODUCT,
        IMPORT_PRODUCTS,
        EXPORT_PRODUCTS,

        // --- Đơn hàng / hóa đơn ---
        CREATE_ORDER,
        UPDATE_ORDER,
        CANCEL_ORDER,
        CONFIRM_ORDER,
        GENERATE_INVOICE,

        // --- Vi phạm / nhật ký ---
        HANDLE_VIOLATION,
        REPORT_USER,
        WARN_USER,
        AUTO_LOG_ENTRY,
        MANUAL_LOG_ENTRY,

        // --- Khác ---
        OTHER_ACTION;

        public String getDisplayName() {
            return switch (this) {
                case LOGIN -> "Đăng nhập";
                case LOGOUT -> "Đăng xuất";
                case REGISTER -> "Đăng ký tài khoản";
                case RESET_PASSWORD -> "Đặt lại mật khẩu";
                case VERIFY_EMAIL -> "Xác minh email";
                case CHANGE_PASSWORD -> "Đổi mật khẩu";

                case BLOCK_USER -> "Chặn người dùng";
                case UNBLOCK_USER -> "Gỡ chặn người dùng";
                case DELETE_USER -> "Xóa người dùng";
                case UPDATE_USER_PROFILE -> "Cập nhật hồ sơ người dùng";
                case ASSIGN_ROLE -> "Gán vai trò";

                case STOP_WORK -> "Dừng hoạt động";
                case RESUME_WORK -> "Tiếp tục hoạt động";
                case SYSTEM_MAINTENANCE -> "Bảo trì hệ thống";
                case SYSTEM_UPDATE -> "Cập nhật hệ thống";

                case CREATE_PRODUCT -> "Tạo sản phẩm";
                case UPDATE_PRODUCT -> "Cập nhật sản phẩm";
                case DELETE_PRODUCT -> "Xóa sản phẩm";
                case IMPORT_PRODUCTS -> "Nhập sản phẩm";
                case EXPORT_PRODUCTS -> "Xuất sản phẩm";

                case CREATE_ORDER -> "Tạo đơn hàng";
                case UPDATE_ORDER -> "Cập nhật đơn hàng";
                case CANCEL_ORDER -> "Hủy đơn hàng";
                case CONFIRM_ORDER -> "Xác nhận đơn hàng";
                case GENERATE_INVOICE -> "Tạo hóa đơn";

                case HANDLE_VIOLATION -> "Xử lý vi phạm";
                case REPORT_USER -> "Báo cáo người dùng";
                case WARN_USER -> "Cảnh cáo người dùng";

                case AUTO_LOG_ENTRY -> "Ghi nhật ký tự động";
                case MANUAL_LOG_ENTRY -> "Ghi nhật ký thủ công";

                case OTHER_ACTION -> "Hành động khác";
            };
        }
    }

}
