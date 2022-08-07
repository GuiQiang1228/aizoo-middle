package aizoo.repository;

import aizoo.common.DownloadStatus;
import aizoo.domain.ExperimentJob;
import aizoo.common.JobStatus;
import aizoo.domain.User;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Map;
import java.util.Set;


public interface ExperimentJobDAO extends JpaRepository<ExperimentJob, Long>, JpaSpecificationExecutor<ExperimentJob> {

    @Query(value = "SELECT * FROM experiment_job WHERE experiment_job.job_key = ?1 ORDER BY experiment_job.update_time DESC limit 1",
            nativeQuery = true)
    ExperimentJob findByJobKey(String jobKey);

    @Query(value = "SELECT experiment_job.*,graph.name FROM experiment_job JOIN graph ON experiment_job.graph_id=graph.id JOIN user ON experiment_job.user_id=user.id WHERE IF(?1 != '',experiment_job.name like CONCAT('%',?1,'%'),1=1) AND IF(?2 != '',experiment_job.description LIKE CONCAT('%',?2,'%'),1=1)AND IF(?3 !='',experiment_job.job_status=?3,1=1) AND IF(?4 !='',graph.name LIKE CONCAT('%',?4,'%'),1=1) AND IF(?5 !='',experiment_job.update_time>=?5,1=1) AND IF(?6 !='',experiment_job.update_time<=?6,1=1) AND user.username=?7 ORDER BY job_status != 'RUNNING', experiment_job.update_time DESC",
            countQuery = "SELECT COUNT(1) FROM experiment_job JOIN graph ON experiment_job.graph_id=graph.id JOIN user ON experiment_job.user_id=user.id WHERE IF(?1 != '',experiment_job.name like CONCAT('%',?1,'%'),1=1) AND IF(?2 != '',experiment_job.description LIKE CONCAT('%',?2,'%'),1=1)AND IF(?3 !='',experiment_job.job_status=?3,1=1) AND IF(?4 !='',graph.name LIKE CONCAT('%',?4,'%'),1=1) AND IF(?5 !='',experiment_job.update_time>=?5,1=1) AND IF(?6 !='',experiment_job.update_time<=?6,1=1) AND user.username=?7 ORDER BY job_status != 'RUNNING', experiment_job.update_time DESC",
            nativeQuery = true)
    Page<ExperimentJob> searchJob(String jobName, String description, String jobStatus, String graphName, String startUpdateTime, String endUpdateTime, String userName, Pageable pageable);

    @Query(value = "SELECT experiment_job.*,graph.name FROM experiment_job JOIN graph ON experiment_job.graph_id=graph.id JOIN user ON experiment_job.user_id=user.id WHERE IF(?1 != '',experiment_job.name like CONCAT('%',?1,'%'),1=1) AND IF(?2 != '',experiment_job.description LIKE CONCAT('%',?2,'%'),1=1)AND IF(?3 !='',experiment_job.job_status=?3,1=1) AND IF(?4 !='',graph.name LIKE CONCAT('%',?4,'%'),1=1) AND IF(?5 !='',experiment_job.update_time>=?5,1=1) AND IF(?6 !='',experiment_job.update_time<=?6,1=1) AND IF(?7 !='',user.username=?7,1=1) ORDER BY job_status != 'RUNNING', experiment_job.update_time DESC",
            countQuery = "SELECT COUNT(1) FROM experiment_job JOIN graph ON experiment_job.graph_id=graph.id JOIN user ON experiment_job.user_id=user.id WHERE IF(?1 != '',experiment_job.name like CONCAT('%',?1,'%'),1=1) AND IF(?2 != '',experiment_job.description LIKE CONCAT('%',?2,'%'),1=1)AND IF(?3 !='',experiment_job.job_status=?3,1=1) AND IF(?4 !='',graph.name LIKE CONCAT('%',?4,'%'),1=1) AND IF(?5 !='',experiment_job.update_time>=?5,1=1) AND IF(?6 !='',experiment_job.update_time<=?6,1=1) AND IF(?7 !='',user.username=?7,1=1) ORDER BY job_status != 'RUNNING', experiment_job.update_time DESC",
            nativeQuery = true)
    Page<ExperimentJob> adminSearchJob(String jobName, String description, String jobStatus, String graphName, String startUpdateTime, String endUpdateTime, String owner, Pageable pageable);

    List<ExperimentJob> findByJobStatusIsIn(List<JobStatus> jobStatuses);

    @CacheEvict
    @Query("select j.downloadStatus from ExperimentJob as j where j.id =:jobId")
    DownloadStatus findDownloadStatusById(long jobId);

    int countByJobStatusAndUserUsername(JobStatus jobStatus, String username);

    int countByUserUsername(String username);

    int countByJobStatus(JobStatus jobStatus);

    @Query(nativeQuery = true,
            value = "SELECT a.date_time, coalesce(b.count_num,0) AS count_num FROM " +
                    "( SELECT DATE_FORMAT(CURDATE(), '%Y-%m') AS date_time " +
                    "UNION ALL SELECT DATE_FORMAT((CURDATE() - INTERVAL 1 MONTH), '%Y-%m') AS date_time " +
                    "UNION ALL SELECT DATE_FORMAT((CURDATE() - INTERVAL 2 MONTH), '%Y-%m') AS date_time " +
                    "UNION ALL SELECT DATE_FORMAT((CURDATE() - INTERVAL 3 MONTH), '%Y-%m') AS date_time " +
                    "UNION ALL SELECT DATE_FORMAT((CURDATE() - INTERVAL 4 MONTH), '%Y-%m') AS date_time) a " +
                    "LEFT JOIN (SELECT DATE_FORMAT(j.update_time,'%Y-%m') AS DATE, COUNT(DATE_FORMAT(j.update_time,'%Y-%m')) AS count_num FROM experiment_job j, `user` u " +
                    "WHERE j.user_id = u.id and u.username =:username and j.job_status =:jobStatus " +
                    "GROUP BY DATE_FORMAT(j.update_time,'%Y-%m')) b ON a.date_time = b.date")
    List<Map<String, Object>> countByMonthAndJobStatus(String username, String jobStatus);

    Set<ExperimentJob> findByJobStatus(JobStatus jobStatus);

    List<ExperimentJob> findByComponentIdAndUserUsernameAndJobStatus(Long id, String username, JobStatus jobStatus);

    List<ExperimentJob> findByComponentId(Long id);


   /* @Query(nativeQuery = true, value = "SELECT COUNT(user_id) FROM experiment_job ej JOIN user ON user.id = ej.user_id WHERE user.username = ?1")
    int findByUsername(String username);*/
}
