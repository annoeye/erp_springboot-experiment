package com.anno.ERP_SpringBoot_Experiment.service.Merchandise;

import com.anno.ERP_SpringBoot_Experiment.model.entity.Category;
import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import com.anno.ERP_SpringBoot_Experiment.repository.CategoryRepository;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.updateCategoryRequest;
import com.anno.ERP_SpringBoot_Experiment.service.interfaces.iCategory;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.BusinessException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService implements iCategory {
    
    private final CategoryRepository categoryRepository;

    @Override
    public void create(String name) {
        Category category = new Category();
        category.setName(name);
        category.setStatus(ActiveStatus.ACTIVE);
        categoryRepository.save(category);
        log.info("Đã tạo mới danh mục {}", name);
    }

    @Override
    @Transactional //Response<String>
    public void update(updateCategoryRequest request) {
        Optional<Category> optionalCategory = categoryRepository.findCategoryBySku_SKU(request.getSku());
        if (optionalCategory.isPresent()) {
            Category category = optionalCategory.get();
            category.setName(request.getName());
            categoryRepository.save(category);
            log.info("Đã sửa danh mục thành {}", category.getName());
            // ghi log
        } else throw new BusinessException("Danh mục không tồn tại.");
    }

    @Override
    @Transactional
    public void delete(String sku) {
        Optional<Category> optionalCategory = categoryRepository.findCategoryBySku_SKU(sku);
        if (optionalCategory.isPresent()) {
            Category category = optionalCategory.get();
            category.setStatus(ActiveStatus.DELETE_CATEGORY);
            log.info("Đã chuyển danh mục {} vào thùng rác. Sẽ tự xóa sau 30 ngày", category.getName());
            // ghi log
        } else throw new BusinessException("Danh mục không tồn tại.");
    }
}
