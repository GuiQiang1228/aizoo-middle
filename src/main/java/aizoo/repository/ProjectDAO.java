package aizoo.repository;

import aizoo.domain.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ProjectDAO extends JpaRepository<Project, Long>, PagingAndSortingRepository<Project, Long> {
    List<Project> findByUserUsername(String name);

    Page<Project> findByUserUsername(String name, Pageable pageable);

    @Query(value = "SELECT project.* FROM project JOIN user ON project.user_id=user.id WHERE IF(?1 !='', project.privacy LIKE CONCAT('%',?1,'%'),1=1) AND IF(?2 !='', project.description LIKE CONCAT('%',?2,'%'),1=1) AND IF(?3 !='',project.update_time>=?3,1=1) AND IF(?4 !='',project.update_time<=?4,1=1)  AND IF(?5 !='', project.name LIKE CONCAT('%',?5,'%'),1=1) AND user.username=?6 ORDER BY project.create_time DESC",
            countQuery = "SELECT COUNT(1) FROM project JOIN user ON project.user_id=user.id WHERE IF(?1 !='', project.privacy LIKE CONCAT('%',?1,'%'),1=1) AND IF(?2 !='', project.description LIKE CONCAT('%',?2,'%'),1=1) AND IF(?3 !='',project.update_time>=?3,1=1) AND IF(?4 !='',project.update_time<=?4,1=1) AND IF(?5 !='', project.name LIKE CONCAT('%',?5,'%'),1=1) AND user.username=?6 ORDER BY project.create_time DESC",
            nativeQuery = true)
    Page<Project> searchProject(String privacy, String desc, String startUpdateTime, String endUpdateTime, String name, String userName, Pageable pageable);

    @Query(value = "INSERT INTO project_application VALUES(?1,?2)",
            nativeQuery = true)
    @Modifying
    @Transactional
    void addApplicationForProject(long projectId, long applicationId);

    @Query(value = "INSERT INTO project_experiment_job VALUES(?1,?2)",
            nativeQuery = true)
    @Modifying
    @Transactional
    void addExperimentJobForProject(long projectId, long experimentJobId);

    @Query(value = "INSERT INTO project_graph VALUES(?1,?2)",
            nativeQuery = true)
    @Modifying
    @Transactional
    void addGraphForProject(long projectId, long graphId);

    @Query(value = "INSERT INTO project_service VALUES(?1,?2)",
            nativeQuery = true)
    @Modifying
    @Transactional
    void addServiceForProject(long projectId, long serviceId);

    @Query(value = "INSERT INTO project_service_job VALUES(?1,?2)",
            nativeQuery = true)
    @Modifying
    @Transactional
    void addServiceJobForProject(long projectId, long serviceJobId);

    @Query(value = "INSERT INTO project_component VALUES(?1,?2)",
            nativeQuery = true)
    @Modifying
    @Transactional
    void addComponentForProject(long projectId, long componentId);

    @Query(value = "INSERT INTO project_datasource VALUES(?1,?2)",
            nativeQuery = true)
    @Modifying
    @Transactional
    void addDatasourceForProject(long projectId, long datasourceId);

    @Query(value = "INSERT INTO project_code VALUES(?1,?2)",
            nativeQuery = true)
    @Modifying
    @Transactional
    void addCodeForProject(long projectId, long codeId);

    @Query(value = "INSERT INTO project_mirror VALUES(?1,?2)",
            nativeQuery = true)
    @Modifying
    @Transactional
    void addMirrorForProject(long projectId, long mirrorId);

    @Query(value = "INSERT INTO project_mirror_job VALUES(?1,?2)",
            nativeQuery = true)
    @Modifying
    @Transactional
    void addMirrorJobForProject(long projectId, long mirrorJobId);

    @Query(value = "DELETE FROM project_application where project_id=?1 and application_id=?2",
            nativeQuery = true)
    @Modifying
    @Transactional
    void deleteApplicationFromProject(long projectId, long applicationId);

    @Query(value = "DELETE FROM project_experiment_job where project_id=?1 and experiment_job_id=?2",
            nativeQuery = true)
    @Modifying
    @Transactional
    void deleteExperimentJobFromProject(long projectId, long experimentJobId);

    @Query(value = "DELETE FROM project_graph where project_id=?1 and graph_id=?2",
            nativeQuery = true)
    @Modifying
    @Transactional
    void deleteGraphFromProject(long projectId, long graphId);

    @Query(value = "DELETE FROM project_service where project_id=?1 and service_id=?2",
            nativeQuery = true)
    @Modifying
    @Transactional
    void deleteServiceFromProject(long projectId, long serviceId);

    @Query(value = "DELETE FROM project_service_job where project_id=?1 and service_job_id=?2",
            nativeQuery = true)
    @Modifying
    @Transactional
    void deleteServiceJobFromProject(long projectId, long serviceJobId);

    @Query(value = "DELETE FROM project_component where project_id=?1 and component_id=?2",
            nativeQuery = true)
    @Modifying
    @Transactional
    void deleteComponentFromProject(long projectId, long componentId);

    @Query(value = "DELETE FROM project_datasource where project_id=?1 and datasource_id=?2",
            nativeQuery = true)
    @Modifying
    @Transactional
    void deleteDatasourceFromProject(long projectId, long datasourceId);

    @Query(value = "DELETE FROM project_code where project_id=?1 and code_id=?2",
            nativeQuery = true)
    @Modifying
    @Transactional
    void deleteCodeFromProject(long projectId, Long codeId);

    @Query(value = "DELETE FROM project_mirror where project_id=?1 and mirror_id=?2",
            nativeQuery = true)
    @Modifying
    @Transactional
    void deleteMirrorFromProject(long projectId, Long mirrorId);

    @Query(value = "DELETE FROM project_mirror_job where project_id=?1 and mirror_job_id=?2",
            nativeQuery = true)
    @Modifying
    @Transactional
    void deleteMirrorJobFromProject(long projectId, Long mirrorJobId);
}
