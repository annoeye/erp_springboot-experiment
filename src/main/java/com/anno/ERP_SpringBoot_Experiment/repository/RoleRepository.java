package com.anno.ERP_SpringBoot_Experiment.repository;

import com.anno.ERP_SpringBoot_Experiment.model.entity.Role;
import com.anno.ERP_SpringBoot_Experiment.model.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(String name);

    @Query("SELECT r FROM Role r WHERE r.name IN :roles")
    List<Role> findByRoleIn(@Param("roles") List<RoleType> roles);

    String name(String name);
}