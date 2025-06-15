package com.anno.ERP_SpringBoot_Experiment.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "Users")
@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "user_name")
    String userName;

    @NotNull
    @Column(nullable = false)
    String password;

    @Column(name = "phone_number")
    @Pattern(regexp = "^\\d{10}$", message = "Số điện thoại chỉ được 10 số!")
    String phoneNumber;

    @Email(message = "Email phải hợp lệ!")
    @NotNull
    @Column(nullable = false)
    String email;

    @Column(name = "full_name")
    @Pattern(regexp = "^[A-Za-z\\s]+$", message = "Tên chỉ có thể là chữ cái!")
    String fullName;

    @Column(name = "date_of_birth")
    Date dateOfBirth;

    @Enumerated(EnumType.STRING)
    Gender gender;

    @Column(name = "avatar_url")
    String AvatarUrl;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    LocalDateTime createdAt;

    @Column(name = "update_at")
    @Temporal(TemporalType.TIMESTAMP)
    LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    Active active;

    @Column(name = "email_verification_token")
    String emailVerificationToken;

    @Size(max = 6)
    @Column(name = "code_reset_password")
    String codeResetPassword;

    @Column(name = "token_expiry_date")
    LocalDateTime tokenExpiryDate;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(  name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "userInfo", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RefreshToken> refreshTokens;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (roles.isEmpty()) return Collections.emptyList();
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
    }

    @Override
    public String getUsername() {
       if(email.isEmpty()) return userName;
       return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active != Active.LOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active == Active.ACTIVE;
    }
        public enum Gender {
        MALE, FEMALE, OTHER
    }

    public enum Active {
        LOCKED, ACTIVE, INACTIVE
    }

    public enum ERole {
        ROLE_USER,
        ROLE_ADMIN,
        ROLE_SUPER_ADMIN
    }
}