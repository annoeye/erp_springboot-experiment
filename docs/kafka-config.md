# Kafka Configuration Report

## 1. Kiến trúc hiện tại

### 1.1 Tổng quan

```
                       ┌──────────────────────────────┐
                       │         Docker Network        │
                       │         app-network           │
                       │                              │
                       │  ┌──────────────────────┐    │
                       │  │    Kafka (KRaft)     │    │
                       │  │  apache/kafka:latest  │    │
                       │  │                      │    │
                       │  │  INTERNAL :9092      │    │
                       │  │  CONTROLLER :9094    │    │
                       │  │  EXTERNAL :9093      │    │
                       │  └──────┬──────┬───────┘    │
                       │         │      │            │
                       │         │      │            │
              ┌────────┘         │      └──────────┐
              ▼                  ▼                  ▼
     ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐
     │  Spring App  │  │  Controller  │  │  Host (local)    │
     │  (Docker)    │  │  (KRaft)     │  │  localhost:9093  │
     │  SASL_PLAIN  │  │  PLAINTEXT   │  │  SASL_PLAIN      │
     └──────────────┘  └──────────────┘  └──────────────────┘
```

### 1.2 Port mapping

| Port | Listener | Protocol | Mapped | Mục đích |
|------|----------|----------|--------|----------|
| 9092 | INTERNAL | SASL_PLAINTEXT | ❌ | Spring App trong Docker |
| 9093 | EXTERNAL | SASL_PLAINTEXT | ✅ host:9093 | Local dev từ máy host |
| 9094 | CONTROLLER | PLAINTEXT | ❌ | KRaft nội bộ (không auth) |

### 1.3 KRaft mode

Kafka từ version 2.8+ hỗ trợ KRaft mode — tự quản lý metadata, không cần Zookeeper.

```
KAFKA_NODE_ID: 1
KAFKA_PROCESS_ROLES: broker,controller
CLUSTER_ID: erp-cluster-01
KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9094
KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
```

- **broker**: xử lý produce/consume message
- **controller**: quản lý metadata (thay Zookeeper)
- Một node chạy cả 2 roles. Nhiều node có thể tách riêng.

---

## 2. Xác thực (Authentication) — SASL/PLAIN

### 2.1 Cơ chế

SASL/PLAIN là cơ chế xác thực username/password đơn giản, dùng JAAS config.

### 2.2 Cấu hình JAAS

```yaml
KAFKA_LISTENER_NAME_INTERNAL_SASL_JAAS_CONFIG: |
  org.apache.kafka.common.security.plain.PlainLoginModule required
  username="kafka"
  password="kafka@2025"
  user_kafka="kafka@2025";
```

**Giải thích:**
| Tham số | Ý nghĩa |
|---------|---------|
| `username/password` | User mặc định cho inter-broker communication |
| `user_<tên>="<pass>"` | Khai báo user cho client connect |

### 2.3 Hỗ trợ nhiều user

Thêm dòng `user_<tên>="<password>"` vào JAAS:

```yaml
KAFKA_LISTENER_NAME_INTERNAL_SASL_JAAS_CONFIG: |
  org.apache.kafka.common.security.plain.PlainLoginModule required
  username="kafka_admin"
  password="admin@2025"
  user_kafka_admin="admin@2025"
  user_kafka_app="app@2025"
  user_kafka_readonly="readonly@2025";
```

| User | Password | Mục đích |
|------|----------|----------|
| `kafka_admin` | `admin@2025` | Full quyền, tạo/xoá topic |
| `kafka_app` | `app@2025` | Spring App produce/consume |
| `kafka_readonly` | `readonly@2025` | Monitoring, consume log |

### 2.4 Hạn chế của SASL/PLAIN (quan trọng)

**SASL/PLAIN chỉ xác thực danh tính, không phân quyền.**

Sau khi đăng nhập thành công:
- `kafka_app` và `kafka_readonly` đều có thể **xoá topic**, **flush hết dữ liệu**
- Không có cơ chế chặn theo mặc định
- Tên user chỉ xuất hiện trong log, không ảnh hưởng tới hành vi

---

## 3. Phân quyền (Authorization) — Kafka ACL

### 3.1 Cách bật

Thêm vào `kafka.yml`:

```yaml
KAFKA_AUTHORIZER_CLASS_NAME: org.apache.kafka.security.authorizer.AclAuthorizer
KAFKA_SUPER_USERS: User:kafka_admin
```

Sau đó dùng `kafka-acls.sh` để thêm rule:

```bash
# kafka_app chỉ được produce/consume topic "order.*"
kafka-acls --bootstrap-server kafka:9092 \
  --add --allow-principal User:kafka_app \
  --operation Read --operation Write \
  --topic 'order.*'

# kafka_readonly chỉ được consume
kafka-acls --bootstrap-server kafka:9092 \
  --add --allow-principal User:kafka_readonly \
  --operation Read \
  --topic '*.log'
```

### 3.2 So sánh với Redis ACL

| Feature | Redis ACL | Kafka SASL/PLAIN | Kafka + ACL |
|---------|-----------|------------------|-------------|
| Xác thực | ✅ | ✅ | ✅ |
| Phân quyền đọc/ghi | ✅ | ❌ | ✅ |
| Chặn lệnh nguy hiểm | ✅ | ❌ | ❌ (chỉ chặn topic) |
| Giới hạn key pattern | ✅ | ❌ | ❌ |
| Dễ cấu hình | ✅ (file text) | ✅ (JAAS) | ❌ (phải chạy script) |

---

## 4. Các file liên quan

### 4.1 File đã sửa

```text
src/docker/
├── .env                                       # Biến môi trường
├── docker-compose.yml                         # Main compose
└── compose/
    ├── kafka.yml                              # Kafka KRaft + SASL
    └── spring-app.yml                         # Spring Kafka SASL
```

### 4.2 File local dev

```text
src/main/resources/application.yml             # Spring local config
```

---

## 5. Kết luận

1. **Local dev**: SASL/PLAIN đủ dùng — biết ai đang connect, không lộ port
2. **Cần phân quyền thật**: phải bật thêm `AclAuthorizer` + script ACL
3. **Multi-user JAAS**: chỉ cần thêm dòng `user_<tên>="<pass>"` trong JAAS config
4. **Không nên nhầm**: SASL = xác thực (who you are), ACL = phân quyền (what you can do)
