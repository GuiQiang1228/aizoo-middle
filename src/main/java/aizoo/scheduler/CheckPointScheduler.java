package aizoo.scheduler;

import aizoo.common.JobStatus;
import aizoo.domain.CheckPoint;
import aizoo.domain.ExperimentJob;
import aizoo.domain.Namespace;
import aizoo.domain.User;
import aizoo.repository.*;
import aizoo.utils.CheckPointUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.persistence.EntityNotFoundException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class CheckPointScheduler {

    @Autowired
    ExperimentJobDAO experimentJobDAO;

    @Autowired
    ComponentDAO componentDAO;

    @Autowired
    CheckPointUtil checkPointUtil;

    @Autowired
    UserDAO userDAO;

    @Autowired
    NamespaceDAO namespaceDAO;

    @Autowired
    CheckPointDAO checkPointDAO;

    @Value("${file.path}")
    String file_path;

    private static final Logger logger = LoggerFactory.getLogger(CheckPointScheduler.class);

    private final static List<String> jobs = new CopyOnWriteArrayList<>();

    private volatile boolean allUnFinishedJobKeyHasAdd = false;

    /**
     * 定时查询是否有已完成但未保存的checkPoint
     * 将checkpoint保存至用户目录下。并保存到数据库中
     * 定时查询，每隔3秒钟执行一次该方法
     */
    @Transactional
    @Scheduled(cron = "*/3 * * * * *")
    public void checkPointCheck() throws IOException {
        // 定时查询是否有已完成但未保存的checkPoint
        logger.info("Start check checkPoint");
        if (jobs != null && allUnFinishedJobKeyHasAdd) {
            // 1、将所有需要检查的job组成job2RemoveList，进行遍历检查
            List<String> job2Remove = new ArrayList<>();
            for (String key : jobs) {
                try {
                    logger.info("开始: jobKey: {}", key);
                    ExperimentJob experimentJob = experimentJobDAO.findByJobKey(key);
                    User user = userDAO.findById(experimentJob.getUser().getId()).orElseThrow(() -> new EntityNotFoundException(String.valueOf(experimentJob.getUser().getId())));
                    // 判断checkpoint的路径是否存在
                    String checkPointPath = Paths.get(file_path, experimentJob.getUser().getUsername(), "checkpoint").toString();
                    File checkPointPathDir = new File(checkPointPath);
                    if (!checkPointPathDir.exists()) {
                        checkPointPathDir.mkdirs();
                    }
                    //  2、生成job对应的CheckPoint
                    Path path = Paths.get(checkPointPath, experimentJob.getId() + "");
                    if (experimentJob.getJobStatus() == JobStatus.COMPLETED
                            || experimentJob.getJobStatus() == JobStatus.CANCELLED
                            || experimentJob.getJobStatus() == JobStatus.FAILED
                            || experimentJob.getJobStatus() == JobStatus.DEADLINE
                            || experimentJob.getJobStatus() == JobStatus.OUT_OF_MEMORY
                            || experimentJob.getJobStatus() == JobStatus.SPECIAL_EXIT
                            || experimentJob.getJobStatus() == JobStatus.TIMEOUT
                    ) {
                        //  3、checkpoint路径复制，写入数据库
                        Map<String, String> checkPoints = checkPointUtil.copyCheckPointToDir(experimentJob, path.toString());
                        logger.info("map: {}", checkPoints);
                        logger.info("已经复制完当前job的所有checkPoint，开始写入数据库");
                        for (String checkPont : checkPoints.keySet()) {
                            // namespace已经在job执行时建好了，如果执行过程中出现别的情况，导致namespace新建失败，则需要在这里新建
                            Namespace namespace = namespaceDAO.findByNamespace(path.toString().replace(file_path + "/", "").replace("/", "."));
                            if (namespace == null) {
                                namespace = new Namespace(path.toString().replace(file_path + "/", "").replace("/", "."));
                                namespace.setPrivacy("private");
                                namespace.setUser(user);
                                namespaceDAO.save(namespace);
                                user.getNamespaces().add(namespace);
                                userDAO.save(user);
                            }
                            // 4、如果checkpoint在数据库里存在，只复制文件，不新建记录
                            boolean skip = false;
                            for (CheckPoint checkPointEntity : checkPointDAO.findByName(checkPont)) {
                                ExperimentJob experimentJobInEntity = checkPointEntity.getExperimentJob();
                                Namespace namespaceInEntity = checkPointEntity.getNamespace();
                                if ((experimentJobInEntity != null && experimentJobInEntity.getId().equals(experimentJob.getId()))
                                        && (namespaceInEntity != null && namespaceInEntity.getNamespace().equals(namespace.getNamespace()))) {
                                    skip = true;
                                    break;
                                }

                            }
                            if (skip) {
                                logger.info("checkpoint已在数据库中存在记录，跳过数据库插入操作。checkpointName={},experimentJobId={},namespace={}", checkPont, experimentJob.getId(), namespace.getNamespace());
                                continue;
                            }
                            // checkpoint写入数据库
                            CheckPoint checkPoint2 = new CheckPoint();
                            checkPoint2.setExperimentJob(experimentJob);
                            checkPoint2.setPath(checkPoints.get(checkPont));
                            checkPoint2.setNamespace(namespaceDAO.findByNamespace(path.toString().replace(file_path + "/", "").replace("/", ".")));
                            checkPoint2.setName(checkPont);
                            checkPoint2.setTitle(checkPont);
                            checkPoint2.setUser(user);
                            checkPoint2.setPrivacy("private");
                            logger.info("当前checkPoint" + checkPoint2 + "写入数据库");
                            checkPointDAO.save(checkPoint2);
                            logger.info("当前checkPoint写入数据库成功");
                        }
                        logger.info("写入数据库完毕");
                        job2Remove.add(key);
                        logger.info("Remove the key: {}", key);
                        logger.info("jobs: {}", jobs);
                    }
                } catch (Exception exception) {
                    logger.error("checkpoint状态更新失败，jobKey={}，错误：{}", key, exception);//都只删正确执行完的jobkey，错误的会一直留在列表里
                    continue;
                }
            }
            jobs.removeAll(job2Remove);
        }
        logger.info("End check checkPoint");
    }

    /**
     * 宕机重启时
     * 遍历全表 complete的job
     * 查询是否checkpoints复制完成，未完成加job
     * 可能会出现部分文件已存在但是并不完整，暂不考虑这种情况
     * 注意： PostConstruct执行时，数据库还未链接，不要在这时操作数据库！
     */
    @PostConstruct
    public void init() throws JsonProcessingException {
        logger.info("Start Init");
        // 1、获取ExperimentJob列表
        Set<ExperimentJob> experimentJobList = experimentJobDAO.findByJobStatus(JobStatus.COMPLETED);
        for (ExperimentJob experimentJob : experimentJobList) {
            // 2、查询ExperimentJob列表是否checkpoints复制完成，未完成加job
            Path path = Paths.get(file_path, experimentJob.getUser().getUsername(), "checkpoint", experimentJob.getId() + "");
            if (!CheckPointUtil.allCheckPointHasSave(experimentJob, path.toString())) {
                jobs.add(experimentJob.getJobKey());
            }
        }
        allUnFinishedJobKeyHasAdd = true;
        logger.info("jobs: {}", jobs);
        logger.info("End Init");
    }

    // 加入到等待更新状态、保存结果的jobs队列中
    public static void addJob(String jobKey) {
        logger.info("Start add Job");
        jobs.add(jobKey);
        logger.info("jobKey: {}", jobKey);
        logger.info("End add Job");
    }

    // 如果退出，启动删除job
    public static void removeJobIfExited(String jobkey) {
        logger.info("Start remove Job If Exited");
        if (jobs.contains(jobkey))
            jobs.remove(jobkey);
        logger.info("jobkey to remove: {}", jobkey);
        logger.info("End remove Job If Exited");
    }
}
