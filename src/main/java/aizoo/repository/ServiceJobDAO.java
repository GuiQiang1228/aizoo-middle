package aizoo.repository;

import aizoo.common.DownloadStatus;
import aizoo.common.JobStatus;
import aizoo.domain.Service;
import aizoo.domain.ServiceJob;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import javax.persistence.Column;
import javax.persistence.ColumnResult;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ServiceJobDAO extends JpaRepository<ServiceJob, Long>, JpaSpecificationExecutor<ServiceJob> {

    @Query(value = "SELECT * FROM service_job WHERE service_job.job_key = ?1 ORDER BY service_job.update_time DESC limit 1",
            nativeQuery = true)
    ServiceJob findByJobKey(String jobKey);

    @Query(value = "SELECT service_job.*,graph.name FROM service_job JOIN graph ON service_job.graph_id=graph.id JOIN user ON service_job.user_id=user.id WHERE IF(?1 != '',service_job.name like CONCAT('%',?1,'%'),1=1) AND IF(?2 != '',service_job.description LIKE CONCAT('%',?2,'%'),1=1)AND IF(?3 !='',service_job.job_status=?3,1=1) AND IF(?4 !='',graph.name LIKE CONCAT('%',?4,'%'),1=1) AND IF(?5 !='',service_job.update_time>=?5,1=1)  AND IF(?6 !='',service_job.update_time<=?6,1=1)  AND user.username=?7 ORDER BY job_status != 'RUNNING', service_job.update_time DESC",
            countQuery = "SELECT COUNT(1) FROM service_job JOIN graph ON service_job.graph_id=graph.id JOIN user ON service_job.user_id=user.id WHERE IF(?1 != '',service_job.name like CONCAT('%',?1,'%'),1=1) AND IF(?2 != '',service_job.description LIKE CONCAT('%',?2,'%'),1=1)AND IF(?3 !='',service_job.job_status=?3,1=1) AND IF(?4 !='',graph.name LIKE CONCAT('%',?4,'%'),1=1) AND IF(?5 !='',service_job.update_time>=?5,1=1)  AND IF(?6 !='',service_job.update_time<=?6,1=1)  AND user.username=?7 ORDER BY job_status != 'RUNNING', service_job.update_time DESC",
            nativeQuery = true)
    Page<ServiceJob> searchServiceJob(String serviceName, String description, String jobStatus, String graphName, String startUpdateTime, String endUpdateTime, String userName, Pageable pageable);

    @Query(value = "SELECT service_job.*,graph.name FROM service_job JOIN graph ON service_job.graph_id=graph.id JOIN user ON service_job.user_id=user.id WHERE IF(?1 != '',service_job.name like CONCAT('%',?1,'%'),1=1) AND IF(?2 != '',service_job.description LIKE CONCAT('%',?2,'%'),1=1)AND IF(?3 !='',service_job.job_status=?3,1=1) AND IF(?4 !='',graph.name LIKE CONCAT('%',?4,'%'),1=1) AND IF(?5 !='',service_job.update_time>=?5,1=1)  AND IF(?6 !='',service_job.update_time<=?6,1=1)  AND IF(?7 !='',user.username=?7,1=1) ORDER BY job_status != 'RUNNING', service_job.update_time DESC",
            countQuery = "SELECT COUNT(1) FROM service_job JOIN graph ON service_job.graph_id=graph.id JOIN user ON service_job.user_id=user.id WHERE IF(?1 != '',service_job.name like CONCAT('%',?1,'%'),1=1) AND IF(?2 != '',service_job.description LIKE CONCAT('%',?2,'%'),1=1)AND IF(?3 !='',service_job.job_status=?3,1=1) AND IF(?4 !='',graph.name LIKE CONCAT('%',?4,'%'),1=1) AND IF(?5 !='',service_job.update_time>=?5,1=1)  AND IF(?6 !='',service_job.update_time<=?6,1=1) AND IF(?7 !='',user.username=?7,1=1) ORDER BY job_status != 'RUNNING', service_job.update_time DESC",
            nativeQuery = true)
    Page<ServiceJob> adminSearchServiceJob(String serviceName, String description, String jobStatus, String graphName, String startUpdateTime, String endUpdateTime, String owner, Pageable pageable);


    //    @Query(value = "SELECT j.id from service_job j JOIN user ON j.user_id=user.id JOIN service ON j.service_id=service.id WHERE service.id=?1 and user.username=?2 and j.service_status=?3 ORDER BY j.create_time DESC",
//            nativeQuery = true)
    List<ServiceJob> findByServiceIdAndUserUsernameAndJobStatusAndIpNotNull(Long id, String username, JobStatus status);

    Set<ServiceJob> findByJobStatus(JobStatus jobStatus);

    @Query(
            value = "select j FROM ServiceJob as j where j.jobStatus=?1 AND (j.ip IS NULL OR j.port IS NULL)"
    )
    Set<ServiceJob> findJobsForIpPortCheck(JobStatus jobStatus);

    List<ServiceJob> findByJobStatusIsIn(List<JobStatus> jobStatus);

    int countByUserUsername(String username);

    @CacheEvict
    @Query("select j.downloadStatus from ServiceJob as j where j.id =:jobId")
    DownloadStatus findDownloadStatusById(long jobId);

    int countByJobStatusAndUserUsername(JobStatus jobStatus, String username);

    List<ServiceJob> findByServiceId(Long id);

    /*@Query(nativeQuery = true, value = "SELECT COUNT(user_id) FROM service_job sj JOIN user ON user.id = sj.user_id WHERE user.username = ?1")
    int findByUsername(String username);*/
}
