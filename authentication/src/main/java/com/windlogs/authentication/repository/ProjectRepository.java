package com.windlogs.authentication.repository;

import com.windlogs.authentication.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    boolean  existsByPrimaryTag(String primaryTag);
    boolean existsByPrimaryTagIgnoreCase(String primaryTag);
    List<Project> findByTenant(String tenant);
    List<Project> findByTagsContainingAndTenant(String tag, String tenant);
    List<Project> findByPrimaryTag(String primaryTag);
    List<Project> findByParentProjectId(Long parentId);
    
}