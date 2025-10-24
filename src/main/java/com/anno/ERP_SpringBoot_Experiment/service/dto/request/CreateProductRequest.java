package com.anno.ERP_SpringBoot_Experiment.service.dto.request;

import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateProductRequest {
    String name;
    String description;
    String categorySku;
    ActiveStatus status;
    List<MultipartFile> images;
}
