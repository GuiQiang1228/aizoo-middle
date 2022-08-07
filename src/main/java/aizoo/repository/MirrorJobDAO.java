package aizoo.repository;

import aizoo.common.JobStatus;
import aizoo.domain.ExperimentJob;
import aizoo.domain.MirrorJob;
import aizoo.domain.ServiceJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface MirrorJobDAO extends JpaRepository<MirrorJob, Long>, PagingAndSortingRepository<MirrorJob, Long> {

    @Query(value = "SELECT * FROM mirror_job WHERE mirror_job.job_key = ?1 ORDER BY mirror_job.update_time DESC limit 1",
            nativeQuery = true)
    MirrorJob findByJobKey(String jobKey);

    List<MirrorJob> findByJobStatusIsIn(List<JobStatus> jobStatuses);

    int countByUserUsername(String username);

    @Query(value = "SELECT mirror_job.* FROM mirror_job JOIN user ON mirror_job.user_id=user.id WHERE IF(?1 != '',mirror_job.name like CONCAT('%',?1,'%'),1=1) AND IF(?2 != '',mirror_job.description LIKE CONCAT('%',?2,'%'),1=1)AND IF(?3 !='',mirror_job.job_status=?3,1=1)  AND IF(?4 !='',mirror_job.update_time>=?4,1=1) AND IF(?5 !='',mirror_job.update_time<=?5,1=1) AND user.username=?6 ORDER BY job_status != 'RUNNING', mirror_job.update_time DESC",
            countQuery = "SELECT COUNT(1) FROM mirror_job JOIN user ON mirror_job.user_id=user.id WHERE IF(?1 != '',mirror_job.name like CONCAT('%',?1,'%'),1=1) AND IF(?2 != '',mirror_job.description LIKE CONCAT('%',?2,'%'),1=1)AND IF(?3 !='',mirror_job.job_status=?3,1=1)  AND IF(?4 !='',mirror_job.update_time>=?4,1=1) AND IF(?5 !='',mirror_job.update_time<=?5,1=1) AND user.username=?6 ORDER BY job_status != 'RUNNING', mirror_job.update_time DESC",
            nativeQuery = true)
    Page<MirrorJob> searchMirrorJob(String jobName, String description, String jobStatus, String startUpdateTime, String endUpdateTime, String userName, Pageable pageable);

    @Query(
            value = "select j FROM MirrorJob as j where j.jobStatus=?1 AND (j.ip IS NULL OR j.port IS NULL)"
    )
    Set<MirrorJob> findJobsForIpPortCheck(JobStatus jobStatus);

}
