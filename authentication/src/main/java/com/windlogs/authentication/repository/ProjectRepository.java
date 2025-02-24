package com.windlogs.authentication.repository;

import com.windlogs.authentication.entity.Project;
import com.windlogs.authentication.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    Optional<Project> findByName(String name);
}
