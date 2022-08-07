package aizoo.scheduler;

import aizoo.common.JobStatus;
import aizoo.domain.ServiceJob;
import aizoo.repository.ServiceJobDAO;
import aizoo.service.ServiceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ServiceJobStatusCheckScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ServiceJobStatusCheckScheduler.class);

    @Autowired
    private ServiceJobDAO serviceJobDAO;

    @Autowired
    private ServiceService serviceService;
    /**
     * 定时更新RUNNING,PENDING的serviceJob状态
     * 获取所有状态的Job
     * 在Job中选择所有RUNNING和PENDING的Job
     * 遍历这些Job并根据jobKey进行更新
     * @return
     * @throws Exception
     */
    @Scheduled(cron = "*/3 * * * * *")
    public void updateServiceJobStatusScheduledTask() throws Exception {
        logger.info("Start update Service Job Status Scheduled Task");
        // 1.获取所有状态的Job
        List<JobStatus> jobStatuses = JobStatus.needUpdate();
        // 2.根据jobKey在Job中选择所有RUNNING和PENDING的Job
        List<ServiceJob> serviceJobs = serviceJobDAO.findByJobStatusIsIn(jobStatuses);
        if (serviceJobs != null) {
            logger.info("更新serviceJob状态");
            // 3.遍历所有获取的serviceJob
            for (ServiceJob serviceJob : serviceJobs) {
                // 获取其jobKey
                String jobKey = serviceJob.getJobKey();
                try {
                    logger.info("当前更新jobKey:{}", jobKey);
                    // 更新该Job的状态和环境信息
                    serviceService.updateServiceJobStatusAndEnv(jobKey);
                } catch (Exception e) {
                    //只删除正确执行完的jobKey，错误的会一直留在列表里
                    logger.error("updateServiceJobStatusScheduledTask job状态更新失败，jobkey={}，错误： {}", jobKey, e);
                    continue;
                }
            }
        }
        logger.info("End update Service Job Status Scheduled Task");
    }
}
