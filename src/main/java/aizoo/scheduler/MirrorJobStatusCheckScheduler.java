package aizoo.scheduler;

import aizoo.common.JobStatus;
import aizoo.domain.MirrorJob;
import aizoo.repository.MirrorJobDAO;
import aizoo.service.MirrorJobService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MirrorJobStatusCheckScheduler {

    private static final Logger logger = LoggerFactory.getLogger(MirrorJobStatusCheckScheduler.class);

    @Autowired
    private MirrorJobDAO mirrorJobDAO;

    @Autowired
    private MirrorJobService mirrorJobService;

    /**
     * 定时更新(3s)mirrorJob的状态
     * 获取所有状态的Job
     * 在Job中选择所有RUNNING和PENDING的Job
     * 遍历这些Job并根据jobKey进行更新
     * @return
     * @throws Exception
     */
    @Scheduled(cron = "*/3 * * * * *")
    public void updateMirrorJobStatusScheduledTask() throws JsonProcessingException {
        logger.info("Start update Mirror Job Status Scheduled Task");
        // 1.获取所有job的状态列表
        List<JobStatus> jobStatusList = JobStatus.needUpdate();
        // 2.在数据库中查找非终止状态的mirrorJob
        List<MirrorJob> mirrorJobs = mirrorJobDAO.findByJobStatusIsIn(jobStatusList);
        // 3.更新非终止的job状态
        if (mirrorJobs != null) {
            logger.info("begin updating mirror job status");
            // 3.遍历这些job并根据jobKey进行更新
            for (MirrorJob mirrorJob : mirrorJobs) {
                String jobKey = mirrorJob.getJobKey();
                logger.info("The currently updating jobKey: {}", jobKey);
                try {
                    // 调用service层的方法进行更新
                    mirrorJobService.updateMirrorJobStatusAndEnv(jobKey);
                } catch (Exception e) {
                    //更新失败
                    logger.error("Mirror job updating failed! jobKey = {}，Error: {}", jobKey, e);
                    continue;
                }
            }
        }
        logger.info("End update Mirror Job Status Scheduled Task");
    }
}
