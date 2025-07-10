package com.anno.ERP_SpringBoot_Experiment.model.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "violation_handling")
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class ViolationHandling {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @OneToOne
    @JoinColumn(name = "target_user_id")
    User targetUser;

    @ManyToOne
    @JoinColumn(name = "handled_by")
    User handledBy;

    String reason;

    @Enumerated(EnumType.STRING)
    UserActionLog.ActionType action;
    LocalDateTime startAt;
    LocalDateTime endAt;

    LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}

