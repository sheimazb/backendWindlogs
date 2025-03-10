package com.windlogs.authentication.service;

import com.windlogs.authentication.entity.*;
import com.windlogs.authentication.repository.ProjectRepository;
import com.windlogs.authentication.repository.UserRepository;
import com.windlogs.authentication.repository.ProjectUserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private static final Logger logger = LoggerFactory.getLogger(ProjectService.class);
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectUserRepository projectUserRepository;

    // Get a user by ID
    public Optional<User> getUserById(Long id) {
        logger.info("Fetching user with ID: {}", id);
        return userRepository.findById(id);
    }

    // Create a new project
    public Project createProject(Project project) {
        logger.info("Creating new project: {}", project.getName());
        return projectRepository.save(project);
    }

    // Get a project by ID
    public Optional<Project> getProjectById(Long id) {
        logger.info("Fetching project with ID: {}", id);
        return projectRepository.findById(id);
    }

    // Get all projects by tenant
    public List<Project> getAllProjectsByTenant(String tenant) {
        logger.info("Fetching all projects for tenant: {}", tenant);
        return projectRepository.findByTenant(tenant);
    }

    // Get all projects
    public List<Project> getAllProjects() {
        logger.info("Fetching all projects");
        return projectRepository.findAll();
    }

    // Update a project
    public Optional<Project> updateProject(Long id, Project updatedProject) {
        logger.info("Updating project with ID: {}", id);

        return projectRepository.findById(id).map(existingProject -> {
            if (updatedProject.getName() != null) {
                existingProject.setName(updatedProject.getName());
            }
            if (updatedProject.getDescription() != null) {
                existingProject.setDescription(updatedProject.getDescription());
            }
            if (updatedProject.getTechnologies() != null) {
                existingProject.setTechnologies(updatedProject.getTechnologies());
            }
            if (updatedProject.getRepositoryLink() != null) {
                existingProject.setRepositoryLink(updatedProject.getRepositoryLink());
            }
            Optional.ofNullable(updatedProject.getProgressPercentage()).ifPresent(existingProject::setProgressPercentage);
            if (updatedProject.getDeadlineDate() != null) {
                existingProject.setDeadlineDate(updatedProject.getDeadlineDate());
            }
            if (updatedProject.getMembersCount() != null) {
                existingProject.setMembersCount(updatedProject.getMembersCount());
            }
            if (updatedProject.getPayed() != null) {
                existingProject.setPayed(updatedProject.getPayed());
            }
            if (updatedProject.getDocumentationUrls() != null) {
                existingProject.setDocumentationUrls(updatedProject.getDocumentationUrls());
            }
            if (updatedProject.getTags() != null) {
                existingProject.setTags(updatedProject.getTags());
            }
            if (updatedProject.getAllowedRoles() != null) {
                existingProject.setAllowedRoles(updatedProject.getAllowedRoles());
            }

            return projectRepository.save(existingProject);
        });
    }


    // Delete a project by ID
    public void deleteProject(Long id) {
        logger.info("Deleting project with ID: {}", id);
        projectRepository.deleteById(id);
    }

    // Search projects by tag and tenant
    public List<Project> findProjectsByTagAndTenant(String tag, String tenant) {
        logger.info("Searching projects with tag: {} for tenant: {}", tag, tenant);
        return projectRepository.findByTagsContainingAndTenant(tag, tenant);
    }

    // Add a user to a project
    @Transactional
    public Project addUserToProject(Long projectId, Long userId) {
        logger.info("Adding user {} to project {}", userId, projectId);
        
        // Get project and user
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
                
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        try {
            // Check if user is already a member using ProjectUserRepository
            if (projectUserRepository.existsById(new ProjectUserId(projectId, userId))) {
                throw new IllegalArgumentException("User is already a member of this project");
            }

            // Create new ProjectUser instance
            ProjectUser projectUser = ProjectUser.builder()
                    .id(new ProjectUserId(projectId, userId))
                    .project(project)
                    .user(user)
                    .isCreator(false)
                    .build();

            // Save the ProjectUser entity first
            projectUserRepository.save(projectUser);
            
            // Update members count
            project.setMembersCount(project.getProjectUsers().size() + 1);
            
            // Save and return updated project
            Project savedProject = projectRepository.save(project);
            logger.info("Successfully added user {} to project {}", userId, projectId);
            return savedProject;
            
        } catch (Exception e) {
            logger.error("Failed to add user {} to project {}: {}", userId, projectId, e.getMessage());
            throw new IllegalStateException("Failed to add user to project: " + e.getMessage());
        }
    }

    // Remove a user from a project
    public Project removeUserFromProject(Long projectId, Long userId) {
        logger.info("Removing user {} from project {}", userId, projectId);
        
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
                
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        project.removeMember(user);
        return projectRepository.save(project);
    }

    // Get all users in a project
    public Set<User> getProjectUsers(Long projectId) {
        logger.info("Getting users for project {}", projectId);
        
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        return project.getProjectUsers().stream()
                .map(ProjectUser::getUser)
                .collect(Collectors.toSet());
    }
}
