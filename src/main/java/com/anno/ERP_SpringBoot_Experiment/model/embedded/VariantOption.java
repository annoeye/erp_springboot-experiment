package com.anno.ERP_SpringBoot_Experiment.model.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantOption {
    private String name;
    private List<String> values;
}
