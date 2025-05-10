package com.windlogs.authentication.service;

import com.windlogs.authentication.entity.*;
import com.windlogs.authentication.repository.ProjectRepository;
import com.windlogs.authentication.repository.UserRepository;
import com.windlogs.authentication.repository.ProjectUserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.ArrayList;

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


    public Optional<User> getUserById(Long id) {
        logger.info("Fetching user with ID: {}", id);
        return userRepository.findById(id);
    }
    public Project createProject(Project project, MultipartFile logo) {
        logger.info("Creating new project: {} of type: {}", project.getName(), project.getProjectType());

        // Check if primary tag already exists
        if (projectRepository.existsByPrimaryTag(project.getPrimaryTag())) {
            logger.warn("Primary tag '{}' already exists", project.getPrimaryTag());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Primary tag already exists. Please choose a different one.");
        }

        // If it's a microservice, load and set the parent project
        if (project.getProjectType() == ProjectType.MICROSERVICES) {
            Long parentId = project.getParentProject() != null ? project.getParentProject().getId() : null;
            if (parentId != null) {
                Project parentProject = projectRepository.findById(parentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Parent project not found with id: " + parentId));
                project.setParentProject(parentProject);
                
                // Important: clear the subProjects collection to avoid the orphanRemoval issue
                // We're not managing the parent's subProjects here; we're just setting the child's reference
                project.setSubProjects(new ArrayList<>());
            }
        }

        // Validate project type and relationships
        validateProjectTypeAndRelationships(project);

        // Process logo if provided
        if (logo != null && !logo.isEmpty()) {
            try {
                String logoUrl = saveLogoFile(logo);
                project.setLogo(logoUrl);
                logger.info("Logo saved successfully at: {}", logoUrl);
            } catch (IOException e) {
                logger.error("Failed to save project logo file: {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Failed to save logo file: " + e.getMessage());
            }
        } else {
            logger.info("No logo provided for project");
        }
        
        // Save the project
        Project savedProject = projectRepository.save(project);
        
        // If this is a microservice, update the parent's subProjects collection separately
        if (project.getProjectType() == ProjectType.MICROSERVICES && project.getParentProject() != null) {
            try {
                logger.info("Updating parent package's subProjects collection");
                Project parentProject = projectRepository.findById(project.getParentProject().getId()).orElse(null);
                if (parentProject != null) {
                    // No need to save, just returning the project
                    logger.info("Successfully linked microservice to parent package");
                }
            } catch (Exception e) {
                logger.warn("Non-critical error updating parent's subProjects: {}", e.getMessage());
                // Don't throw here as the microservice itself was already created
            }
        }
        
        return savedProject;
    }

    private void validateProjectTypeAndRelationships(Project project) {
        logger.info("Validating project type {} and relationships", project.getProjectType());
        
        switch (project.getProjectType()) {
            case MICROSERVICES:
                if (project.getParentProject() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Microservice project must belong to a microservices package");
                }
                Project parent = projectRepository.findById(project.getParentProject().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Parent project not found"));
                if (parent.getProjectType() != ProjectType.MICROSERVICES_PACKAGE) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Parent project must be a microservices package");
                }
                break;

            case MICROSERVICES_PACKAGE:
                if (project.getParentProject() != null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Microservices package cannot have a parent project");
                }
                break;

            case MONOLITHIC:
                if (project.getParentProject() != null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Monolithic project cannot have a parent project");
                }
                break;
        }
    }

    public Optional<Project> getProjectById(Long id) {
        logger.info("Fetching project with ID: {}", id);
        Optional<Project> projectOpt = projectRepository.findById(id);
        
        projectOpt.ifPresent(project -> {
            // For microservices package, eagerly fetch sub-projects
            if (project.getProjectType() == ProjectType.MICROSERVICES_PACKAGE) {
                logger.info("Fetching sub-projects for microservices package: {}", project.getName());
                List<Project> subProjects = projectRepository.findByParentProjectId(id);
                project.setSubProjects(subProjects);
            }
            
            // For microservice, ensure parent project is loaded
            if (project.getProjectType() == ProjectType.MICROSERVICES && project.getParentProject() != null) {
                logger.info("Fetching parent project for microservice: {}", project.getName());
                projectRepository.findById(project.getParentProject().getId())
                    .ifPresent(parent -> project.setParentProject(parent));
            }
        });
        
        return projectOpt;
    }

    public List<Project> getAllProjectsByTenant(String tenant) {
        logger.info("Fetching all projects for tenant: {}", tenant);
        List<Project> projects = projectRepository.findByTenant(tenant);
        
        // Process each project to load its relationships
        projects.forEach(project -> {
            if (project.getProjectType() == ProjectType.MICROSERVICES_PACKAGE) {
                List<Project> subProjects = projectRepository.findByParentProjectId(project.getId());
                project.setSubProjects(subProjects);
            }
            if (project.getProjectType() == ProjectType.MICROSERVICES && project.getParentProject() != null) {
                projectRepository.findById(project.getParentProject().getId())
                    .ifPresent(parent -> project.setParentProject(parent));
            }
        });
        
        return projects;
    }

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
            Optional.of(updatedProject.getProgressPercentage()).ifPresent(existingProject::setProgressPercentage);
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

            // Process logo if provided
            if (logo != null && !logo.isEmpty()) {
                try {
                    String uploadDir = "public/images/";
                    
                    // Delete old logo if it exists
                    if (existingProject.getLogo() != null && !existingProject.getLogo().isEmpty()) {
                        // If the stored logo is not a URL, try to delete it locally
                        if (!existingProject.getLogo().startsWith("http://") && !existingProject.getLogo().startsWith("https://")) {
                            Path oldLogoPath = Paths.get(uploadDir, existingProject.getLogo());
                            try {
                                Files.delete(oldLogoPath);
                                logger.info("Deleted old logo file: {}", oldLogoPath);
                            } catch (Exception e) {
                                logger.error("Failed to delete old logo: {}", e.getMessage());
                            }
                        } else {
                            logger.info("Existing logo is a URL; skipping file deletion.");
                        }
                    }
                    
                    // Save new logo
                    String logoUrl = saveLogoFile(logo);
                    existingProject.setLogo(logoUrl);
                    logger.info("Updated project logo to: {}", logoUrl);
                } catch (IOException e) {
                    logger.error("Failed to save logo file: {}", e.getMessage());
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                        "Failed to save logo file: " + e.getMessage());
                }
            }

            return projectRepository.save(existingProject);
        });
    }

    public void deleteProject(Long id) {
        logger.info("Deleting project with ID: {}", id);
        projectRepository.deleteById(id);
    }

    public List<Project> findProjectsByTagAndTenant(String tag, String tenant) {
        logger.info("Searching projects with tag: {} for tenant: {}", tag, tenant);
        return projectRepository.findByTagsContainingAndTenant(tag, tenant);
    }

    public List<Project> findProjectsByPrimaryTag(String primaryTag) {
        logger.info("Searching projects with primary tag: {}", primaryTag);
        return projectRepository.findByPrimaryTag(primaryTag);
    }

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

    public Project removeUserFromProject(Long projectId, Long userId) {
        logger.info("Removing user {} from project {}", userId, projectId);
        
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
                
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        project.removeMember(user);
        return projectRepository.save(project);
    }

    public Set<User> getProjectUsers(Long projectId) {
        logger.info("Getting users for project {}", projectId);
        
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        return project.getProjectUsers().stream()
                .map(ProjectUser::getUser)
                .collect(Collectors.toSet());
    }

    public Set<Project> getProjectsUser(Long userId){
        logger.info("Getting projects for users {}",userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));
        
        return user.getProjectUsers().stream()
                .map(ProjectUser::getProject)
                .collect(Collectors.toSet());
    }

    private String saveLogoFile(MultipartFile logoFile) throws IOException {
        String uploadDir = "public/images/";
        Date createdDate = new Date();
        String storageFileName = createdDate.getTime() + "_" + logoFile.getOriginalFilename();
        
        try (InputStream inputStream = logoFile.getInputStream()) {
            // Create directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath);
            
            // Use Paths.get(uploadDir, storageFileName) for better path handling
            Path destination = Paths.get(uploadDir, storageFileName);
            logger.info("Saving logo to: {}", destination.toAbsolutePath());
            
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            return "http://localhost:8222/images" + '/' + storageFileName;
        }
    }

    @Transactional
    public Project addMicroserviceToPackage(Long packageId, Project microservice) {
        logger.info("Adding microservice {} to package {}", microservice.getName(), packageId);

        Project packageProject = projectRepository.findById(packageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                    "Microservices package not found"));

        if (packageProject.getProjectType() != ProjectType.MICROSERVICES_PACKAGE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Target project is not a microservices package");
        }

        microservice.setProjectType(ProjectType.MICROSERVICES);
        microservice.setParentProject(packageProject);
        packageProject.getSubProjects().add(microservice);

        return projectRepository.save(packageProject);
    }

    public List<Project> getMicroservicesForPackage(Long packageId) {
        logger.info("Fetching microservices for package ID: {}", packageId);
        Project packageProject = projectRepository.findById(packageId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "Microservices package not found"));

        if (packageProject.getProjectType() != ProjectType.MICROSERVICES_PACKAGE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Specified project is not a microservices package");
        }

        return projectRepository.findByParentProjectId(packageId);
    }
}
