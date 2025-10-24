package com.anno.ERP_SpringBoot_Experiment.service.Merchandise;

import com.anno.ERP_SpringBoot_Experiment.mapper.CategoryMapper;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Category;
import com.anno.ERP_SpringBoot_Experiment.repository.CategoryRepository;
import com.anno.ERP_SpringBoot_Experiment.service.dto.CategoryDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CategorySearchRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.Response;
import com.anno.ERP_SpringBoot_Experiment.service.interfaces.iCategory;
import com.anno.ERP_SpringBoot_Experiment.utils.SecurityUtil;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.BusinessException;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
    public Response<String> create(@NonNull final String name) {
        Category category = new Category();
        category.setName(name);
        category.getAuditInfo().setCreatedBy(securityUtil.getCurrentUsername());
        category.getAuditInfo().setCreatedAt(LocalDateTime.now());
        categoryRepository.save(category);
        log.info("Đã tạo mới danh mục {}", name);
        return Response.ok(name);
    }

    @Override
    @Transactional
    public Response<CategoryDto> update(final CategoryDto request) { // sửa tên thôi
        Optional<Category> optionalCategory = categoryRepository.findCategoriesBySkuInfo_SKU(request.getSku());
        Category category = optionalCategory.orElseThrow(() -> new BusinessException("Danh mục không tồn tại."));
        category.setName(request.getName());
        category.getAuditInfo().setUpdatedAt(LocalDateTime.now());
        category.getAuditInfo().setUpdatedBy(securityUtil.getCurrentUsername());
        log.info("Đã sửa danh mục thành {} với mã {}", category.getName(), category.getSkuInfo().getSKU());
        return Response.ok(categoryMapper.toDto(categoryRepository.save(category)), "Sửa danh mục thành công.");
    }

    @Override
    public Response<?> delete(@NonNull final List<String> ids) { // thêm deleteBy
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
        categoryRepository.deleteAllExpiredCategories(); // Xóa tất cả cái quá hạn
        return categoryRepository.findAll(request.specification(), request.getPaging().pageable())
                .map(categoryMapper::toDto);
    }
}
