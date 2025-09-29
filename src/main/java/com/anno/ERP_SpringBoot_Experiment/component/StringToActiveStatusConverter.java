package com.anno.ERP_SpringBoot_Experiment.component;

import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToActiveStatusConverter implements Converter<String, ActiveStatus> {
    @Override
    public ActiveStatus convert(String source) {
        return ActiveStatus.valueOf(source.toUpperCase());
    }
}
