package com.windlogs.authentication.controller;

import java.util.*;
import java.util.stream.Collectors;

import com.windlogs.authentication.entity.ProjectType;
import com.windlogs.authentication.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.windlogs.authentication.entity.Project;
import com.windlogs.authentication.entity.User;
import com.windlogs.authentication.entity.Role;
import com.windlogs.authentication.service.ProjectService;
import com.windlogs.authentication.dto.ProjectRequestDTO;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {
    private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);
    private final ProjectService projectService;
    private final ProjectRepository projectRepository;

    @GetMapping("/check-primary-tag")
    public ResponseEntity<Boolean> checkPrimaryTag(@RequestParam String tag) {
        boolean exists = projectRepository.existsByPrimaryTagIgnoreCase(tag);
        return ResponseEntity.ok(exists);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CREATE_PROJECT')")
    public ResponseEntity<Project> createProject(
            @ModelAttribute ProjectRequestDTO projectDTO,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();

            if (user.getRole() != Role.PARTNER) {
                logger.warn("Unauthorized attempt to create project by non-partner user: {}", user.getEmail());
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Only partners can create projects"
                );
            }

            // Log detailed project request information
            logger.info("Project creation request: name={}, type={}, parentId={}",
                projectDTO.getName(), projectDTO.getProjectType(), projectDTO.getParentProjectId());

            // Convert DTO to Project entity
            Project project = projectDTO.toProject();
            
            // Set additional project values
            String partnerTenant = user.getTenant();
            project.setTenant(partnerTenant);
            project.setCreator(user);
            project.setProgressPercentage(project.getProgressPercentage() > 0 ? project.getProgressPercentage() : 0);
            project.setMembersCount(1); // Starting with creator
            if (project.getPayed() == null) {
                project.setPayed(false);
            }

            // If creating a microservice, verify parent package exists and has correct type
            if (project.getProjectType() == ProjectType.MICROSERVICES && projectDTO.getParentProjectId() != null) {
                logger.info("Verifying parent package (ID: {}) for microservice", projectDTO.getParentProjectId());
                Project packageProject = projectService.getProjectById(projectDTO.getParentProjectId())
                    .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Parent package not found with id: " + projectDTO.getParentProjectId()
                    ));
                
                if (packageProject.getProjectType() != ProjectType.MICROSERVICES_PACKAGE) {
                    throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Parent project must be a microservices package"
                    );
                }
                
                // Ensure parent project is properly set
                project.setParentProject(packageProject);
            }

            logger.info("Creating new project of type {} for partner: {} in tenant: {}", 
                project.getProjectType(), user.getEmail(), partnerTenant);
            Project createdProject = projectService.createProject(project, projectDTO.getLogo());

            // Add the creator as a project member
            createdProject.addMember(user, true);

            return ResponseEntity.ok(projectService.getProjectById(createdProject.getId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "Failed to retrieve created project"
                    )));

        } catch (ResponseStatusException e) {
            logger.error("Failed to create project: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Failed to create project: {}", e.getMessage(), e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to create project: " + e.getMessage()
            );
        }
    }

    /**
     * New dedicated endpoint for creating microservices within a package
     */
    @PostMapping(value = "/packages/{packageId}/microservices", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CREATE_PROJECT')")
    public ResponseEntity<Project> createMicroservice(
            @PathVariable Long packageId,
            @ModelAttribute ProjectRequestDTO microserviceDTO,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            logger.info("Creating microservice in package {} by user {}", packageId, user.getEmail());

            // Get package and verify it exists
            Project packageProject = projectService.getProjectById(packageId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Package not found with id: " + packageId
                    ));

            // Verify package is of correct type
            if (packageProject.getProjectType() != ProjectType.MICROSERVICES_PACKAGE) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Target project is not a microservices package"
                );
            }

            // Verify tenant access
            if (!packageProject.getTenant().equals(user.getTenant())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You don't have access to this package"
                );
            }

            // Check if user is authorized (package creator or PARTNER)
            boolean isCreator = packageProject.getCreator().getId().equals(user.getId());
            boolean isPartner = user.getRole() == Role.PARTNER;
            boolean isMember = packageProject.getProjectUsers().stream()
                    .anyMatch(pu -> pu.getUser().getId().equals(user.getId()));

            logger.info("Authorization check - Is Creator: {}, Is Partner: {}, Is Member: {}",
                    isCreator, isPartner, isMember);

            if (!isCreator && !isPartner && !isMember) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Only package owner, members, or partners can create microservices"
                );
            }

            // Create microservice - use the main project creation endpoint by adding the necessary fields
            microserviceDTO.setProjectType(ProjectType.MICROSERVICES);
            microserviceDTO.setParentProjectId(packageId);
            
            // Now use the regular createProject method by calling directly
            Project microservice = microserviceDTO.toProject();
            
            // Set parent project reference
            Project parentRef = new Project();
            parentRef.setId(packageId);
            microservice.setParentProject(parentRef);
            
            // Keep tenant consistent with package
            microservice.setTenant(packageProject.getTenant());
            
            // Set creator
            microservice.setCreator(user);
            
            // Initialize empty subProjects list to avoid orphanRemoval issues
            microservice.setSubProjects(new ArrayList<>());
            
            // Set sensible defaults
            if (microservice.getProgressPercentage() <= 0) {
                microservice.setProgressPercentage(0);
            }
            microservice.setMembersCount(1); // Starting with creator
            if (microservice.getPayed() == null) {
                microservice.setPayed(packageProject.getPayed());
            }

            // Log before creation for debugging
            logger.info("Creating microservice: name={}, type={}, parentId={}, tenant={}",
                    microservice.getName(),
                    microservice.getProjectType(),
                    packageProject.getId(),
                    microservice.getTenant());

            // Create the microservice
            Project createdMicroservice = projectService.createProject(microservice, microserviceDTO.getLogo());

            // Add the creator as a member
            createdMicroservice.addMember(user, true);

            logger.info("Successfully created microservice with ID: {}", createdMicroservice.getId());

            return ResponseEntity.ok(createdMicroservice);
        } catch (ResponseStatusException e) {
            logger.error("Failed to create microservice: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Failed to create microservice: {}", e.getMessage(), e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to create microservice: " + e.getMessage()
            );
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> getProjectById(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return projectService.getProjectById(id)
                .map(project -> {
                    if (!project.getTenant().equals(user.getTenant())) {
                        throw new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "You don't have access to this project"
                        );
                    }

                    // Add project type specific information
                    if (project.getProjectType() == ProjectType.MICROSERVICES_PACKAGE) {
                        logger.info("Returning microservices package with {} sub-projects",
                                project.getSubProjects().size());
                    } else if (project.getProjectType() == ProjectType.MICROSERVICES) {
                        logger.info("Returning microservice with parent package ID: {}",
                                project.getParentProject() != null ? project.getParentProject().getId() : "none");
                    }

                    return ResponseEntity.ok(project);
                })
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Project not found with id: " + id
                ));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CREATE_PROJECT')")
    public ResponseEntity<List<Project>> getAllProjects(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(projectService.getAllProjectsByTenant(user.getTenant()));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Project> updateProject(
            @PathVariable Long id,
            @ModelAttribute ProjectRequestDTO projectDTO,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();

            Project existingProject = projectService.getProjectById(id)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Project not found with id: " + id
                    ));

            if (!existingProject.getTenant().equals(user.getTenant())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You don't have access to this project"
                );
            }

            if (!existingProject.getCreator().equals(user) && user.getRole() != Role.PARTNER) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Only the project creator or partners can update the project"
                );
            }

            // Convert DTO to Project entity
            Project updatedProject = projectDTO.toProject();
            updatedProject.setId(id);
            updatedProject.setCreator(existingProject.getCreator());
            updatedProject.setTenant(existingProject.getTenant());

            return projectService.updateProject(id, updatedProject, projectDTO.getLogo())
                    .map(ResponseEntity::ok)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "Failed to update project"
                    ));
        } catch (Exception e) {
            logger.error("Failed to update project: {}", e.getMessage(), e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to update project: " + e.getMessage()
            );
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();

        Project project = projectService.getProjectById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Project not found with id: " + id
                ));

        if (!project.getTenant().equals(user.getTenant())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You don't have access to this project"
            );
        }

        if (!project.getCreator().equals(user) && user.getRole() != Role.PARTNER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the project creator or partners can delete the project"
            );
        }

        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<Project>> findProjectsByTag(
            @RequestParam String tag,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(projectService.findProjectsByTagAndTenant(tag, user.getTenant()));
    }

    @GetMapping("/search/by-primary-tag")
    public ResponseEntity<List<Project>> findProjectsByPrimaryTag(
            @RequestHeader("X-Primary-Tag") String primaryTag) {
        logger.info("Searching for projects with primary tag: {}", primaryTag);
        return ResponseEntity.ok(projectService.findProjectsByPrimaryTag(primaryTag));
    }

    @GetMapping("/public/by-tag/{tag}")
    public ResponseEntity<List<Project>> findProjectsByPrimaryTagPublic(
            @PathVariable String tag) {
        logger.info("Public API - Searching for projects with primary tag: {}", tag);
        return ResponseEntity.ok(projectService.findProjectsByPrimaryTag(tag));
    }

    @PostMapping("/{projectId}/users/{userId}")
    public ResponseEntity<Project> addUserToProject(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            Authentication authentication) {
        User authenticatedUser = (User) authentication.getPrincipal();
        logger.info("Add user to project request - Project: {}, User: {}, Requestor: {} (Role: {})",
                projectId, userId, authenticatedUser.getEmail(), authenticatedUser.getRole());

        try {
            // 1. Get project and verify it exists
            Project project = projectService.getProjectById(projectId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Project not found with id: " + projectId
                    ));
            logger.info("Project found - Name: {}, Tenant: {}, Creator: {}",
                    project.getName(), project.getTenant(), project.getCreator().getEmail());

            // 2. Get user to add and verify they exist
            User userToAdd = projectService.getUserById(userId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "User not found with id: " + userId
                    ));
            logger.info("User to add found - Email: {}, Role: {}, Tenant: {}",
                    userToAdd.getEmail(), userToAdd.getRole(), userToAdd.getTenant());

            // 3. Verify tenant access for authenticated user (must be from project's tenant)
            if (!authenticatedUser.getTenant().equals(project.getTenant())) {
                logger.warn("Tenant mismatch - Authenticated user tenant: {}, Project tenant: {}",
                        authenticatedUser.getTenant(), project.getTenant());
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Only users from the project's tenant can manage project members"
                );
            }

            // 4. Verify authorization (must be project creator or PARTNER)
            boolean isCreator = project.getCreator().getId().equals(authenticatedUser.getId());
            boolean isPartner = authenticatedUser.getRole() == Role.PARTNER;
            logger.info("Authorization check - Is Creator: {}, Is Partner: {}", isCreator, isPartner);

            if (!isCreator && !isPartner) {
                logger.warn("Unauthorized attempt - User: {} is neither creator nor PARTNER",
                        authenticatedUser.getEmail());
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Only project creator or partners can add users"
                );
            }

            // 5. Verify user role is appropriate (must be TESTER or DEVELOPER)
            if (userToAdd.getRole() != Role.TESTER && userToAdd.getRole() != Role.DEVELOPER && userToAdd.getRole() != Role.MANAGER) {
                logger.warn("Invalid role for user to add: {}", userToAdd.getRole());
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Only TESTER or DEVELOPER roles can be added to projects"
                );
            }

            // 6. Check if user is already in project
            boolean isAlreadyMember = project.getProjectUsers().stream()
                    .anyMatch(pu -> pu.getUser().getId().equals(userId));

            if (isAlreadyMember) {
                logger.warn("User {} is already a member of project {}", userToAdd.getEmail(), project.getName());
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "User is already a member of this project"
                );
            }

            // 7. Add user to project
            logger.info("Adding user {} to project {}", userToAdd.getEmail(), project.getName());
            Project updatedProject = projectService.addUserToProject(projectId, userId);
            logger.info("Successfully added user {} to project {}", userToAdd.getEmail(), project.getName());
            return ResponseEntity.ok(updatedProject);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error adding user {} to project {}: {}", userId, projectId, e.getMessage(), e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to add user to project: " + e.getMessage()
            );
        }
    }

    @DeleteMapping("/{projectId}/users/{userId}")
    public ResponseEntity<Project> removeUserFromProject(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            Authentication authentication) {
        User authenticatedUser = (User) authentication.getPrincipal();

        Project project = projectService.getProjectById(projectId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Project not found with id: " + projectId
                ));

        if (!project.getTenant().equals(authenticatedUser.getTenant())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You don't have access to this project"
            );
        }

        if (!project.getCreator().equals(authenticatedUser) && authenticatedUser.getRole() != Role.PARTNER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the project creator or partners can remove users from the project"
            );
        }

        return ResponseEntity.ok(projectService.removeUserFromProject(projectId, userId));
    }

    @GetMapping("/{projectId}/users")
    public ResponseEntity<Set<User>> getProjectUsers(
            @PathVariable Long projectId,
            Authentication authentication) {
        User authenticatedUser = (User) authentication.getPrincipal();

        Project project = projectService.getProjectById(projectId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Project not found with id: " + projectId
                ));

        if (!project.getTenant().equals(authenticatedUser.getTenant())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You don't have access to this project"
            );
        }

        return ResponseEntity.ok(projectService.getProjectUsers(projectId));
    }

    //without authorization
    @GetMapping("/public/{projectId}/members")
    public ResponseEntity<Set<User>> getProjectMembers(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(projectService.getProjectUsers(projectId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Set<Project>> getUserProjects(
            @PathVariable Long userId,
            Authentication authentication) {
        User authenticatedUser = (User) authentication.getPrincipal();

        // Verify user exists
        User targetUser = projectService.getUserById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found with id: " + userId
                ));

        // Verify tenant access
        if (!targetUser.getTenant().equals(authenticatedUser.getTenant())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You don't have access to this user's projects"
            );
        }

        return ResponseEntity.ok(projectService.getProjectsUser(userId));
    }

    @GetMapping("/packages/{packageId}/microservices")
    public ResponseEntity<List<Project>> getMicroservicesInPackage(
            @PathVariable Long packageId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();

        // First check if user has access to the package
        Project packageProject = projectService.getProjectById(packageId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Package not found with id: " + packageId
                ));

        if (!packageProject.getTenant().equals(user.getTenant())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You don't have access to this package"
            );
        }

        return ResponseEntity.ok(projectService.getMicroservicesForPackage(packageId));
    }

    @GetMapping("/types/{projectType}")
    public ResponseEntity<List<Project>> getProjectsByType(
            @PathVariable ProjectType projectType,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<Project> allProjects = projectService.getAllProjectsByTenant(user.getTenant());
        List<Project> filteredProjects = allProjects.stream()
                .filter(p -> p.getProjectType() == projectType)
                .collect(Collectors.toList());
        return ResponseEntity.ok(filteredProjects);
    }
}