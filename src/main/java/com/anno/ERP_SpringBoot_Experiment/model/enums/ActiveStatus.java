package com.anno.ERP_SpringBoot_Experiment.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ActiveStatus {

    // ==========================
    // 🔒 Trạng thái hoạt động
    // ==========================
    LOCKED("Ngừng hoạt động"),
    ACTIVE("Đang hoạt động"),
    INACTIVE("Chưa xác thực"),
    PASSWORD_RESET("Yêu cầu đặt lại mật khẩu"),
    EMAIL_VERIFICATION("Xác minh email"),
    TWO_FACTOR_AUTH("Xác thực hai lớp"),
    INVITATION("Thư mời"),
    ACCOUNT_UNLOCK("Mở khóa tài khoản"),
    LOGIN_VERIFICATION("Đang xác nhận tính hợp lệ của đăng nhập"),

    // ==========================
    // 🔑 Đăng nhập / Tài khoản
    // ==========================
    LOGIN("Người dùng đăng nhập"),
    LOGOUT("Người dùng đăng xuất"),
    REGISTER("Đăng ký tài khoản"),
    RESET_PASSWORD("Đặt lại mật khẩu"),
    VERIFY_EMAIL("Xác minh email"),
    CHANGE_PASSWORD("Thay đổi mật khẩu"),

    // ==========================
    // 👤 Người dùng
    // ==========================
    BLOCK_USER("Khóa người dùng"),
    GET_ALL("Lấy toàn bộ người dùng"),
    UNBLOCK_USER("Mở khóa người dùng"),
    DELETE_USER("Xóa người dùng"),
    UPDATE_USER_PROFILE("Cập nhật hồ sơ người dùng"),
    ASSIGN_ROLE("Gán vai trò cho người dùng"),

    // ==========================
    // ⚙️ Hệ thống / Bảo trì
    // ==========================
    STOP_WORK("Tạm ngừng hệ thống"),
    RESUME_WORK("Tiếp tục hệ thống"),
    SYSTEM_MAINTENANCE("Bảo trì hệ thống"),
    SYSTEM_UPDATE("Cập nhật hệ thống"),
    CREATE_USER("Tạo người dùng"), // TODO: Chưa làm

    // ==========================
    // 📦 Sản phẩm
    // ==========================
    CREATE_PRODUCT("Tạo sản phẩm"),
    UPDATE_PRODUCT("Cập nhật sản phẩm"),
    DELETE_PRODUCT("Xóa sản phẩm"),
    IMPORT_PRODUCTS("Nhập sản phẩm"),
    EXPORT_PRODUCTS("Xuất sản phẩm"),

    // ==========================
    // 🧾 Đơn hàng / Hóa đơn
    // ==========================
    CREATE_ORDER("Tạo đơn hàng"),
    UPDATE_ORDER("Cập nhật đơn hàng"),
    CANCEL_ORDER("Hủy đơn hàng"),
    CONFIRM_ORDER("Xác nhận đơn hàng"),
    GENERATE_INVOICE("Sinh hóa đơn"),

    // ==========================
    // 🚨 Vi phạm / Nhật ký
    // ==========================
    HANDLE_VIOLATION("Xử lý vi phạm"),
    REPORT_USER("Báo cáo người dùng"),
    WARN_USER("Cảnh báo người dùng"),
    AUTO_LOG_ENTRY("Tự động ghi nhật ký"),
    MANUAL_LOG_ENTRY("Thủ công ghi nhật ký"),

    // ==========================
    // 🔹 Khác
    // ==========================
    OTHER_ACTION("Hành động khác");

    private final String description;
}
