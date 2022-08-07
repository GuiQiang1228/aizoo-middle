package aizoo.scheduler;

import aizoo.Client;
import aizoo.common.JobStatus;
import aizoo.common.exception.NoSlurmAccountException;
import aizoo.domain.MirrorJob;
import aizoo.domain.SlurmAccount;
import aizoo.domain.User;
import aizoo.repository.MirrorJobDAO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.netty.shaded.io.netty.util.internal.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

@Component
public class MirrorJobIPScheduler {

    @Autowired
    MirrorJobDAO mirrorJobDAO;

    @Autowired
    Client client;

    private final static Logger logger = LoggerFactory.getLogger(MirrorJobIPScheduler.class);

    /**
     * 定时查询、更新mirrorJob的ip和端口号，
     * 并保存到数据库中
     */
    @Transactional(rollbackFor = Exception.class, noRollbackFor = NoSlurmAccountException.class)
    @Scheduled(cron = "*/2 * * * * *")
    public void ipCheck() throws Exception {
        //System.out.println("waitForIpJobs : " + waitForIpJobs.toString());
        logger.info("Start Mirror Job IP Check");
        // 获取需要进行进行更新的mirrorJob：从数据库中找出处于运行状态且ip和port有一项为空的mirrorJob
        Set<MirrorJob> mirrorJobs = mirrorJobDAO.findJobsForIpPortCheck(JobStatus.RUNNING);
        for (MirrorJob mirrorJob : mirrorJobs) {
            //获取正在运行任务的用户和账号
            String mirrorJobKey = mirrorJob.getJobKey();
            try {
                User user = mirrorJob.getUser();
                SlurmAccount slurmAccount = null;
                if(user!=null)
                    slurmAccount = user.getSlurmAccount();
                if (slurmAccount == null)
                    throw new NoSlurmAccountException();
                String args = mirrorJob.getUserArgs();
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> argsMap = null;
                if (StringUtils.isNotEmpty(args)) {
                    argsMap = objectMapper.readValue(args, new TypeReference<Map<String, Object>>() {});
                }
                if(argsMap==null)
                    continue;
                if (argsMap.containsKey("port")) { //若填了port参数，查询和更新ip和port
                        String jobKey=mirrorJob.getJobKey();
                        Map<String, String> ipAndPort = client.getServiceIpAndPort(jobKey, slurmAccount);
                        //查询serviceJob的ip
                        String ip = ipAndPort.get("ip");
                        //serviceJob的端口号
                        String port = ipAndPort.get("port");
                        //更新serviceJob的ip和端口号
                        if (!StringUtil.isNullOrEmpty(ip) && !StringUtil.isNullOrEmpty(port)) {
                            mirrorJob.setIp(ip);
                            mirrorJob.setPort(port);
                            mirrorJobDAO.save(mirrorJob);
                            logger.info("ip和host写入数据库完毕");
                            logger.info("Remove {}", mirrorJobKey);
                        }
                    };
                } catch (Exception exception) {
                    //都只删正确执行完的jobkey，错误的会一直留在列表里
                    logger.error("ipCheck 更新失败，jobid={}", mirrorJobKey);
                    logger.error(exception.getMessage(), exception);
                    continue;
                }

        }
        logger.info("End Mirror Job IP Check");
    }



}
