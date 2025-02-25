package com.windlogs.authentication.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;



@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "_user")
@EntityListeners(AuditingEntityListener.class) //when we have this annotation we must think to add @EnableJpaAuditing to the main class
public class User implements UserDetails, Principal {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "_user_seq")
    @SequenceGenerator(name = "_user_seq")
    private Long id;

    private String firstname;

    private String lastname;

    @Column(unique = true)
    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private String tenant;

    private String image; // Field for profile image

    private String pronouns;

    private String location;

    private String bio;

    private String company;

    private String phone;

    private String lien;

    @Column(name = "reset_password_token")
    private String resetPasswordToken;

    private boolean accountLocked;

    private boolean enabled;
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column(insertable = false)
    private LocalDateTime lastModifiedDate;
    /**
     * here when we use a table instead of an enum
     *
    @ManyToMany(fetch = EAGER)
    @JoinTable(
        name = "user_role",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private List<Role> roles = new ArrayList<>();
*/
    //if we choose to use an enum


    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
    }

    /**
     * in this case we use a list of roles but when we utilize an enum we should use a single role
     * we don't need to use a list
     *
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .toList();
    }
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        // Add authorities directly from the role
        role.getAuthorities().forEach(authority -> 
            authorities.add(new SimpleGrantedAuthority(authority.name()))
        );
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
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
        return !accountLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public String fullName() {
        return getFirstname() + " " + getLastname();
    }

    @Override
    public String getName() {
        return email;
    }

    public String getFullName() {
        return firstname + " " + lastname;
    }
}
