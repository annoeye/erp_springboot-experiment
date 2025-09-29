package com.anno.ERP_SpringBoot_Experiment.repository;

import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("SELECT u FROM User u WHERE u.authCode.code = :code")
    Optional<User> findByAuthCode(@Param("code") String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.name = :name")
    Optional<User> findByName(@Param("name") String name);

    @Query("SELECT u FROM User u WHERE u.name = :value OR u.email = :value")
    Optional<User> findByNameOrEmail(@Param("value") String value);

    @Query("SELECT u FROM User u WHERE u.name = :name OR u.email = :email")
    Optional<User> findByNameAndEmail(@Param("name") String name, @Param("email") String email);
}