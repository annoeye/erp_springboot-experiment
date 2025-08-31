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

    Optional<User> findByAuthCode_Code(String authCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.name = :userName")
    Optional<User> findByUsername(@Param("userName") String userName);

    @Query("SELECT u FROM User u WHERE u.name = :username AND u.email = :email")
    Optional<User> findByUsernameAndEmail(@Param("username") String username, @Param("email") String email);

    User getUserByUsername(String userName);
}