package com.windlogs.authentication.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "project_user", 
    indexes = {
        @Index(name = "idx_project_user", columnList = "project_id,user_id")
    }
)
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProjectUser implements Serializable {
    private static final long serialVersionUID = 1L;

    @EmbeddedId
    private ProjectUserId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("projectId")
    @JoinColumn(name = "project_id")
    @JsonIgnoreProperties({"projectUsers", "creator"})
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({"projectUsers", "password", "authorities", "accountNonExpired", "accountNonLocked", "credentialsNonExpired"})
    private User user;

    @Column(nullable = false)
    private boolean isCreator;

    @CreatedDate
    private LocalDateTime joinedAt;

    @PrePersist
    public void prePersist() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectUser that = (ProjectUser) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static ProjectUserBuilder builder() {
        return new ProjectUserBuilder();
    }

    public static class ProjectUserBuilder {
        private ProjectUserId id;
        private Project project;
        private User user;
        private boolean isCreator;
        private LocalDateTime joinedAt;

        ProjectUserBuilder() {
        }

        public ProjectUserBuilder id(ProjectUserId id) {
            this.id = id;
            return this;
        }

        public ProjectUserBuilder project(Project project) {
            this.project = project;
            return this;
        }

        public ProjectUserBuilder user(User user) {
            this.user = user;
            return this;
        }

        public ProjectUserBuilder isCreator(boolean isCreator) {
            this.isCreator = isCreator;
            return this;
        }

        public ProjectUserBuilder joinedAt(LocalDateTime joinedAt) {
            this.joinedAt = joinedAt;
            return this;
        }

        public ProjectUser build() {
            return new ProjectUser(id, project, user, isCreator, joinedAt);
        }
    }
}
