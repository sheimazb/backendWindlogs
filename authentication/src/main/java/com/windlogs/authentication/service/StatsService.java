package com.windlogs.authentication.service;

import org.springframework.stereotype.Service;

import com.windlogs.authentication.repository.ProjectUserRepository;
import com.windlogs.authentication.repository.UserRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class StatsService {
    private final UserRepository userRepository;
    private final ProjectUserRepository projectUserRepository;

    public long totalPartners() {
        return userRepository.countAllPartners();
    }

    public long activePartners() {
        return userRepository.countActivePartners();
    }

    public long lockedPartners() {
        return userRepository.countLockedPartners();
    }
    
    /**
     * Count the number of projects a user is a member of within a tenant
     * @param userId The ID of the user
     * @param tenant The tenant identifier
     * @return Number of projects the user is a member of
     */
    public Long countUserProjects(Long userId, String tenant) {
        return projectUserRepository.countProjectsByUserIdAndTenant(userId, tenant);
    }
}