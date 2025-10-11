-- Truy cập vào SQL*Plus bên trong container
docker exec -it oracle-db sqlplus sys/ddinhnn11@//localhost:1521/XEPDB1 as sysdba

-- 1. Tạo user 'Spring_app' với mật khẩu 'ddinhnn11' khớp với application.yml
CREATE USER Spring_app IDENTIFIED BY ddinhnn11;

-- 2. Cấp các quyền cơ bản để kết nối và tạo tài nguyên (bảng, view,...)
-- CONNECT: Cho phép user tạo phiên kết nối (sửa lỗi ORA-01017).
-- RESOURCE: Cho phép user tạo các đối tượng như bảng, sequence, procedure...
GRANT CONNECT, RESOURCE TO Spring_app;

-- 3. Cấp hạn ngạch (quota) để user có thể lưu trữ dữ liệu
-- Nếu không có lệnh này, bạn sẽ gặp lỗi 'ORA-01950: no privileges on tablespace' khi Hibernate cố gắng tạo bảng.
ALTER USER Spring_app QUOTA UNLIMITED ON USERS;

-- Thoát khỏi SQL*Plus
EXIT;