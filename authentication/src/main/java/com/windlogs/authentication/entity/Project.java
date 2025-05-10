package com.windlogs.authentication.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.util.StdConverter;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "projects")
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Project implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "project_seq")
    @SequenceGenerator(name = "project_seq", sequenceName = "project_seq", allocationSize = 1)
    private Long id;

    private String name;
    private String description;
    
    @JsonDeserialize(converter = TechnologiesConverter.class)
    private String technologies;
    
    private String repositoryLink;
    private String primaryTag;
    private float progressPercentage;
    private LocalDate deadlineDate;
    private Integer membersCount;
    private Boolean payed;
    private String tenant;
    private String logo;

    @Enumerated(EnumType.STRING)
    @Column(name = "project_type")
    private ProjectType projectType = ProjectType.MONOLITHIC;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_project_id")
    @JsonIgnoreProperties({"subProjects", "projectUsers", "creator"})
    private Project parentProject;

    @OneToMany(mappedBy = "parentProject", cascade = CascadeType.ALL, orphanRemoval = false)
    @JsonIgnoreProperties({"parentProject", "projectUsers", "creator"})
    private List<Project> subProjects = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "project_documentation_urls", joinColumns = @JoinColumn(name = "project_id"))
    @Column(name = "documentation_url")
    private List<String> documentationUrls;

    @ElementCollection
    @CollectionTable(name = "project_tags", joinColumns = @JoinColumn(name = "project_id"))
    @Column(name = "tag")
    private Set<String> tags = new HashSet<>();

    @ElementCollection(targetClass = Role.class)
    @CollectionTable(name = "project_allowed_roles", joinColumns = @JoinColumn(name = "project_id"))
    @Enumerated(EnumType.STRING)
    private Set<Role> allowedRoles = new HashSet<>();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "creator_id")
    @JsonIgnoreProperties({"projectUsers", "password", "authorities", "accountNonExpired", "accountNonLocked", "credentialsNonExpired"})
    private User creator;

    @OneToMany(
        mappedBy = "project",
        cascade = CascadeType.ALL,
        fetch = FetchType.EAGER,
        orphanRemoval = true
    )
    @JsonIgnoreProperties({"project", "user"})
    private Set<ProjectUser> projectUsers = new HashSet<>();

    @PrePersist
    @PreUpdate
    private void validateAndInitialize() {
        if (creator != null && !creator.getTenant().equals(tenant)) {
            throw new IllegalStateException("Project tenant must match creator's tenant");
        }

        // Validate project type and relationships
        if (projectType == ProjectType.MICROSERVICES_PACKAGE && !subProjects.isEmpty()) {
            subProjects.forEach(subProject -> {
                if (subProject.getProjectType() != ProjectType.MICROSERVICES) {
                    throw new IllegalStateException("Microservices package can only contain microservice projects");
                }
            });
        }

        if (projectType == ProjectType.MICROSERVICES && parentProject != null 
            && parentProject.getProjectType() != ProjectType.MICROSERVICES_PACKAGE) {
            throw new IllegalStateException("Microservice project must belong to a microservices package");
        }

        // Initialize collections if null
        if (tags == null) {
            tags = new HashSet<>();
        }
        if (allowedRoles == null) {
            allowedRoles = new HashSet<>();
        }
        if (documentationUrls == null) {
            documentationUrls = new ArrayList<>();
        }
        if (projectUsers == null) {
            projectUsers = new HashSet<>();
        }

        // Set default values if null
        if (progressPercentage == 0.0f) {
            progressPercentage = 0.0f;
        }
        if (membersCount == null) {
            membersCount = 0;
        }
        if (payed == null) {
            payed = false;
        }
    }

    public void addMember(User user, boolean isCreator) {
        // Initialize collection if null
        if (projectUsers == null) {
            projectUsers = new HashSet<>();
        }

        // Validate user
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        // Check if user is already a member
        boolean isAlreadyMember = projectUsers.stream()
                .anyMatch(pu -> pu.getUser().getId().equals(user.getId()));
        if (isAlreadyMember) {
            throw new IllegalArgumentException("User is already a member of this project");
        }

        // Create and add project user
        ProjectUser projectUser = ProjectUser.builder()
                .id(new ProjectUserId(this.id, user.getId()))
                .project(this)
                .user(user)
                .isCreator(isCreator)
                .build();

        // Update both sides of the relationship
        projectUsers.add(projectUser);
        user.getProjectUsers().add(projectUser);
        
        // Update members count
        this.membersCount = projectUsers.size();
    }

    public void removeMember(User user) {
        projectUsers.removeIf(projectUser -> projectUser.getUser().equals(user));
        user.getProjectUsers().removeIf(projectUser -> projectUser.getProject().equals(this));
    }

    public boolean isUserAllowedToCreate(User user) {
        return allowedRoles.contains(user.getRole()) || user.getRole() == Role.PARTNER;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Project project = (Project) o;
        return Objects.equals(id, project.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // Converter class to handle both string and array formats for technologies
    public static class TechnologiesConverter extends StdConverter<Object, String> {
        @Override
        public String convert(Object value) {
            if (value == null) {
                return null;
            }
            
            if (value instanceof String) {
                return (String) value;
            }
            
            if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> list = (List<String>) value;
                return list.stream().collect(Collectors.joining(", "));
            }
            
            return value.toString();
        }
    }
}
