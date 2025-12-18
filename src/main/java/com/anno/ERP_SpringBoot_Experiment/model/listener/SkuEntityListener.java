package com.anno.ERP_SpringBoot_Experiment.model.listener;

import com.anno.ERP_SpringBoot_Experiment.model.base.SkuAware;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.SkuInfo;
import jakarta.persistence.PrePersist;

import java.util.Random;
import java.util.stream.Collectors;

public class SkuEntityListener {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final Random RANDOM = new Random();

    @PrePersist
    public void prePersist(Object entity) {
        if (entity instanceof SkuAware skuAware) {

            SkuInfo skuInfo = skuAware.getSkuInfo();

            if (skuInfo != null && (skuInfo.getSku() == null || skuInfo.getSku().isEmpty())) {
                skuInfo.setSku(generate());
            }
        }
    }

    private String generate() {
        return RANDOM.ints(5, 0, CHARACTERS.length())
                .mapToObj(CHARACTERS::charAt)
                .map(Object::toString)
                .collect(Collectors.joining());
    }
}
