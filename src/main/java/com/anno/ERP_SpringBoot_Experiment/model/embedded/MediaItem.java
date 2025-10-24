package com.anno.ERP_SpringBoot_Experiment.model.embedded;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MediaItem {
    @Size(max = 5)
    String key;

    String url;
}
