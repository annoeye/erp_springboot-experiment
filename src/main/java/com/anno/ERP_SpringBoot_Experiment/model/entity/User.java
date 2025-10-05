package com.anno.ERP_SpringBoot_Experiment.model.entity;

import com.anno.ERP_SpringBoot_Experiment.model.base.Auditable;
import com.anno.ERP_SpringBoot_Experiment.model.base.IdentityOnly;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.AuditInfo;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.AuthCode;
import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import com.anno.ERP_SpringBoot_Experiment.model.enums.Gender;
import com.anno.ERP_SpringBoot_Experiment.model.enums.RoleType;
import com.anno.ERP_SpringBoot_Experiment.model.listener.AuditEntityListener;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "Users")
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@EntityListeners(AuditEntityListener.class)
@Builder
public class User extends IdentityOnly implements UserDetails, Auditable {

    @Column(name = "full_name")
    @Pattern(regexp = "^[\\p{L}\\s]+$", message = "Tên chỉ được chứa chữ cái và khoảng trắng!")
    String fullName;

    @NotNull(message = "Mật khẩu không được để trống")
    @Column(nullable = false)
    String password;

    @Column(name = "phone_number")
    @Pattern(regexp = "^\\d{10}$", message = "Số điện thoại chỉ được 10 số!")
    String phoneNumber;

    @NotNull(message = "Email không được để trống")
    @Email(message = "Email phải hợp lệ!")
    @Column(nullable = false)
    String email;

    @Column(name = "date_of_birth")
    Date dateOfBirth;

    @Column(name = "avatar_url")
    String avatarUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "roles")
    Set<RoleType> roles = new HashSet<>();

    /* ============================ 🧩 Embedded Fields ============================ */

    @Embedded
    AuditInfo auditInfo = new AuditInfo();

    @Embedded
    AuthCode authCode = new AuthCode();

    /* ============================ 🗂️ Enum ============================ */

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    ActiveStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    Gender gender;

    /* ============================ 🔐 UserDetails ============================ */

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (roles == null || roles.isEmpty()) return Collections.emptyList();
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .collect(Collectors.toList());
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != ActiveStatus.LOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == ActiveStatus.ACTIVE;
    }
}