package com.anno.ERP_SpringBoot_Experiment.service.Merchandise;

import com.anno.ERP_SpringBoot_Experiment.model.entity.Product;
import com.anno.ERP_SpringBoot_Experiment.service.interfaces.iProduct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService implements iProduct {

    @Override
    public void addProduct(Product product) {
        /*
         Chức năng set sku của sản phẩm yêu cầu có xxxxxx-name.
         xxxxxx: mã sản phẩm, name: vd: IPHONE-16 SERIES.
         1 sản phẩm sẽ chứa rất nhiều các sản phẩm cụ thể vd: IPHONE-16, IPHONE-16-PRO.
         Sẽ chứa 1 list ảnh bao gồm key và url vd: key: xxxxx, url: .....................
         Dể khi set sku cho Attributes thì sẽ đính kèm cả key ảnh vào trong nhằm mục đích
         khi bấm vào nó sẽ đổi ảnh trên web hiển thị nếu có.
         */
    }
}
