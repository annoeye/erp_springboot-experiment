package com.anno.ERP_SpringBoot_Experiment.model.embedded;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthCode {

    @Column(name = "code", nullable = false)
    String code;

    @Column(name = "purpose", nullable = false)
    @Enumerated(EnumType.STRING)
    Purpose purpose;

    @Column(name = "expiry_date", nullable = false)
    LocalDateTime expiryDate;

    public enum Purpose {

    }

    public boolean isExpired() {
        return expiryDate.isBefore(LocalDateTime.now());
    }
}