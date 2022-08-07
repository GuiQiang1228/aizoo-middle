package aizoo.scheduler;

import aizoo.common.JobStatus;
import aizoo.domain.ResourceUsage;
import aizoo.repository.ResourceUsageDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *定时检查需要释放的资源、定时更新资源使用情况
 */
@Service
public class ResourceUsageScheduler {

    @Autowired
    ResourceUsageDAO resourceUsageDAO;
    private final static Logger logger = LoggerFactory.getLogger(ResourceUsageScheduler.class);
    
    private final static List<Long> jobs = new CopyOnWriteArrayList<>();

    private final static List<JobStatus> JOB_FINISHED_STATUSES = Arrays.asList(JobStatus.CANCELLED, JobStatus.COMPLETED, JobStatus.FAILED, JobStatus.DEADLINE,
            JobStatus.NODE_FAIL, JobStatus.OUT_OF_MEMORY, JobStatus.TIMEOUT);

    private volatile boolean allUnCheckedJobIdHasAdd = false;

    @Transactional
    @Scheduled(cron = "*/1 * * * * *")
    /**
     * 释放资源检查
     * 定时检查，每隔1秒钟执行一次该方法
     */
    public void releaseResourceCheck() {
        logger.info("Start release Resource Check");
        if (jobs != null && allUnCheckedJobIdHasAdd) {
            // 1、将所有需要释放资源的job组成job2RemoveList
            List<Long> job2Remove = new ArrayList<>();
            // 2、遍历检查job2RemoveList
            for (Long id : jobs) {
                try {
                    String jobStatus = resourceUsageDAO.getJobStatusById(id);
                    // 查看job的状态判断是否任务结束，是否符合释放资源的条件
                    if (jobStatus == null)
                        continue;
                    if (JOB_FINISHED_STATUSES.contains(JobStatus.valueOf(jobStatus))) {
                        // 3、任务结束，符合释放资源的条件,释放该job申请的所有资源
                        List<ResourceUsage> resourceUsages = resourceUsageDAO.findByJobId(id);
                        for (ResourceUsage ru : resourceUsages) {
                            ru.setReleased(true);
                            resourceUsageDAO.save(ru);
                        }
                        job2Remove.add(id);
                    }

                } catch (Exception exception) {
                    logger.error("releaseResourceCheck job占用资源更新失败，jobid={}，错误：{}", id, exception);//都只删正确执行完的jobkey，错误的会一直留在列表里
                    continue;
                }

            }
            jobs.removeAll(job2Remove);
        }
        logger.info("End release Resource Check");
    }

    /**
     * 宕机重启后，遍历ResourceUsage表中还没释放的资源，得到它们的jobId列表
     */
    @PostConstruct
    public void CheckInit() {
        logger.info("Start Check Init");
        // 将ResourceUsage表中还没释放的资源组成jobIdList
        List<Long> jobIdList = resourceUsageDAO.getJobIdListOfUnreleasedResources();
        jobs.addAll(jobIdList);
        allUnCheckedJobIdHasAdd = true;
        logger.info("End Check Init");
    }

    public static void addJob(Long jobId) {
        logger.info("Start add Job");
        logger.info("addJob jobId:{}", jobId);
        jobs.add(jobId);
        logger.info("End add Job");
    }
}
