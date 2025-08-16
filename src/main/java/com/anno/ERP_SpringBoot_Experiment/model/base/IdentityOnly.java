package com.anno.ERP_SpringBoot_Experiment.model.base;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@MappedSuperclass
@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public abstract class IdentityOnly {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    String id = UUID.randomUUID().toString();

    String name;
}
