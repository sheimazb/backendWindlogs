package com.windlogs.authentication.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "_user")
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password", "authorities", "accountNonExpired", "accountNonLocked", "credentialsNonExpired"})
public class User implements UserDetails, Principal, Serializable {
    private static final long serialVersionUID = 1L;

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
    private String image;
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

    @OneToMany(
        mappedBy = "user",
        cascade = CascadeType.ALL,
        fetch = FetchType.EAGER,
        orphanRemoval = true
    )
    @JsonIgnoreProperties({"user", "project"})
    private Set<ProjectUser> projectUsers = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        if (projectUsers == null) {
            projectUsers = new HashSet<>();
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static UserBuilder builder() {
        return new UserBuilder();
    }

    public static class UserBuilder {
        private Long id;
        private String firstname;
        private String lastname;
        private String email;
        private String password;
        private Role role;
        private String tenant;
        private String image;
        private String pronouns;
        private String location;
        private String bio;
        private String company;
        private String phone;
        private String lien;
        private String resetPasswordToken;
        private boolean accountLocked;
        private boolean enabled;
        private LocalDateTime createdDate;
        private LocalDateTime lastModifiedDate;
        private Set<ProjectUser> projectUsers = new HashSet<>();

        UserBuilder() {
        }

        public UserBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public UserBuilder firstname(String firstname) {
            this.firstname = firstname;
            return this;
        }

        public UserBuilder lastname(String lastname) {
            this.lastname = lastname;
            return this;
        }

        public UserBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserBuilder password(String password) {
            this.password = password;
            return this;
        }

        public UserBuilder role(Role role) {
            this.role = role;
            return this;
        }

        public UserBuilder tenant(String tenant) {
            this.tenant = tenant;
            return this;
        }

        public UserBuilder image(String image) {
            this.image = image;
            return this;
        }

        public UserBuilder pronouns(String pronouns) {
            this.pronouns = pronouns;
            return this;
        }

        public UserBuilder location(String location) {
            this.location = location;
            return this;
        }

        public UserBuilder bio(String bio) {
            this.bio = bio;
            return this;
        }

        public UserBuilder company(String company) {
            this.company = company;
            return this;
        }

        public UserBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public UserBuilder lien(String lien) {
            this.lien = lien;
            return this;
        }

        public UserBuilder resetPasswordToken(String resetPasswordToken) {
            this.resetPasswordToken = resetPasswordToken;
            return this;
        }

        public UserBuilder accountLocked(boolean accountLocked) {
            this.accountLocked = accountLocked;
            return this;
        }

        public UserBuilder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public UserBuilder createdDate(LocalDateTime createdDate) {
            this.createdDate = createdDate;
            return this;
        }

        public UserBuilder lastModifiedDate(LocalDateTime lastModifiedDate) {
            this.lastModifiedDate = lastModifiedDate;
            return this;
        }

        public UserBuilder projectUsers(Set<ProjectUser> projectUsers) {
            this.projectUsers = projectUsers;
            return this;
        }

        public User build() {
            return new User(id, firstname, lastname, email, password, role, tenant,
                    image, pronouns, location, bio, company, phone, lien,
                    resetPasswordToken, accountLocked, enabled, createdDate,
                    lastModifiedDate, projectUsers);
        }
    }
}
