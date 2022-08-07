package aizoo.repository;

import aizoo.common.ResourceType;
import aizoo.domain.ResourceUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface ResourceUsageDAO extends JpaRepository<ResourceUsage, Long>, PagingAndSortingRepository<ResourceUsage, Long> {

    @Query(value = "SELECT IFNULL(SUM(ru.used_amount),0) FROM resource_usage ru join user on user.id = ru.user_id WHERE ru.resource_type=?1 and user.username=?2 and ru.released=0",
            nativeQuery = true)
    Double getAllTypeResourceUsage(String resourceType, String username);

    @Query(value = "SELECT IFNULL( IFNULL( IFNULL( (SELECT e.job_status FROM experiment_job e WHERE e.id=?1) , (SELECT a.job_status FROM application a WHERE a.id=?1)) , (SELECT s.job_status FROM service_job s WHERE s.id=?1) ), (SELECT m.job_status FROM mirror_job m WHERE m.id=?1) ) AS job_status",
           nativeQuery = true)
    String getJobStatusById(Long id);

    List<ResourceUsage> findByJobId(Long jobId);

    @Query(value = "SELECT DISTINCT job_id FROM resource_usage ru WHERE released = FALSE AND resource_type != 'DISK'",
           nativeQuery = true)
    List<Long> getJobIdListOfUnreleasedResources();

    ResourceUsage findByResourceTypeAndUserUsername(ResourceType resourceType, String username);
}
