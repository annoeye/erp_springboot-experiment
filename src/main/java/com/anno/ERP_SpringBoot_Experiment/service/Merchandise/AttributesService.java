package com.anno.ERP_SpringBoot_Experiment.service.Merchandise;

import com.anno.ERP_SpringBoot_Experiment.mapper.AttributesMapper;
import com.anno.ERP_SpringBoot_Experiment.mapper.PromotionMapper;
import com.anno.ERP_SpringBoot_Experiment.mapper.SpecificationMapper;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.SkuInfo;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.SpecificationGroup;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.Specificationa;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.VariantOption;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Attributes;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Product;
import com.anno.ERP_SpringBoot_Experiment.model.enums.StockStatus;
import com.anno.ERP_SpringBoot_Experiment.repository.AttributesRepository;
import com.anno.ERP_SpringBoot_Experiment.repository.ProductRepository;
import com.anno.ERP_SpringBoot_Experiment.repository.specification.SearchCriteria;
import com.anno.ERP_SpringBoot_Experiment.repository.specification.SpecificationBuilder;
import com.anno.ERP_SpringBoot_Experiment.service.dto.AttributesDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.AttributesSearchRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CreateAttributesRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UpdateAttributesRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.VariantGroupInput;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.Response;
import com.anno.ERP_SpringBoot_Experiment.service.interfaces.iAttributes;
import com.anno.ERP_SpringBoot_Experiment.util.SecurityUtil;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.BusinessException;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class AttributesService implements iAttributes {

    private final AttributesRepository attributesRepository;
    private final ProductRepository productRepository;
    private final Helper featureMerchandiseHelper;
    private final SpecificationMapper specificationMapper;
    private final PromotionMapper promotionMapper;
    private final AttributesMapper attributesMapper;
    private final SecurityUtil securityUtil;

    @Override
    @Transactional
    public Response<List<AttributesDto>> create(@NonNull CreateAttributesRequest request) {
        if (request.getStockQuantity() < 0) {
            throw new BusinessException(ErrorCode.PRODUCT_OUT_OF_STOCK, "Số lượng tồn kho không thể là số âm.");
        }

        Product product = productRepository
                .findProductBySkuInfo_Sku(request.getProductSku())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Sản phẩm không tồn tại."));

        // Tạo tổ hợp Cartesian product N chiều từ variantGroups
        List<List<VariantOption>> combinations = generateCartesianProduct(request.getVariantGroups());

        List<Attributes> attributesList = new ArrayList<>();
        for (List<VariantOption> combination : combinations) {
            Attributes attr = Attributes.builder()
                    .product(product)
                    .sku(SkuInfo.builder()
                            .sku(new SkuInfo().createSku("attr-").getSku()
                                    .replaceFirst("-", "-" + request.getProductSku().substring(request.getProductSku().length() - 3)))
                            .build())
                    .name(request.getName())
                    .price(request.getPrice())
                    .salePrice(request.getSalePrice())
                    .stockQuantity(request.getStockQuantity())
                    .variantOptions(combination)
                    .keywords(request.getKeywords())
                    .promotions((request.getPromotions() != null && !request.getPromotions().isEmpty())
                            ? request.getPromotions().stream().map(promotionMapper::toEntity).toList()
                            : new ArrayList<>())
                    .specifications((request.getSpecifications() != null && !request.getSpecifications().isEmpty())
                            ? mapSpecificationGroups(request.getSpecifications())
                            : new ArrayList<>())
                    .statusProduct((request.getSpecifications() != null && !request.getSpecifications().isEmpty())
                            ? request.getStatusProduct()
                            : StockStatus.NOT_ACTIVE)
                    .build();

            attributesList.add(attr);
        }

        List<Attributes> savedList = attributesRepository.saveAll(attributesList);

        log.info("Đã tạo {} attributes cho sản phẩm {} | variant groups: {}",
                savedList.size(),
                product.getName(),
                request.getVariantGroups().size());

        String message = savedList.size() == 1
                ? "Tạo attributes '"+ savedList.getFirst().getName() + "' thành công."
                : String.format("Đã tạo thành công %d variants từ %d variant groups.",
                        savedList.size(), request.getVariantGroups().size());

        attributesMapper.toDto(savedList);
        return Response.ok(message);
    }

    @Override
    @Transactional
    public Response<?> update(@NonNull UpdateAttributesRequest request) {

        Attributes attributes = attributesRepository
                .findById(Long.valueOf(request.getId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.ATTRIBUTES_NOT_FOUND,
                        "Thuộc tính sản phẩm không tồn tại."));

        if (request.getName() != null && !request.getName().isBlank()) {
            attributes.setName(request.getName());
        }

        if (request.getPrice() != null) {
            if (request.getPrice() < 0) {
                throw new BusinessException(ErrorCode.INVALID_PRICE, "Giá của sản phẩm thể là số âm.");
            }
            attributes.setPrice(request.getPrice());
        }

        if (request.getSalePrice() != null) {
            if (request.getSalePrice() < 0) {
                throw new BusinessException(ErrorCode.INVALID_PRICE, "Giá khuyến mãi không thể là số âm.");
            }
            double currentPrice = request.getPrice() != null ? request.getPrice() : attributes.getPrice();
            if (request.getSalePrice() > currentPrice) {
                throw new BusinessException(ErrorCode.INVALID_PRICE, "Giá khuyến mãi không thể lớn hơn giá gốc.");
            }
            attributes.setSalePrice(request.getSalePrice());
        }

        if (request.getVariantOptions() != null && !request.getVariantOptions().isEmpty()) {
            List<VariantOption> variantOptions = request.getVariantOptions().stream()
                    .map(dto -> new VariantOption(dto.getKey(), dto.getValue()))
                    .toList();
            attributes.setVariantOptions(variantOptions);
        }

        if (request.getStockQuantity() != null) {
            if (request.getStockQuantity() < 0) {
                throw new BusinessException(ErrorCode.INVALID_QUANTITY, "Số lượng tồn kho không thể là số âm.");
            }
            attributes.setStockQuantity(request.getStockQuantity());
        }

        if (request.getKeywords() != null) {
            attributes.setKeywords(new HashSet<>(request.getKeywords()));
        }

        if (request.getSpecifications() != null) {
            attributes.setSpecifications(mapSpecificationGroups(request.getSpecifications()));
        }

        attributes.getPromotions().clear();
        attributes.getPromotions().addAll(
                promotionMapper.toEntity(request.getPromotions()));

        if (request.getStatus() != null) {
            attributes.setStatusProduct(request.getStatus());
        }

        attributesRepository.save(attributes);
        return Response.ok("Đã cập nhật thành công.");
    }

    @Override
    @Transactional
    public Response<?> delete(@NonNull List<String> ids) {
        if (ids.isEmpty()) {
            throw new BusinessException(ErrorCode.ATTRIBUTES_NOT_FOUND, "Mã định danh không được để trống.");
        }

        List<Long> attrUuids = ids.stream()
                .map(Long::valueOf)
                .toList();

        List<Attributes> attributesToDelete = attributesRepository.findAllById(attrUuids);

        if (attributesToDelete.isEmpty()) {
            throw new BusinessException(ErrorCode.ATTRIBUTES_NOT_FOUND,
                    "Không tìm thấy Danh mục với mã định danh đã cung cấp.");
        }

        if (attributesToDelete.size() != ids.size()) {
            log.warn("Một số SKU không tồn tại. Yêu cầu: {}, Tìm thấy: {}",
                    ids.size(),
                    attributesToDelete.size());
        }

        String currentUser = securityUtil.getCurrentUsername();

        // Soft delete
        attributesToDelete.forEach(attr -> {
            attr.getAuditInfo().markDeleted(currentUser);
        });

        attributesRepository.saveAll(attributesToDelete);

        log.info("Đã xóa {} attributes", attributesToDelete.size());
        return Response.noContent();
    }

    @Override
    @Transactional
    public Response<?> deleteByProduct(@NonNull String productId) {
        Product product = productRepository.findById(Long.valueOf(productId))
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Sản phẩm không tồn tại."));

        List<Attributes> attributesList = attributesRepository.findAllByProduct(product);

        if (attributesList.isEmpty()) {
            return Response.ok(null, "Sản phẩm không có danh mục nào để xóa.");
        }

        String currentUser = securityUtil.getCurrentUsername();

        attributesList.forEach(attr -> {
            attr.getAuditInfo().markDeleted(currentUser);
        });

        attributesRepository.saveAll(attributesList);

        log.info("Đã xóa {} attributes của sản phẩm {} bởi user {}",
                attributesList.size(),
                product.getName(),
                currentUser);

        return Response.ok(
                null,
                String.format("Đã xóa thành công %d danh mục của sản phẩm.", attributesList.size()));
    }

    @Override
    @Transactional
    public Page<AttributesDto> search(@NonNull AttributesSearchRequest request) {
        List<SearchCriteria> criteriaList = new ArrayList<>();

        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            criteriaList.add(new SearchCriteria("name", ":", request.getKeyword()));
        }

        if (request.getAttributesIds() != null && !request.getAttributesIds().isEmpty()) {
            List<Long> attrIds = request.getAttributesIds().stream()
                    .map(Long::valueOf)
                    .toList();
            criteriaList.add(new SearchCriteria("id", "~", attrIds));
        }

        if (request.getProductIds() != null && !request.getProductIds().isEmpty()) {
            List<Long> prodIds = request.getProductIds().stream()
                    .map(Long::valueOf)
                    .toList();
            criteriaList.add(new SearchCriteria("product.id", "~", prodIds));
        }

        if (request.getSkus() != null && !request.getSkus().isEmpty()) {
            criteriaList.add(new SearchCriteria("sku.sku", "~", request.getSkus()));
        }

        if (request.getStatuses() != null && !request.getStatuses().isEmpty()) {
            criteriaList.add(new SearchCriteria("statusProduct", "~", request.getStatuses()));
        }

        if (request.getMinPrice() != null) {
            criteriaList.add(new SearchCriteria("price", ">", request.getMinPrice()));
        }
        if (request.getMaxPrice() != null) {
            criteriaList.add(new SearchCriteria("price", "<", request.getMaxPrice()));
        }

        if (request.getMinSalePrice() != null) {
            criteriaList.add(new SearchCriteria("salePrice", ">", request.getMinSalePrice()));
        }
        if (request.getMaxSalePrice() != null) {
            criteriaList.add(new SearchCriteria("salePrice", "<", request.getMaxSalePrice()));
        }

        if (request.getMinStockQuantity() != null) {
            criteriaList.add(new SearchCriteria("stockQuantity", ">", request.getMinStockQuantity()));
        }
        if (request.getMaxStockQuantity() != null) {
            criteriaList.add(new SearchCriteria("stockQuantity", "<", request.getMaxStockQuantity()));
        }

        if (request.getMinSoldQuantity() != null) {
            criteriaList.add(new SearchCriteria("soldQuantity", ">", request.getMinSoldQuantity()));
        }
        if (request.getMaxSoldQuantity() != null) {
            criteriaList.add(new SearchCriteria("soldQuantity", "<", request.getMaxSoldQuantity()));
        }

        if (request.getMinCostPrice() != null) {
            criteriaList.add(new SearchCriteria("costPrice", ">", request.getMinCostPrice()));
        }
        if (request.getMaxCostPrice() != null) {
            criteriaList.add(new SearchCriteria("costPrice", "<", request.getMaxCostPrice()));
        }

        if (request.getCreatedBy() != null && !request.getCreatedBy().isEmpty()) {
            criteriaList.add(new SearchCriteria("auditInfo.createdBy", "~", request.getCreatedBy()));
        }

        if (request.getCreatedFrom() != null) {
            criteriaList.add(new SearchCriteria("auditInfo.createdAt", ">", request.getCreatedFrom()));
        }
        if (request.getCreatedTo() != null) {
            criteriaList.add(new SearchCriteria("auditInfo.createdAt", "<", request.getCreatedTo()));
        }
        if (request.getUpdatedFrom() != null) {
            criteriaList.add(new SearchCriteria("auditInfo.updatedAt", ">", request.getUpdatedFrom()));
        }
        if (request.getUpdatedTo() != null) {
            criteriaList.add(new SearchCriteria("auditInfo.updatedAt", "<", request.getUpdatedTo()));
        }

        SpecificationBuilder<Attributes> builder = new SpecificationBuilder<>(criteriaList);
        Specification<Attributes> spec = builder.build();

        Pageable pageable = (request.getPaging() != null) ? request.getPaging().pageable() : PageRequest.of(0, 10);

        return attributesRepository.findAll(spec, pageable)
                .map(attributesMapper::toDto);
    }

    private List<List<VariantOption>> generateCartesianProduct(List<VariantGroupInput> groups) {
        List<List<VariantOption>> result = new ArrayList<>();
        result.add(new ArrayList<>());

        for (VariantGroupInput group : groups) {
            List<List<VariantOption>> newResult = new ArrayList<>();
            for (List<VariantOption> existing : result) {
                for (String value : group.getValues()) {
                    List<VariantOption> combination = new ArrayList<>(existing);
                    combination.add(new VariantOption(group.getKey(), value));
                    newResult.add(combination);
                }
            }
            result = newResult;
        }

        return result;
    }

    /**
     * Map List<SpecificationGroupDto> → List<SpecificationGroup>
     */
    private List<SpecificationGroup> mapSpecificationGroups(
            List<com.anno.ERP_SpringBoot_Experiment.service.dto.SpecificationGroupDto> dtos) {
        if (dtos == null)
            return new ArrayList<>();
        return dtos.stream().map(dto -> SpecificationGroup.builder()
                .title(dto.getTitle())
                .items(dto.getItems() != null
                        ? dto.getItems().stream()
                                .map(item -> Specificationa.builder()
                                        .key(item.getKey())
                                        .data(item.getData())
                                        .build())
                                .toList()
                        : new ArrayList<>())
                .build()).toList();
    }

}
