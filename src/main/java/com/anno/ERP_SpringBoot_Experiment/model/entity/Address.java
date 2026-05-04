package com.anno.ERP_SpringBoot_Experiment.model.entity;

import com.anno.ERP_SpringBoot_Experiment.model.base.IdentityOnly;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.AuditInfo;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "addresses", indexes = {
        @Index(name = "idx_address_user ", columnList = "user_id"),
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Address extends IdentityOnly<Long> {

    @Column(name = "shipping_address", length = 500)
    String address;

    @Column(name = "shipping_phone", length = 20)
    String phoneNumber;

    @Column(name = "shipping_recipient_name", length = 200)
    String recipientName;

    @ManyToOne(targetEntity = User.class)
    @JoinColumn(name = "user_id")
    User user;

    @Embedded
    @Builder.Default
    AuditInfo auditInfo = new AuditInfo();
}
