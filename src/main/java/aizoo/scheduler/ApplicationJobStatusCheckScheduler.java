package aizoo.scheduler;

import aizoo.common.JobStatus;
import aizoo.domain.Application;
import aizoo.repository.ApplicationDAO;
import aizoo.service.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ApplicationJobStatusCheckScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationJobStatusCheckScheduler.class);

    @Autowired
    ApplicationDAO applicationDAO;

    @Autowired
    ApplicationService applicationService;

    /**
     * 对于数据库中status为jobStatuses(比如 Pending、Running等)中任意状态的 application任务
     * 定时更新其状态，每隔3秒钟执行一次该方法
     */
    @Scheduled(cron = "*/3 * * * * *")
    public void updateApplicationStatusScheduledTask() {
        logger.info("Start update ApplicationStatusScheduledTask");
        // 获取所有需要更新的任务状态
        List<JobStatus> jobStatuses = JobStatus.needUpdate();
        // 根据状态，过滤出需要更新的application列表
        List<Application> applications = applicationDAO.findByJobStatusIsIn(jobStatuses);
        if (applications != null) {
            logger.info("更新application状态");
            for (Application app : applications) {
                String jobKey = app.getJobKey();
                try {
                    logger.info("当前更新jobKey: {}", jobKey);
                    applicationService.updateApplicationJobStatusAndEnv(jobKey);
                } catch (Exception exception) {
                    logger.error("更新application状态失败，jobKey={}，错误： {}", jobKey, exception);
                    continue;
                }
            }
        }
        logger.info("End update ApplicationStatusScheduledTask");
    }

}
