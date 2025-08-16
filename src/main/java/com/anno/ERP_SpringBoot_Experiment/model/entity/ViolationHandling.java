package com.anno.ERP_SpringBoot_Experiment.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
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
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id", referencedColumnName = "id")
    User targetUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handled_by", referencedColumnName = "id")
    private User handledBy;

    String reason;

    @Enumerated(EnumType.STRING)
    Log.ActionType action;
    LocalDateTime startAt;
    LocalDateTime endAt;

    LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}

