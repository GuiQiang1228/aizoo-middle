package aizoo.repository;

import aizoo.common.JobStatus;
import aizoo.domain.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ApplicationDAO extends JpaRepository<Application, Long>, PagingAndSortingRepository<Application, Long> {

    @Query(value = "SELECT app.*,graph.name FROM application app JOIN graph ON app.graph_id=graph.id JOIN user ON app.user_id=user.id WHERE IF(?1 != '',app.name like CONCAT('%',?1,'%'),1=1) AND IF(?2 != '',app.description LIKE CONCAT('%',?2,'%'),1=1) AND IF(?3 !='',graph.name LIKE CONCAT('%',?3,'%'),1=1) AND IF(?4 !='',app.job_status=?4,1=1)  AND IF(?5 !='',app.update_time>=?5,1=1) AND IF(?6 !='',app.update_time<=?6,1=1) AND user.username=?7 ORDER BY job_status != 'RUNNING', app.update_time DESC",
            countQuery = "SELECT COUNT(1) FROM application app JOIN graph ON app.graph_id=graph.id JOIN user ON app.user_id=user.id WHERE IF(?1 != '',app.name like CONCAT('%',?1,'%'),1=1) AND IF(?2 != '',app.description LIKE CONCAT('%',?2,'%'),1=1) AND IF(?3 !='',graph.name LIKE CONCAT('%',?3,'%'),1=1) AND IF(?4 !='',app.job_status=?4,1=1)  AND IF(?5 !='',app.update_time>=?5,1=1) AND IF(?6 !='',app.update_time<=?6,1=1) AND user.username=?7 ORDER BY job_status != 'RUNNING', app.update_time DESC",
            nativeQuery = true)
    Page<Application> searchApplication(String appName, String description, String graphName, String applicationStatus, String startUpdateTime, String endUpdateTime, String userName, Pageable pageable);

    @Query(value = "SELECT app.*,graph.name FROM application app JOIN graph ON app.graph_id=graph.id JOIN user ON app.user_id=user.id WHERE IF(?1 != '',app.name like CONCAT('%',?1,'%'),1=1) AND IF(?2 != '',app.description LIKE CONCAT('%',?2,'%'),1=1) AND IF(?3 !='',graph.name LIKE CONCAT('%',?3,'%'),1=1) AND IF(?4 !='',app.job_status=?4,1=1)  AND IF(?5 !='',app.update_time>=?5,1=1) AND IF(?6 !='',app.update_time<=?6,1=1) AND IF(?7 !='',user.username=?7,1=1) ORDER BY job_status != 'RUNNING', app.update_time DESC",
            countQuery = "SELECT COUNT(1) FROM application app JOIN graph ON app.graph_id=graph.id JOIN user ON app.user_id=user.id WHERE IF(?1 != '',app.name like CONCAT('%',?1,'%'),1=1) AND IF(?2 != '',app.description LIKE CONCAT('%',?2,'%'),1=1) AND IF(?3 !='',graph.name LIKE CONCAT('%',?3,'%'),1=1) AND IF(?4 !='',app.job_status=?4,1=1)  AND IF(?5 !='',app.update_time>=?5,1=1) AND IF(?6 !='',app.update_time<=?6,1=1) AND IF(?7 !='',user.username=?7,1=1) ORDER BY job_status != 'RUNNING', app.update_time DESC",
            nativeQuery = true)
    Page<Application> adminSearchApplication(String appName, String description, String graphName, String applicationStatus, String startUpdateTime, String endUpdateTime, String owner, Pageable pageable);

    int countByUserUsername(String username);

    List<Application> findByUserUsername(String username);

    @Query(value = "SELECT * FROM application WHERE application.job_key = ?1 ORDER BY application.update_time DESC limit 1",
            nativeQuery = true)
    Application findByJobKey(String jobKey);

    Set<Application> findByJobStatus(JobStatus jobStatus);

    List<Application> findByJobStatusIsIn(List<JobStatus> jobStatuses);

}
