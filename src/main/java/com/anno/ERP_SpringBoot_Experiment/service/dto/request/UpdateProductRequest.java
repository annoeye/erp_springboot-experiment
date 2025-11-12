package com.anno.ERP_SpringBoot_Experiment.service.dto.request;

import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateProductRequest {
    String id;
    String name;
    String description;
    String categorySku;
    ActiveStatus status;
}
