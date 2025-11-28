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
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.Response;
import com.anno.ERP_SpringBoot_Experiment.service.interfaces.iShoppingCart;
import com.anno.ERP_SpringBoot_Experiment.utils.SecurityUtil;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
            throw new BusinessException("Danh sách sản phẩm không được rỗng");
        }

        String username = securityUtil.getCurrentUsername();
        User user = userRepository.findByName(username)
                .orElseThrow(() -> new BusinessException("User không tồn tại"));

        ShoppingCart cart = shoppingCartRepository.findByUser(user)
                .orElseGet(() -> helper.createNewCart(user));

        List<String> attributesIds = items.stream()
                .map(ProductQuantity::getAttributesId)
                .distinct()
                .toList();

        Map<String, Attributes> attributesMap = attributesRepository
                .findAllById(attributesIds.stream().map(UUID::fromString).toList())
                .stream()
                .collect(Collectors.toMap(
                        a -> a.getId().toString(),
                        a -> a
                ));

        for (ProductQuantity item : items) {
            String attributesId = item.getAttributesId();
            int quantity = item.getQuantity();

            Attributes attributes = attributesMap.get(attributesId);
            if (attributes == null) {
                throw new BusinessException("Sản phẩm " + attributesId + " không tồn tại");
            }

            if (quantity == 0) {
                cart.getItems().removeIf(i -> i.getAttributesId().equals(attributesId));
                log.info("Đã xóa sản phẩm {} khỏi giỏ hàng của user {}", attributesId, username);
                
            } else if (quantity > 0) {
                helper.handleAddItem(cart, item, attributes);
                
            } else {
                helper.handleDecreaseItem(cart, item, attributesId);
            }
        }

        helper.recalculateAndUpdateTotals(cart);

        cart.getAuditInfo().setUpdatedAt(LocalDateTime.now());
        cart.getAuditInfo().setUpdatedBy(username);

        ShoppingCart savedCart = shoppingCartRepository.save(cart);
        log.info("User {} đã cập nhật giỏ hàng với {} items", username, items.size());

        return Response.ok(
                shoppingCartMapper.toDto(savedCart),
                "Cập nhật giỏ hàng thành công"
        );
    }

    @Override
    @Transactional
    public Response<ShoppingCartDto> remove(final List<String> attributesIds) {
        if (attributesIds == null || attributesIds.isEmpty()) {
            throw new BusinessException("Danh sách sản phẩm cần xóa không được rỗng");
        }

        String username = securityUtil.getCurrentUsername();
        User user = userRepository.findByName(username)
                .orElseThrow(() -> new BusinessException("User không tồn tại"));

        ShoppingCart cart = shoppingCartRepository.findByUser(user)
                .orElseThrow(() -> new BusinessException("Giỏ hàng không tồn tại"));

        int removedCount = 0;
        for (String attributesId : attributesIds) {
            boolean removed = cart.getItems().removeIf(
                    item -> item.getAttributesId().equals(attributesId)
            );
            if (removed) {
                removedCount++;
            }
        }

        if (removedCount == 0) {
            throw new BusinessException("Không tìm thấy sản phẩm nào để xóa");
        }

        helper.recalculateAndUpdateTotals(cart);

        cart.getAuditInfo().setUpdatedAt(LocalDateTime.now());
        cart.getAuditInfo().setUpdatedBy(username);

        ShoppingCart savedCart = shoppingCartRepository.save(cart);
        log.info("User {} đã xóa {} sản phẩm khỏi giỏ hàng", username, removedCount);

        return Response.ok(
                shoppingCartMapper.toDto(savedCart),
                String.format("Đã xóa %d sản phẩm khỏi giỏ hàng", removedCount)
        );
    }
}
