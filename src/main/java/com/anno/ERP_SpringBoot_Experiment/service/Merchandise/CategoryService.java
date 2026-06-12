package com.anno.ERP_SpringBoot_Experiment.service.Merchandise;

import com.anno.ERP_SpringBoot_Experiment.mapper.CategoryMapper;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.AuditInfo;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.SkuInfo;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Category;
import com.anno.ERP_SpringBoot_Experiment.repository.CategoryRepository;
import com.anno.ERP_SpringBoot_Experiment.repository.specification.SearchCriteria;
import com.anno.ERP_SpringBoot_Experiment.repository.specification.SpecificationBuilder;
import com.anno.ERP_SpringBoot_Experiment.service.dto.CategoryDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CategorySearchRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UpdateCategoryRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.CategoryExitingResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.Response;
import com.anno.ERP_SpringBoot_Experiment.service.interfaces.iCategory;
import com.anno.ERP_SpringBoot_Experiment.util.SecurityUtil;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.BusinessException;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.ErrorCode;
import org.springframework.transaction.annotation.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService implements iCategory {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final SecurityUtil securityUtil;
    private final Helper featureMerchandiseHelper;
    private final org.springframework.cache.CacheManager cacheManager;

    @Override
    @CacheEvict(value = "categories", allEntries = true)
    public Response<?> create(@NonNull final String name) {
        if (categoryRepository.existsAllByName(name)) {
            throw new BusinessException(ErrorCode.CATEGORY_ALREADY_EXISTS, "Danh mục đã tồn tại.");
        }
        
        Category category = categoryRepository.save(
                Category.builder()
                        .name(name)
                        .skuInfo(new SkuInfo().createSku("ctgr-"))
                        .createdBy(securityUtil.getCurrentUsername())
                        .createdAt(LocalDateTime.now())
                        .build()
        );
        log.info("Đã tạo mới danh mục {}", name);
        
        return Response.ok("Tạo danh mục thành công.");
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public Response<?> update(final UpdateCategoryRequest request) {
        Optional<Category> optionalCategory = categoryRepository
                .findCategoryById(Long.valueOf(request.getId()));
        Category category = optionalCategory
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "Danh mục không tồn tại."));
        category.setName(request.getName());
        category.addUpdateEntry("Cập nhật tên danh mục", securityUtil.getCurrentUsername());
        log.info("Đã sửa danh mục thành {} với mã id {}", category.getName(), category.getId());
        categoryMapper.toDto(categoryRepository.save(category));
        return Response.ok("Sửa danh mục thành công.");
    }

    @Override
    @CacheEvict(value = "categories", allEntries = true)
    public Response<?> delete(@NonNull final List<String> ids) {
        List<Long> idList = ids.stream()
                .map(Long::valueOf)
                .collect(Collectors.toList());
        categoryRepository.softDeleteAllByIds(idList, securityUtil.getCurrentUsername());
        // phần get Category sẽ check và tự động xóa nếu quá 30 ngày có yêu cầu xóa
        return Response.noContent();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "#request.hashCode()")
    public Page<CategoryDto> search(@NonNull final CategorySearchRequest request) {
        categoryRepository.deleteAllExpiredCategories();

        List<SearchCriteria> list = new ArrayList<>();

        var names = featureMerchandiseHelper.filterBlank(request.getNames());
        var skus = featureMerchandiseHelper.filterBlank(request.getSkus());
        var ids = featureMerchandiseHelper.filterBlank(request.getIds()).stream()
                .map(Long::valueOf)
                .toList();

        if (!names.isEmpty()) {
            list.add(new SearchCriteria("name", "~", names));
        }
        if (!skus.isEmpty()) {
            list.add(new SearchCriteria("skuInfo.sku", "~", skus));
        }
        if (!ids.isEmpty()) {
            list.add(new SearchCriteria("id", "~", ids));
        }
        
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            list.add(new SearchCriteria("name", "~", request.getKeyword()));
        }

        if (request.getCreatedBy() != null && !request.getCreatedBy().isEmpty()) {
            list.add(new SearchCriteria("auditInfo.createdBy", "~", request.getCreatedBy()));
        }
        
        if (request.getCreatedFrom() != null) {
            list.add(new SearchCriteria("auditInfo.createdAt", ">", request.getCreatedFrom()));
        }
        if (request.getCreatedTo() != null) {
            list.add(new SearchCriteria("auditInfo.createdAt", "<", request.getCreatedTo()));
        }
        if (request.getUpdatedFrom() != null) {
            list.add(new SearchCriteria("auditInfo.updatedAt", ">", request.getUpdatedFrom()));
        }
        if (request.getUpdatedTo() != null) {
            list.add(new SearchCriteria("auditInfo.updatedAt", "<", request.getUpdatedTo()));
        }

        log.info("Search criteria list: {}", list);

        return categoryRepository.findAll(
                new SpecificationBuilder<Category>(list).build(),
                request.getPaging().pageable()).map(categoryMapper::toDto);
    }

    @Override
    public CategoryExitingResponse isExiting(String name) {
        return categoryRepository.findCategoryByName(name)
                .map(c -> CategoryExitingResponse.builder()
                        .id(String.valueOf(c.getId()))
                        .isExiting(true)
                        .build())
                .orElseGet(() -> CategoryExitingResponse.builder()
                        .id(null)
                        .isExiting(false)
                        .build());
    }

    @Override
    @Transactional(readOnly = true)
    public Response<List<CategoryDto>> getCategoriesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Response.ok(new java.util.ArrayList<>());
        }

        org.springframework.cache.Cache cache = cacheManager.getCache("categoryDetails");
        java.util.Map<Long, CategoryDto> dtoMap;

        if (cache != null) {
            @SuppressWarnings("unchecked")
            com.github.benmanes.caffeine.cache.Cache<Long, CategoryDto> nativeCache =
                    (com.github.benmanes.caffeine.cache.Cache<Long, CategoryDto>) cache.getNativeCache();

            dtoMap = nativeCache.getAll(ids, missingIds -> {
                List<Long> missingList = new java.util.ArrayList<>(missingIds);
                List<Category> categories = categoryRepository.findAllById(missingList);
                java.util.Map<Long, CategoryDto> loaded = new java.util.HashMap<>();
                for (Category c : categories) {
                    loaded.put(c.getId(), categoryMapper.toDto(c));
                }
                return loaded;
            });
        } else {
            List<Category> categories = categoryRepository.findAllById(ids);
            dtoMap = new java.util.HashMap<>();
            for (Category c : categories) {
                dtoMap.put(c.getId(), categoryMapper.toDto(c));
            }
        }

        // Reconstruct list preserving the original requested order
        List<CategoryDto> result = new java.util.ArrayList<>();
        for (Long id : ids) {
            CategoryDto dto = dtoMap.get(id);
            if (dto != null) {
                result.add(dto);
            }
        }

        return Response.ok(result);
    }

    @Override
    @Transactional(readOnly = true)
    public Response<CategoryDto> getCategoryBySku(String sku) {
        if (!org.springframework.util.StringUtils.hasText(sku)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "SKU không được để trống.");
        }
        Long id = categoryRepository.findIdBySku(sku)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "Danh mục với SKU '" + sku + "' không tồn tại."));
        List<CategoryDto> list = getCategoriesByIds(java.util.List.of(id)).getData();
        if (list.isEmpty()) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "Danh mục với SKU '" + sku + "' không tồn tại.");
        }
        return Response.ok(list.getFirst());
    }

    @Override
    @Transactional(readOnly = true)
    public Response<List<CategoryDto>> getCategoriesBySkus(List<String> skus) {
        if (skus == null || skus.isEmpty()) {
            return Response.ok(new java.util.ArrayList<>());
        }

        List<Object[]> rows = categoryRepository.findIdsAndSkusBySkus(skus);
        java.util.Map<String, Long> skuToIdMap = new java.util.HashMap<>();
        for (Object[] row : rows) {
            Long id = (Long) row[0];
            String s = (String) row[1];
            skuToIdMap.put(s, id);
        }

        List<Long> ids = new java.util.ArrayList<>();
        for (String s : skus) {
            Long id = skuToIdMap.get(s);
            if (id != null) {
                ids.add(id);
            }
        }

        List<CategoryDto> dtos = getCategoriesByIds(ids).getData();

        java.util.Map<String, CategoryDto> skuToDtoMap = new java.util.HashMap<>();
        for (CategoryDto dto : dtos) {
            if (dto.getSkuInfo() != null && dto.getSkuInfo().getSku() != null) {
                skuToDtoMap.put(dto.getSkuInfo().getSku(), dto);
            }
        }

        List<CategoryDto> result = new java.util.ArrayList<>();
        for (String s : skus) {
            CategoryDto dto = skuToDtoMap.get(s);
            if (dto != null) {
                result.add(dto);
            }
        }

        return Response.ok(result);
    }
}
