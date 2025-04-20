package com.windlogs.authentication.service;

import com.windlogs.authentication.entity.*;
import com.windlogs.authentication.repository.ProjectRepository;
import com.windlogs.authentication.repository.UserRepository;
import com.windlogs.authentication.repository.ProjectUserRepository;
import com.windlogs.authentication.dto.ProjectMultipartRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service class responsible for managing projects and their associated users.
 * Provides methods for creating, updating, deleting, and querying projects, as well as managing project membership.
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectUserRepository projectUserRepository;
    
    @Value("${file.upload-dir:uploads/}")
    private String uploadDir;

    /**
     * Retrieves a user by their ID.
     *
     * @param id the ID of the user to retrieve
     * @return an {@link Optional} containing the {@link User} if found, or empty if not
     */
    public Optional<User> getUserById(Long id) {
        logger.info("Fetching user with ID: {}", id);
        return userRepository.findById(id);
    }

    /**
     * Creates a new project and saves it to the database.
     *
     * @param project the {@link Project} object to create
     * @param logo
     * @return the saved {@link Project} entity
     */
    public Project createProject(Project project, MultipartFile logo) {
        logger.info("Creating new project: {}", project.getName());
        return projectRepository.save(project);
    }

    /**
     * Retrieves a project by its ID.
     *
     * @param id the ID of the project to retrieve
     * @return an {@link Optional} containing the {@link Project} if found, or empty if not
     */
    public Optional<Project> getProjectById(Long id) {
        logger.info("Fetching project with ID: {}", id);
        return projectRepository.findById(id);
    }

    /**
     * Retrieves all projects associated with a specific tenant.
     *
     * @param tenant the tenant identifier to filter projects by
     * @return a {@link List} of {@link Project} objects for the specified tenant
     */
    public List<Project> getAllProjectsByTenant(String tenant) {
        logger.info("Fetching all projects for tenant: {}", tenant);
        return projectRepository.findByTenant(tenant);
    }

    /**
     * Updates an existing project with the provided details.
     *
     * @param id             the ID of the project to update
     * @param updatedProject the {@link Project} object containing updated fields
     * @param logo
     * @return an {@link Optional} containing the updated {@link Project} if found, or empty if not
     */
    public Optional<Project> updateProject(Long id, Project updatedProject, MultipartFile logo) {
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
            if (updatedProject.getPrimaryTag() != null) {
                existingProject.setPrimaryTag(updatedProject.getPrimaryTag());
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


    /**
     * Deletes a project by its ID.
     *
     * @param id the ID of the project to delete
     */
    public void deleteProject(Long id) {
        logger.info("Deleting project with ID: {}", id);
        projectRepository.deleteById(id);
    }

    /**
     * Searches for projects by a specific tag and tenant.
     *
     * @param tag the tag to search for within project tags
     * @param tenant the tenant identifier to filter projects by
     * @return a {@link List} of {@link Project} objects matching the tag and tenant
     */
    public List<Project> findProjectsByTagAndTenant(String tag, String tenant) {
        logger.info("Searching projects with tag: {} for tenant: {}", tag, tenant);
        return projectRepository.findByTagsContainingAndTenant(tag, tenant);
    }

    /**
     * Retrieves projects by their primary tag.
     *
     * @param primaryTag the primary tag to filter projects by
     * @return a {@link List} of {@link Project} objects with the specified primary tag
     */
    public List<Project> findProjectsByPrimaryTag(String primaryTag) {
        logger.info("Searching projects with primary tag: {}", primaryTag);
        return projectRepository.findByPrimaryTag(primaryTag);
    }

    /**
     * Adds a user to a project and updates the project's member count.
     *
     * @param projectId the ID of the project to add the user to
     * @param userId the ID of the user to add
     * @return the updated {@link Project} entity
     * @throws IllegalArgumentException if the project or user is not found, or if the user is already a member
     * @throws IllegalStateException if there is an error adding the user to the project
     */
    @Transactional
    public Project addUserToProject(Long projectId, Long userId) {
        logger.info("Adding user {} to project {}", userId, projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        try {
            if (projectUserRepository.existsById(new ProjectUserId(projectId, userId))) {
                throw new IllegalArgumentException("User is already a member of this project");
            }

            ProjectUser projectUser = ProjectUser.builder()
                    .id(new ProjectUserId(projectId, userId))
                    .project(project)
                    .user(user)
                    .isCreator(false)
                    .build();

            projectUserRepository.save(projectUser);

            project.setMembersCount(project.getProjectUsers().size() + 1);

            Project savedProject = projectRepository.save(project);
            logger.info("Successfully added user {} to project {}", userId, projectId);
            return savedProject;

        } catch (Exception e) {
            logger.error("Failed to add user {} to project {}: {}", userId, projectId, e.getMessage());
            throw new IllegalStateException("Failed to add user to project: " + e.getMessage());
        }
    }

    /**
     * Removes a user from a project and updates the project's member count.
     *
     * @param projectId the ID of the project to remove the user from
     * @param userId the ID of the user to remove
     * @return the updated {@link Project} entity
     * @throws IllegalArgumentException if the project or user is not found
     */
    public Project removeUserFromProject(Long projectId, Long userId) {
        logger.info("Removing user {} from project {}", userId, projectId);
        
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
                
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        project.removeMember(user);
        return projectRepository.save(project);
    }

    /**
     * Retrieves all users associated with a project.
     *
     * @param projectId the ID of the project to query
     * @return a {@link Set} of {@link User} objects associated with the project
     * @throws IllegalArgumentException if the project is not found
     */
    public Set<User> getProjectUsers(Long projectId) {
        logger.info("Getting users for project {}", projectId);
        
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        return project.getProjectUsers().stream()
                .map(ProjectUser::getUser)
                .collect(Collectors.toSet());
    }

    /**
     * Retrieves all projects associated with a user.
     *
     * @param userId the ID of the user to query
     * @return a {@link Set} of {@link Project} objects associated with the user
     * @throws IllegalArgumentException if the user is not found
     */
    public Set<Project> getProjectsUser(Long userId){
        logger.info("Getting projects for users {}",userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));
        
        return user.getProjectUsers().stream()
                .map(ProjectUser::getProject)
                .collect(Collectors.toSet());
    }

    /**
     * Helper method to save a logo file to disk and return its URL
     *
     * @param logoFile the MultipartFile to save
     * @return the URL where the file can be accessed
     * @throws IOException if the file cannot be saved
     */
    private String saveLogoFile(MultipartFile logoFile) throws IOException {
        Date createdDate = new Date();
        String storageFileName = createdDate.getTime() + "_" + logoFile.getOriginalFilename();
        
        try (InputStream inputStream = logoFile.getInputStream()) {
            // Create directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath);
            
            // Use Paths.get(uploadDir, storageFileName) for better path handling
            Path destination = Paths.get(uploadDir, storageFileName);
            logger.info("Saving logo to: {}", destination.toAbsolutePath().toString());
            
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            return "http://localhost:8222/images" + '/' + storageFileName;
        }
    }
}
