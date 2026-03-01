package com.anno.ERP_SpringBoot_Experiment.service.Merchandise;

import com.anno.ERP_SpringBoot_Experiment.mapper.CategoryMapper;
import com.anno.ERP_SpringBoot_Experiment.repository.specification.SearchCriteria;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.SkuInfo;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Category;
import com.anno.ERP_SpringBoot_Experiment.repository.CategoryRepository;
import com.anno.ERP_SpringBoot_Experiment.repository.specification.SpecificationBuilder;
import com.anno.ERP_SpringBoot_Experiment.service.dto.CategoryDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CategorySearchRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UpdateCategoryRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.CategoryCreateResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.CategoryExitingResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.Response;
import com.anno.ERP_SpringBoot_Experiment.service.interfaces.iCategory;
import com.anno.ERP_SpringBoot_Experiment.util.SecurityUtil;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.BusinessException;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService implements iCategory {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final SecurityUtil securityUtil;
    private final Helper featureMerchandiseHelper;

    @Override
    public Response<CategoryCreateResponse> create(@NonNull final String name) {
        if (categoryRepository.existsAllByName(name)) {
            throw new BusinessException(ErrorCode.CATEGORY_ALREADY_EXISTS, "Danh mục đã tồn tại.");
        }
        Category category = new Category();
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setSku(name.toUpperCase());
        category.setName(name);
        category.setSkuInfo(skuInfo);
        category.getAuditInfo().setCreatedBy(securityUtil.getCurrentUsername());
        category.getAuditInfo().setCreatedAt(LocalDateTime.now());
        categoryRepository.save(category);
        log.info("Đã tạo mới danh mục {}", name);
        return Response.ok(CategoryCreateResponse.builder()
                .name(category.getName())
                .id(String.valueOf(category.getId()))
                .build());
    }

    @Override
    @Transactional
    public Response<?> update(final UpdateCategoryRequest request) {
        Optional<Category> optionalCategory = categoryRepository
                .findCategoryById(featureMerchandiseHelper.convertStringToUUID(request.getId()));
        Category category = optionalCategory
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "Danh mục không tồn tại."));
        category.setName(request.getName());
        log.info("Đã sửa danh mục thành {} với mã id {}", category.getName(), category.getId());
        categoryMapper.toDto(categoryRepository.save(category));
        return Response.ok("Sửa danh mục thành công.");
    }

    @Override
    public Response<?> delete(@NonNull final List<String> ids) {
        List<UUID> uuidList = ids.stream()
                .map(featureMerchandiseHelper::convertStringToUUID)
                .collect(Collectors.toList());
        categoryRepository.softDeleteAllByIds(uuidList, securityUtil.getCurrentUsername());
        // phần get Category sẽ check và tự động xóa nếu quá 30 ngày có yêu cầu xóa
        return Response.noContent();
    }

    @Override
    @Transactional
    public Page<CategoryDto> search(@NonNull final CategorySearchRequest request) {
        categoryRepository.deleteAllExpiredCategories();

        List<SearchCriteria> list = new ArrayList<>();

        var names = featureMerchandiseHelper.filterBlank(request.getNames());
        var skus = featureMerchandiseHelper.filterBlank(request.getSkus());
        var ids = featureMerchandiseHelper.filterBlank(request.getIds()).stream()
                .map(featureMerchandiseHelper::convertStringToUUID)
                .toList();

        if (!names.isEmpty()) {
            list.add(new SearchCriteria("name", ":", names));
        }
        if (!skus.isEmpty()) {
            list.add(new SearchCriteria("skuInfo.sku", ":", skus));
        }
        if (!ids.isEmpty()) {
            list.add(new SearchCriteria("id", "~", ids));
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
}
