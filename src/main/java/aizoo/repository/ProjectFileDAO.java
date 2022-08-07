package aizoo.repository;

import aizoo.domain.ProjectFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectFileDAO extends JpaRepository<ProjectFile, Long>, PagingAndSortingRepository<ProjectFile, Long> {
    Page<ProjectFile> findByProjectIdAndUserUsername(long projectId, String name, Pageable pageable);

    Page<ProjectFile> findByUserUsername(String name, Pageable pageable);
}
