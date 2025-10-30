package com.anno.ERP_SpringBoot_Experiment.repository;

import com.anno.ERP_SpringBoot_Experiment.model.entity.Attributes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AttributesRepository extends JpaRepository<Attributes, UUID> {
}
