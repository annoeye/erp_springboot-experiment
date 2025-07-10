package com.anno.ERP_SpringBoot_Experiment.repository;

import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import com.anno.ERP_SpringBoot_Experiment.model.entity.ViolationHandling;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ViolationHandlingRepository extends JpaRepository<ViolationHandling, Long> {
    ViolationHandling findViolationHandlingByTargetUser_Id(Long id);
}
