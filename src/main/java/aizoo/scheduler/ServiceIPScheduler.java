package aizoo.scheduler;

import aizoo.Client;
import aizoo.common.JobStatus;
import aizoo.common.exception.NoSlurmAccountException;
import aizoo.domain.ServiceJob;
import aizoo.domain.SlurmAccount;
import aizoo.domain.User;
import aizoo.repository.*;
import io.grpc.netty.shaded.io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

@Service
public class ServiceIPScheduler {
    @Autowired
    ServiceJobDAO serviceJobDAO;

    @Autowired
    Client client;

    private final static Logger logger = LoggerFactory.getLogger(ServiceIPScheduler.class);

    /**
     * 定时查询、更新serviceJob的ip和端口号，
     * 并保存到数据库中
     */

    //Schedule默认是单线程运行定时任务的，即使是多个不同的定时任务，默认也是单线程运行
    //所以不需要synchronized保证顺序执行
    @Transactional(rollbackFor = Exception.class, noRollbackFor = NoSlurmAccountException.class)
    @Scheduled(cron = "*/2 * * * * *")
    public void ipCheck() throws Exception {
        //System.out.println("waitForIpJobs : " + waitForIpJobs.toString());
        logger.info("Start ipCheck");
        //获取正在运行的任务实体
        Set<ServiceJob> serviceJobs = serviceJobDAO.findJobsForIpPortCheck(JobStatus.RUNNING);
        for (ServiceJob serviceJob : serviceJobs) {
            //获取正在运行任务的用户和账号
            String serviceJobKey = serviceJob.getJobKey();
            try {
                User user = serviceJob.getUser();
                SlurmAccount slurmAccount = null;
                if(user!=null)
                    slurmAccount = user.getSlurmAccount();
                if (slurmAccount == null)
                    throw new NoSlurmAccountException();
                Map<String, String> ipAndPort = client.getServiceIpAndPort(serviceJob.getJobKey(), slurmAccount);
                //查询serviceJob的ip
                String ip = ipAndPort.get("ip");
                //serviceJob的端口号
                String port = ipAndPort.get("port");
                //更新serviceJob的ip和端口号
                if (!StringUtil.isNullOrEmpty(ip) && !StringUtil.isNullOrEmpty(port)) {
                    serviceJob.setIp(ip);
                    serviceJob.setPort(port);
                    serviceJobDAO.save(serviceJob);
                    logger.info("ip写入数据库完毕");
                    logger.info("Remove {}", serviceJobKey);
                }
            } catch (Exception exception) {
                //都只删正确执行完的jobkey，错误的会一直留在列表里
                logger.error("ipCheck 更新失败，jobid={}", serviceJobKey);
                logger.error(exception.getMessage(), exception);
                continue;
            }

        }
        logger.info("End ipCheck");
    }


    // 加入到等待更新状态、保存结果的jobs队列中
    public static void addJob(String jobKey) {
    }

    public static void removeJobIfExited(String jobkey) {
    }
}
