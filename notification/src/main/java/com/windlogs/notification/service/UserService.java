package com.windlogs.notification.service;

import com.windlogs.notification.dto.ProjectDTO;
import com.windlogs.notification.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Service for user-related operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final RestTemplate restTemplate;

    @Value("${authentication.service.url:http://localhost:8088}")
    private String authServiceUrl;

    /**
     * Get user emails by tenant
     *
     * @param tenant The tenant
     * @return List of user emails for the tenant
     */
    public List<String> getUserEmailsByTenant(String tenant) {
        log.info("Getting user emails for tenant: {}", tenant);

        try {
            // Get managers for the tenant from the authentication service
            String url = authServiceUrl + "/api/v1/auth/users/tenant/" + tenant + "/role/MANAGER";
            log.info("Calling authentication service: {}", url);

            try {
                ResponseEntity<List<UserDTO>> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<List<UserDTO>>() {
                        }
                );

                List<UserDTO> managers = response.getBody();

                log.info("Authentication service response status: {}", response.getStatusCode());

                if (managers == null || managers.isEmpty()) {
                    log.warn("No managers found for tenant: {}", tenant);
                    return new ArrayList<>();
                }

                // Extract emails from the managers
                List<String> managerEmails = managers.stream()
                        .map(UserDTO::getEmail)
                        .toList();

                log.info("Found {} managers for tenant: {}", managerEmails.size(), tenant);
                log.info("Manager emails: {}", managerEmails);
                return managerEmails;
            } catch (Exception e) {
                log.error("Error calling authentication service: {}", e.getMessage(), e);
                throw e;
            }
        } catch (Exception e) {
            log.error("Error getting managers for tenant {}: {}", tenant, e.getMessage(), e);
            return new ArrayList<>(); // Return empty list as fallback in case of error
        }
    }

    /**
     * Get project manager's email for a specific project
     *
     * @param projectId The project ID
     * @return The project manager's email, or empty if not found
     */
    public Optional<String> getProjectManagerEmail(Long projectId) {
        log.info("Getting project manager for project ID: {}", projectId);

        try {
            // First, get the project details to find its creator/manager
            String projectUrl = authServiceUrl + "/api/v1/projects/public/" + projectId;
            log.info("Calling authentication service for project details: {}", projectUrl);

            try {
                ProjectDTO project = restTemplate.getForObject(projectUrl, ProjectDTO.class);

                if (project == null) {
                    log.warn("Project not found with ID: {}", projectId);
                    return Optional.empty();
                }

                log.info("Found project: {}, tenant: {}", project.getName(), project.getTenant());

                // Get project members to find the project manager/creator
                String membersUrl = authServiceUrl + "/api/v1/projects/public/" + projectId + "/members";
                log.info("Calling authentication service for project members: {}", membersUrl);

                ResponseEntity<List<UserDTO>> response = restTemplate.exchange(
                        membersUrl,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<List<UserDTO>>() {
                        }
                );

                List<UserDTO> members = response.getBody();

                if (members == null || members.isEmpty()) {
                    log.warn("No members found for project ID: {}", projectId);

                    // Fallback to getting any manager in the project's tenant
                    List<String> tenantManagers = getUserEmailsByTenant(project.getTenant());
                    if (!tenantManagers.isEmpty()) {
                        log.info("Using tenant manager as fallback: {}", tenantManagers.get(0));
                        return Optional.of(tenantManagers.get(0));
                    }
                    return Optional.empty();
                }

                // First look for users with MANAGER role
                Optional<String> managerEmail = members.stream()
                        .filter(user -> "MANAGER".equalsIgnoreCase(user.getRole()))
                        .map(UserDTO::getEmail)
                        .findFirst();

                // If no manager found, use the first user (likely the creator)
                if (managerEmail.isEmpty() && !members.isEmpty()) {
                    managerEmail = Optional.of(members.get(0).getEmail());
                    log.info("No manager found, using first project member: {}", managerEmail.get());
                } else if (managerEmail.isPresent()) {
                    log.info("Found project manager: {}", managerEmail.get());
                }

                return managerEmail;
            } catch (Exception e) {
                log.error("Error calling authentication service for project details: {}", e.getMessage(), e);
                throw e;
            }
        } catch (Exception e) {
            log.error("Error getting project manager for project ID {}: {}", projectId, e.getMessage(), e);
            return Optional.empty(); // Return empty Optional as fallback in case of error
        }
    }

} 