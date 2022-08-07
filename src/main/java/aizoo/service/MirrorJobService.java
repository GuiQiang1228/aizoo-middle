package aizoo.service;

import aizoo.common.*;
import aizoo.common.exception.NoSlurmAccountException;
import aizoo.domain.*;
import aizoo.repository.*;
import aizoo.response.BaseException;
import aizoo.response.ResponseCode;
import aizoo.scheduler.ResourceUsageScheduler;
import aizoo.utils.*;
import aizoo.viewObject.mapper.MirrorJobVOEntityMapper;
import aizoo.viewObject.object.MirrorJobVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.io.File;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.*;

@Service("MirrorJobService")
public class MirrorJobService {

    @Autowired
    SlurmService slurmService;

    @Autowired
    MirrorJobDAO mirrorJobDAO;

    @Value("${file.path}")
    private String filePath;

    @Value("${download.dir}")
    String downloadDir;

    @Autowired
    UserDAO userDAO;

    @Autowired
    CodeDAO codeDAO;

    @Autowired
    MirrorDAO mirrorDAO;

    @Autowired
    TranslationService translationService;

    @Autowired
    ResourceUsageService resourceUsageService;

    @Autowired
    ResourceUsageDAO resourceUsageDAO;

    @Autowired
    JobService jobService;

    private static final Logger logger = LoggerFactory.getLogger(MirrorJobService.class);

    /**
     * 通过jobKey一次性更新job的status以及environment
     *
     * @param jobKey 用于标识当前的job
     * @return 返回更新后的MirrorJob信息
     * @throws Exception
     */
    @Transactional
    public MirrorJob updateMirrorJobStatusAndEnv(String jobKey) throws Exception {
        logger.info("Start Update Mirror Job Status and Env");
        logger.info("The updating jobKey: {}", jobKey);

        // 1.根据jobKey从数据库中获取MirrorJob完整信息
        MirrorJob mirrorJob = mirrorJobDAO.findByJobKey(jobKey);
        // 2.获取对应用户的slurm账户
        User user = mirrorJob.getUser();
        SlurmAccount slurmAccount = user.getSlurmAccount();
        // 若该用户不存在slurm账户, 则抛出异常
        if (slurmAccount == null)
            throw new NoSlurmAccountException();
        // 获取更新后的status和environment
        String jobInfo = slurmService.showJob(jobKey, slurmAccount);
        Map<String, String> resultMap = JobUtil.updateJobStatusAndEnv(jobKey, jobInfo, mirrorJob.getEnvironment());
        // 修改实体对应的属性的值，更新数据库中的信息
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> jobKey2Environment = objectMapper.readValue(resultMap.get("environment"), new TypeReference<Map<String, Object>>() {
        });
        Map<String, Object> environment = (Map<String, Object>) jobKey2Environment.get(jobKey);
        if (environment.get("command") != null)
            mirrorJob.setScriptPath(environment.get("command").toString());
        mirrorJob.setEnvironment(resultMap.get("environment"));
        mirrorJob.setJobStatus(JobStatus.valueOf(resultMap.get("jobStatus")));
        mirrorJobDAO.save(mirrorJob);
        logger.info("End update Mirror Job Status And Env");
        return mirrorJob;
    }

    /**
     * 该方法用于执行实验
     *
     * @param relativePath 前端传的该mirror所使用的入口文件的相对地址
     * @param mirrorJobVO  job相关信息
     * @param username     用户名
     * @param mirrorId     镜像id
     * @throws Exception
     */
    @Transactional(rollbackFor = Exception.class, noRollbackFor = NoSlurmAccountException.class)
    public void executeMirrorJob(String relativePath, long codeId, MirrorJobVO mirrorJobVO, List<List<String>> userArgs, String username, long mirrorId) throws Exception {
        logger.info("Start execute MirrorJob");
        User user = userDAO.findByUsername(username);
        Mirror mirror = mirrorDAO.findById(mirrorId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(mirrorId)));

        logger.info("findByUsername username: {}", username);
        SlurmAccount slurmAccount = user.getSlurmAccount();
        if (slurmAccount == null)
            throw new NoSlurmAccountException();

        // 1.先将mirrorJobVO转化为mirrorJob
        MirrorJob mirrorJob = MirrorJobVOEntityMapper.MAPPER.jobVO2Job(mirrorJobVO, mirrorJobDAO);
        logger.info("jobVO2Job mirrorJobVO: {}", mirrorJobVO.toString());

        // 2.找到所使用的code
        String codePath = Paths.get(filePath, username, "code", String.valueOf(codeId), relativePath).toString();
        Code code = codeDAO.findById(codeId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(codeId)));

        // 添加关联
        mirrorJob.setUser(user);
        mirrorJob.setMirror(mirror);
        mirrorJob.setCode(code);
        mirrorJobDAO.save(mirrorJob);

        // 3.找到所使用的mirrorPath
        String mirrorPath = mirror.getPath();
        if (!new File(mirrorPath).exists())
            new File(mirrorPath).mkdirs();

        // 4.找到挂载路径mountPath  ../files/username/mirrorJob/mirrorJobId（即根目录）
        String rootPath = Paths.get(filePath, username, "mirrorjob", String.valueOf(mirrorJob.getId())).toString();
        mirrorJob.setRootPath(rootPath);

        // 5.将实验的代码复制到job目录下

        if (!new File(rootPath).exists())
            new File(rootPath).mkdirs();
        FileUtil.copyFileOrDir(code.getPath(), rootPath);

        String logPath = Paths.get(rootPath, "runtime_log").toString();
        if (!new File(logPath).exists())
            new File(logPath).mkdirs();

        //执行入口路径，是复制code到job目录之后的路径，所以需要把用户选的code路径前缀替换成mirror job的前缀
        String executePath = codePath.replace(Paths.get(filePath, username, "code", String.valueOf(codeId)).toString(), rootPath);
        mirrorJob.setExecutePath(executePath);
        String[] paths = {executePath, logPath, mirrorPath, rootPath};

        // 6.组织用户自己定义的参数，准备传给slurm并保存
        Map<String, String> mapArgs = new HashMap<>();
        if (userArgs.size() > 0) {
            for (List<String> line : userArgs) {
                mapArgs.put(line.get(0), line.get(1));
            }
        }
        if (mirrorJobVO.getPort() != null) {
            if (mirrorJobVO.getPort().equals("auto"))
                mapArgs.put("port", null);
            else
                mapArgs.put("port", mirrorJobVO.getPort());
        }
        ObjectMapper objectMapper = new ObjectMapper();

        // 7.提交实验任务到slurm，执行实验，并保存参数
        String command = mirrorJobVO.getCommand();
        String args = SlurmUtil.getSlurmArgs(mirrorJob.getName(), AizooConstans.SLURM_PYTHON_VERSION, paths, mirrorJobVO.getEnvironment().get("slurmKwargs"), rootPath, command);
        mirrorJob.setArgs(args);
        if (userArgs.size() > 0)
            mirrorJob.setUserArgs(objectMapper.writeValueAsString(mapArgs));
        else
            mirrorJob.setUserArgs(null);

        String jobKey = slurmService.startMirrorJob(args, objectMapper.writeValueAsString(mapArgs), slurmAccount);

        // 8.保存资源使用情况
        Map<String, Object> slurmArgs = mirrorJobVO.getEnvironment().get(AizooConstans.SLURM_RUN_ARGUMENTS_NAME);
        resourceUsageService.saveResourceUsageOfJob(slurmArgs, mirrorJob.getId(), user);
        ResourceUsage ru = new ResourceUsage(ResourceType.MIRROR_JOB_RUNNING_NUMBER, 1, mirrorJob.getId(), user);
        resourceUsageDAO.save(ru);

        // 9.保存jobKey、job的状态和该实验运行环境
        mirrorJob.setJobKey(jobKey);
        mirrorJob.setJobStatus(JobStatus.valueOf(jobService.getJobStatus(jobKey, JobType.MIRROR_JOB, user)));
        String environment = slurmService.showJob(jobKey, slurmAccount);
        //将前端传来的args存入environment
        Map<String, Object> environmentArgs = mirrorJobVO.getEnvironment().get("args");
        Map<String, Map<String, Object>> environmentMap = objectMapper.readValue(environment, new TypeReference<Map<String, Map<String, Object>>>() {
        });
        environmentMap.put("args",environmentArgs);
        environment = objectMapper.writeValueAsString(environmentMap);

        //如果slurm返回的运行环境信息不为空则对实验运行环境进行更新
        if (!(environment == null || environment.isEmpty() || environment.equals("null")))
            mirrorJob.setEnvironment(environment);
        mirrorJobDAO.save(mirrorJob);

        // 10.加入到等待更新的资源使用情况队列中
        ResourceUsageScheduler.addJob(mirrorJob.getId());
        logger.info("End execute MirrorJob");
    }

    /**
     * 此方法用于根据id终止对应的MirrorJob
     *
     * @param jobId 需要终止的MirrorJob的id
     * @return 布尔类型变量，表示是否终止成功
     * @throws Exception
     */
    @Transactional
    public boolean slurmStopMirrorJob(long jobId) throws Exception {
        logger.info("Start stop Slurm MirrorJob");
        MirrorJob mirrorJob = mirrorJobDAO.findById(jobId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(jobId)));
        logger.info("findById jobId: {}", jobId);
        //1.从数据库中获取当前Job的状态
        String databaseJobStatus = mirrorJob.getJobStatus().toString();
        String jobKey = mirrorJob.getJobKey();
        //2.从slurm中获取当前job的状态
        String slurmJobStatus = jobService.getJobStatus(jobKey, JobType.MIRROR_JOB, mirrorJob.getUser());
        User user = mirrorJob.getUser();
        SlurmAccount slurmAccount = user.getSlurmAccount();
        //3.如果与数据库中的不一致则返回false，并更新数据库
        if (!databaseJobStatus.equals(slurmJobStatus)) {
            updateMirrorJobStatusAndEnv(jobKey);
            logger.info("Fail to stop Slurm MirrorJob");
            logger.info("End stop Slurm MirrorJob");
            return false;
        } else {
            //一致则终止任务并查询slurm更新数据库并返回true
            int flag = Integer.parseInt(slurmService.stop(jobKey, slurmAccount));
            if (flag == 0) {
                updateMirrorJobStatusAndEnv(jobKey);
                logger.info("Success to stop Slurm MirrorJob");
                logger.info("End stop Slurm MirrorJob");
                return true;
            } else {
                throw new BaseException(ResponseCode.CANCEL_JOB_ERROR);
            }
        }
    }

    /**
     * 该方法用于根据id删除对应MirrorJob
     *
     * @param id 需要删除的MirrorJob的id
     * @throws Exception
     */
    @Transactional
    public void removeMirrorJobById(long id) throws Exception {
        logger.info("Start remove MirrorJobById");
        MirrorJob mirrorJob = mirrorJobDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
        logger.info("findById id: {}", id);
        String username = mirrorJob.getUser().getUsername();

        //删除对应文件夹
        String path = Paths.get(filePath, username, "mirrorjob", String.valueOf(mirrorJob.getId())).toString();
        FileUtil.deleteFile(new File(path));

        //数据库删除mirrorJob
        mirrorJobDAO.delete(mirrorJob);
        resourceUsageService.updateDiskCapacity(username);
        logger.info("End remove MirrorJobById");
    }
}
