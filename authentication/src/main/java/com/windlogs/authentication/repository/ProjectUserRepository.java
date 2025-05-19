package com.windlogs.authentication.repository;

import com.windlogs.authentication.entity.ProjectUser;
import com.windlogs.authentication.entity.ProjectUserId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectUserRepository extends JpaRepository<ProjectUser, ProjectUserId> {
    
    /**
     * Count the number of projects a user is a member of within a specific tenant
     * @param userId The ID of the user
     * @param tenant The tenant identifier
     * @return Number of projects the user is a member of
     */
    @Query("SELECT COUNT(pu) FROM ProjectUser pu JOIN pu.project p WHERE pu.user.id = :userId AND p.tenant = :tenant")
    Long countProjectsByUserIdAndTenant(@Param("userId") Long userId, @Param("tenant") String tenant);
}