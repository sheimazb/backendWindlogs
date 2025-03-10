package com.windlogs.authentication.repository;

import com.windlogs.authentication.entity.ProjectUser;
import com.windlogs.authentication.entity.ProjectUserId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectUserRepository extends JpaRepository<ProjectUser, ProjectUserId> {
} 