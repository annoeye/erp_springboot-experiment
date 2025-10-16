package com.anno.ERP_SpringBoot_Experiment.model.listener;

import com.anno.ERP_SpringBoot_Experiment.model.embedded.SkuInfo;
import jakarta.persistence.PrePersist;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.stream.Collectors;

public class SkuEntityListener {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final Random RANDOM = new Random();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyMMdd");

    @PrePersist
    public void prePersist(Object entity) {
        if (entity instanceof SkuInfo skuInfo) {
            if (skuInfo.getSKU() == null || skuInfo.getSKU().isEmpty()) {
                skuInfo.setSKU(generate());
            }
        }
    }

    private String generate() {
        String datePart = LocalDate.now().format(DATE_FORMAT);
        String randomPart = RANDOM.ints(6, 0, CHARACTERS.length())
                .mapToObj(CHARACTERS::charAt)
                .map(Object::toString)
                .collect(Collectors.joining());
        return "SKU-" + datePart + "-" + randomPart;
    }
}
