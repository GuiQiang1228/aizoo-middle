package aizoo.scheduler;

import aizoo.common.JobStatus;
import aizoo.domain.ExperimentJob;
import aizoo.repository.ExperimentJobDAO;
import aizoo.service.ExperimentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ExperimentJobStatusCheckScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ExperimentJobStatusCheckScheduler.class);

    @Autowired
    private ExperimentJobDAO experimentJobDAO;

    @Autowired
    private ExperimentService experimentService;

    /**
     * 更新需要更新的job列表
     * @throws JsonProcessingException
     */
    @Scheduled(cron = "*/3 * * * * *")
    public void updateJobStatusScheduledTask() throws JsonProcessingException {
        logger.info("Start update JobStatusScheduledTask");
        //获取需要更新的job列表
        List<JobStatus> jobStatuses = JobStatus.needUpdate();
        List<ExperimentJob> experimentJobs = experimentJobDAO.findByJobStatusIsIn(jobStatuses);
        //更新非终止的job状态
        if (experimentJobs != null) {
            logger.info("更新job状态");
            for (ExperimentJob experimentJob : experimentJobs) {
                String jobKey = experimentJob.getJobKey();
                logger.info("当前更新jobKey: {}", jobKey);
                try {
                    //进行更新
                    experimentService.updateExperimentJobStatusAndEnv(jobKey);
                } catch (Exception exception) {
                    //更新失败
                    logger.error("JobStatusScheduledTask job状态更新失败，jobKey={}，错误：{}", jobKey, exception);
                    continue;
                }

            }
        }
        logger.info("End update JobStatusScheduledTask");
    }
}
