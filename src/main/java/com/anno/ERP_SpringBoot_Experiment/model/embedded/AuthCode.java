package com.anno.ERP_SpringBoot_Experiment.model.embedded;

import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
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
    ActiveStatus purpose;

    @Column(name = "expiry_date", nullable = false)
    LocalDateTime expiryDate;

    public AuthCode authCode(String code,  ActiveStatus purpose, LocalDateTime expiryDate) {
        this.code = code;
        this.purpose = purpose;
        this.expiryDate = expiryDate;
        return this;
    }

}