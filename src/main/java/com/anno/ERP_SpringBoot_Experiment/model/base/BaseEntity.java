package com.anno.ERP_SpringBoot_Experiment.model.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import lombok.Builder;

import java.io.Serializable;
import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
@ToString
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class BaseEntity<T extends Serializable> extends IdentityOnly<T> {

  @Column(name = "created_by")
  String createdBy;

  @Column(name = "updated_by")
  String updatedBy;

  @Column(name = "created_at")
  LocalDateTime createdAt;

  @Column(name = "updated_at")
  LocalDateTime updatedAt;

  @Builder.Default
  @Column(name = "is_deleted")
  Boolean isDeleted = false;
}
