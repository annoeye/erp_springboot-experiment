package com.anno.ERP_SpringBoot_Experiment.service.Merchandise;

import com.anno.ERP_SpringBoot_Experiment.mapper.ShoppingCartMapper;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.ProductQuantity;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Attributes;
import com.anno.ERP_SpringBoot_Experiment.model.entity.ShoppingCart;
import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import com.anno.ERP_SpringBoot_Experiment.repository.AttributesRepository;
import com.anno.ERP_SpringBoot_Experiment.repository.ShoppingCartRepository;
import com.anno.ERP_SpringBoot_Experiment.repository.UserRepository;
import com.anno.ERP_SpringBoot_Experiment.service.dto.ShoppingCartDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.Response;
import com.anno.ERP_SpringBoot_Experiment.service.interfaces.iShoppingCart;
import com.anno.ERP_SpringBoot_Experiment.util.SecurityUtil;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.BusinessException;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ShoppingCartService implements iShoppingCart {

    private final ShoppingCartRepository shoppingCartRepository;
    private final AttributesRepository attributesRepository;
    private final UserRepository userRepository;
    private final SecurityUtil securityUtil;
    private final ShoppingCartMapper shoppingCartMapper;
    private final Helper helper;

    @Override
    @Transactional
    public Response<ShoppingCartDto> add(final List<ProductQuantity> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Danh sách sản phẩm không được rỗng");
        }

        String username = securityUtil.getCurrentUsername();
        User user = userRepository.findByName(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "User không tồn tại"));

        ShoppingCart cart = shoppingCartRepository.findByUser(user)
                .orElseGet(() -> helper.createNewCart(user));

        List<String> skus = items.stream()
                .map(ProductQuantity::getSku)
                .distinct()
                .toList();

        Map<String, Attributes> attributesMap = attributesRepository
                .findAllBySku_skuIn(skus)
                .stream()
                .collect(Collectors.toMap(
                        a -> a.getSku().getSku(),
                        a -> a));

        for (ProductQuantity item : items) {
            String sku = item.getSku();
            int quantity = item.getQuantity();

            Attributes attributes = attributesMap.get(sku);
            if (attributes == null) {
                throw new BusinessException(ErrorCode.ATTRIBUTES_NOT_FOUND,
                        "Sản phẩm " + sku + " không tồn tại");
            }

            if (quantity == 0) {
                cart.getItems().removeIf(i -> i.getSku().equals(sku));
                log.info("Đã xóa sản phẩm {} khỏi giỏ hàng của user {}", sku, username);

            } else if (quantity > 0) {
                helper.handleAddItem(cart, item, attributes);

            } else {
                helper.handleDecreaseItem(cart, item, sku);
            }
        }

        helper.recalculateAndUpdateTotals(cart);

        if (cart.getAuditInfo() == null) {
            cart.setAuditInfo(new com.anno.ERP_SpringBoot_Experiment.model.embedded.AuditInfo());
        }
        cart.getAuditInfo().addUpdateEntry("Cập nhật giỏ hàng", username);

        ShoppingCart savedCart = shoppingCartRepository.save(cart);
        log.info("User {} đã cập nhật giỏ hàng với {} items", username, items.size());

        return Response.ok(
                shoppingCartMapper.toDto(savedCart),
                "Cập nhật giỏ hàng thành công");
    }

    @Override
    @Transactional
    public Response<ShoppingCartDto> remove(final List<String> skus) {
        if (skus == null || skus.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Danh sách sản phẩm cần xóa không được rỗng");
        }

        String username = securityUtil.getCurrentUsername();
        User user = userRepository.findByName(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "User không tồn tại"));

        ShoppingCart cart = shoppingCartRepository.findByUser(user)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Giỏ hàng không tồn tại"));

        int removedCount = 0;
        for (String sku : skus) {
            boolean removed = cart.getItems().removeIf(
                    item -> item.getSku().equals(sku));
            if (removed) {
                removedCount++;
            }
        }

        if (removedCount == 0) {
            throw new BusinessException(ErrorCode.ATTRIBUTES_NOT_FOUND, "Không tìm thấy sản phẩm nào để xóa");
        }

        helper.recalculateAndUpdateTotals(cart);

        if (cart.getAuditInfo() == null) {
            cart.setAuditInfo(new com.anno.ERP_SpringBoot_Experiment.model.embedded.AuditInfo());
        }
        cart.getAuditInfo().addUpdateEntry("Xóa sản phẩm khỏi giỏ hàng", username);

        ShoppingCart savedCart = shoppingCartRepository.save(cart);
        log.info("User {} đã xóa {} sản phẩm khỏi giỏ hàng", username, removedCount);

        return Response.ok(
                shoppingCartMapper.toDto(savedCart),
                String.format("Đã xóa %d sản phẩm khỏi giỏ hàng", removedCount));
    }
}
