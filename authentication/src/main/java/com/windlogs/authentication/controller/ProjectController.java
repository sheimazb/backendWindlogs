package com.windlogs.authentication.controller;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.windlogs.authentication.entity.Project;
import com.windlogs.authentication.entity.User;
import com.windlogs.authentication.entity.Role;
import com.windlogs.authentication.service.ProjectService;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {
    private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);
    private final ProjectService projectService;

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('CREATE_PROJECT')")
    public ResponseEntity<Project> createProject(@RequestBody Project project, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            if (user.getRole() != Role.PARTNER) {
                logger.warn("Unauthorized attempt to create project by non-partner user: {}", user.getEmail());
                throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only partners can create projects"
                );
            }

            // Ensure project tenant matches partner's tenant
            String partnerTenant = user.getTenant();
            if (project.getTenant() != null && !project.getTenant().equals(partnerTenant)) {
                logger.warn("Attempt to create project with mismatched tenant. User tenant: {}, Project tenant: {}", 
                    partnerTenant, project.getTenant());
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Project tenant must match partner's tenant"
                );
            }

            // Initialize collections if null
            if (project.getTags() == null) {
                project.setTags(new HashSet<>());
            }
            if (project.getAllowedRoles() == null) {
                project.setAllowedRoles(new HashSet<>());
            }
            if (project.getDocumentationUrls() == null) {
                project.setDocumentationUrls(new ArrayList<>());
            }

            // Set default values
            project.setTenant(partnerTenant);
            project.setCreator(user);
            project.setProgressPercentage(0);
            project.setMembersCount(1); // Starting with creator
            if (project.getPayed() == null) {
                project.setPayed(false);
            }
            
            logger.info("Creating new project for partner: {} in tenant: {}", user.getEmail(), partnerTenant);
            Project createdProject = projectService.createProject(project);
            
            // Add the creator as a project member
            createdProject.addMember(user, true);
            
            return ResponseEntity.ok(projectService.getProjectById(createdProject.getId())
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to retrieve created project"
                )));
                
        } catch (Exception e) {
            logger.error("Failed to create project: {}", e.getMessage(), e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to create project: " + e.getMessage()
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

    @PutMapping("/{id}")
    public ResponseEntity<Project> updateProject(
            @PathVariable Long id,
            @RequestBody Project updatedProject,
            Authentication authentication) {
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

        updatedProject.setCreator(existingProject.getCreator());
        updatedProject.setTenant(existingProject.getTenant());
        
        return projectService.updateProject(id, updatedProject)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to update project"
                ));
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
}
