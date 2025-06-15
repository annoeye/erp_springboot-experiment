package com.anno.ERP_SpringBoot_Experiment.repository;

import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserNameOrEmail(String userName, String email);
    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.userName = :userName")
    Optional<User> findByUsername(@Param("userName") String userName);

    @Query("SELECT u FROM User u WHERE u.email = :userEmail")
    Optional<User> findByByEmail(@Param("userEmail") String userEmail);

    @Query("SELECT u FROM User u WHERE u.userName = :userName")
    Optional<User> getUserByUsername(@Param("userName") String userName);

}