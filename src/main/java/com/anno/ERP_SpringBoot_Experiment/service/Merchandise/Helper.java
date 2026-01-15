package com.anno.ERP_SpringBoot_Experiment.service.Merchandise;

import com.anno.ERP_SpringBoot_Experiment.model.embedded.AuditInfo;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.ProductQuantity;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Attributes;
import com.anno.ERP_SpringBoot_Experiment.model.entity.ShoppingCart;
import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import com.anno.ERP_SpringBoot_Experiment.repository.AttributesRepository;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Component("featureMerchandiseHelper")
@Slf4j
@RequiredArgsConstructor
public class Helper {

    private final AttributesRepository attributesRepository;
    private static final String ALPHANUMERIC_CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    UUID convertStringToUUID(String id) {
        // 1. Kiểm tra null/empty
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID không được để trống.");
        }

        String text = id.trim();

        // 2. Tối ưu: Nếu đúng chuẩn 36 ký tự (có dấu -), thử parse ngay lập tức
        // Đỡ mất công xóa đi gộp lại
        if (text.length() == 36) {
            try {
                return UUID.fromString(text);
            } catch (IllegalArgumentException ignored) {
                // Nếu lỗi format (ví dụ dấu - đặt sai chỗ), để code chạy xuống dưới xử lý lại
            }
        }

        // 3. Xử lý trường hợp không có dấu - hoặc dấu - lung tung
        // Xóa sạch dấu gạch ngang cũ
        String raw = text.replace("-", "");

        // Check độ dài bắt buộc phải là 32 ký tự hex
        if (raw.length() != 32) {
            throw new IllegalArgumentException("Định dạng ID sai. Mong đợi 32 ký tự hex hoặc chuẩn UUID, nhận được: " + raw.length());
        }

        // 4. Chèn dấu gạch ngang vào đúng vị trí 8-4-4-4-12
        // Dùng insert của StringBuilder gọn hơn là append từng khúc
        StringBuilder sb = new StringBuilder(raw);
        sb.insert(8, "-");
        sb.insert(13, "-");
        sb.insert(18, "-");
        sb.insert(23, "-");

        try {
            return UUID.fromString(sb.toString());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("ID chứa ký tự không hợp lệ (không phải Hex).");
        }
    }

    public String generateKey() {
        StringBuilder sb = new StringBuilder(5);

        for (int i = 0; i < 5; i++) {
            int randomIndex = ThreadLocalRandom.current().nextInt(ALPHANUMERIC_CHARACTERS.length());
            sb.append(ALPHANUMERIC_CHARACTERS.charAt(randomIndex));
        }

        return sb.toString();
    }

    public void handleAddItem(ShoppingCart cart, ProductQuantity item, Attributes attributes) {
        String attributesId = item.getAttributesId();
        int quantityToAdd = item.getQuantity();

        Optional<ProductQuantity> existingItem = cart.getItems().stream()
                .filter(i -> i.getAttributesId().equals(attributesId))
                .findFirst();

        int currentQuantity = existingItem.map(ProductQuantity::getQuantity).orElse(0);
        int newTotalQuantity = currentQuantity + quantityToAdd;

        if (newTotalQuantity > attributes.getStockQuantity()) {
            throw new BusinessException(
                    String.format("Số lượng vượt quá tồn kho. Sản phẩm: %s, Tồn kho: %d, Trong giỏ: %d",
                            attributes.getName(),
                            attributes.getStockQuantity(),
                            currentQuantity)
            );
        }

        if (existingItem.isPresent()) {
            existingItem.get().setQuantity(newTotalQuantity);
            log.debug("Cập nhật số lượng sản phẩm {} từ {} lên {}",
                    attributesId, currentQuantity, newTotalQuantity);
        } else {
            cart.addItems(List.of(item));
            log.debug("Thêm mới sản phẩm {} với số lượng {}", attributesId, quantityToAdd);
        }
    }

    public void handleDecreaseItem(ShoppingCart cart, ProductQuantity item, String attributesId) {
        int quantityToDecrease = Math.abs(item.getQuantity()); // Chuyển về số dương

        Optional<ProductQuantity> existingItem = cart.getItems().stream()
                .filter(i -> i.getAttributesId().equals(attributesId))
                .findFirst();

        if (existingItem.isEmpty()) {
            throw new BusinessException("Sản phẩm " + attributesId + " không có trong giỏ hàng");
        }

        int currentQuantity = existingItem.get().getQuantity();
        int newQuantity = currentQuantity - quantityToDecrease;

        if (newQuantity <= 0) {
            cart.getItems().removeIf(i -> i.getAttributesId().equals(attributesId));
            log.debug("Xóa sản phẩm {} khỏi giỏ hàng do số lượng <= 0", attributesId);
        } else {
            existingItem.get().setQuantity(newQuantity);
            log.debug("Giảm số lượng sản phẩm {} từ {} xuống {}",
                    attributesId, currentQuantity, newQuantity);
        }
    }

    public void recalculateAndUpdateTotals(ShoppingCart cart) {
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            cart.updateTotals(0, 0.0, 0.0);
            log.debug("Giỏ hàng rỗng, reset totals về 0");
            return;
        }

        List<String> attributesIds = cart.getItems().stream()
                .map(ProductQuantity::getAttributesId)
                .toList();

        Map<String, Attributes> attributesMap = attributesRepository
                .findAllById(attributesIds.stream().map(UUID::fromString).toList())
                .stream()
                .collect(Collectors.toMap(
                        a -> a.getId().toString(),
                        a -> a
                ));

        int totalItems = cart.getItems().stream()
                .mapToInt(ProductQuantity::getQuantity)
                .sum();

        double totalPrice = cart.getItems().stream()
                .mapToDouble(item -> {
                    Attributes attributes = attributesMap.get(item.getAttributesId());
                    if (attributes != null) {
                        return attributes.getPrice() * item.getQuantity();
                    }
                    return 0.0;
                })
                .sum();

        double totalDiscount = cart.getItems().stream()
                .mapToDouble(item -> {
                    Attributes attributes = attributesMap.get(item.getAttributesId());
                    if (attributes != null && attributes.getSalePrice() > 0) {
                        return (attributes.getPrice() - attributes.getSalePrice()) * item.getQuantity();
                    }
                    return 0.0;
                })
                .sum();

        cart.updateTotals(totalItems, totalPrice, totalDiscount);
        log.debug("Đã tính toán lại totals: items={}, price={}, discount={}",
                totalItems, totalPrice, totalDiscount);
    }

    public ShoppingCart createNewCart(User user) {
        ShoppingCart cart = new ShoppingCart();
        cart.setUser(user);
        cart.setAuditInfo(new AuditInfo());
        cart.getAuditInfo().setCreatedAt(LocalDateTime.now());
        cart.getAuditInfo().setCreatedBy(user.getUsername());
        log.info("Tạo giỏ hàng mới cho user: {}", user.getUsername());
        return cart;
    }

    public String normalizeUUID(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            throw new IllegalArgumentException("UUID không được để trống");
        }

        String cleanUuid = uuid.replaceAll("-", "");

        if (cleanUuid.length() != 32) {
            throw new IllegalArgumentException("UUID phải có 32 ký tự");
        }

        return String.format("%s-%s-%s-%s-%s",
                cleanUuid.substring(0, 8),
                cleanUuid.substring(8, 12),
                cleanUuid.substring(12, 16),
                cleanUuid.substring(16, 20),
                cleanUuid.substring(20, 32)
        ).toLowerCase();
    }
}
