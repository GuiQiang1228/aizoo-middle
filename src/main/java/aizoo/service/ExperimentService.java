package aizoo.service;

import aizoo.Client;
import aizoo.common.*;
import aizoo.common.exception.NoSlurmAccountException;
import aizoo.domain.*;
import aizoo.elasticObject.ElasticExperimentJob;
import aizoo.elasticRepository.ExperimentJobRepository;
import aizoo.repository.*;
import aizoo.response.BaseException;
import aizoo.response.ResponseCode;
import aizoo.scheduler.CheckPointScheduler;
import aizoo.scheduler.ResourceUsageScheduler;
import aizoo.utils.*;
import aizoo.viewObject.mapper.ExperimentJobVOEntityMapper;
import aizoo.viewObject.mapper.GraphVOEntityMapper;
import aizoo.viewObject.object.ExperimentJobVO;
import aizoo.viewObject.object.GraphVO;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service("ExperimentService")

public class ExperimentService {

    @Autowired
    SlurmService slurmService;

    @Autowired
    TranslationService translationService;

    @Autowired
    Client client;

    @Autowired
    JobService jobService;

    @Autowired
    UserDAO userDAO;

    @Autowired
    CheckPointDAO checkPointDAO;

    @Autowired
    ExperimentJobDAO experimentJobDAO;

    @Autowired
    GraphDAO graphDAO;

    @Autowired
    ComponentDAO componentDAO;

    @Autowired
    DatatypeDAO datatypeDAO;

    @Autowired
    NamespaceDAO namespaceDAO;

    @Autowired
    ResourceUsageDAO resourceUsageDAO;

    @Autowired
    ResourceUsageService resourceUsageService;

    @Autowired
    DAOUtil daoUtil;

    @Autowired
    ObjectMapper objectMapper;

    @Value("${file.path}")
    private String filePath;

    @Autowired
    ProjectService projectService;

    @Autowired
    ExperimentJobRepository experimentJobRepository;

    @Autowired
    ServiceDAO serviceDAO;

    @Autowired
    DatasourceDAO datasourceDAO;

    private static final Logger logger = LoggerFactory.getLogger(ExperimentService.class);

    /**
     * 该方法用于更新ExperimentJob的状态
     * @param curStatus 实验当前状态
     * @param experimentJob 需要更新的ExperimentJob
     * @return 返回更新后的ExperimentJob信息
     */
    @Transactional
    public ExperimentJob updateStatus(String curStatus, ExperimentJob experimentJob) {
        logger.info("Start update Status");
        logger.info("curStatus: {}", curStatus);
        logger.info("experimentJob: {}", experimentJob.toString());
        //获取当前状态，设置对象的状态，并对数据库中的该ExperimentJob进行更新
        JobStatus currentStatus = JobStatus.valueOf(curStatus);
        experimentJob.setJobStatus(currentStatus);
        experimentJobDAO.save(experimentJob);
        logger.info("Updating experimentJob status: {}", experimentJob);
        logger.info("End update Status");
        return experimentJob;
    }

    /**
     * 一次更新job的status以及environment
     * @param jobKey 用于标识当前的job
     * @return 返回更新后的ExperimentJob信息
     * @throws Exception
     */
    @Transactional
    public ExperimentJob updateExperimentJobStatusAndEnv(String jobKey) throws Exception {
        logger.info("Start update ExperimentJobStatusAndEnv");
        logger.info("jobKey: {}", jobKey);
        //根据jobkey从数据库中获取ExperimentJob完整信息
        ExperimentJob experimentJob = experimentJobDAO.findByJobKey(jobKey);
        //获取对应用户的slurm账户
        User user = experimentJob.getUser();
        SlurmAccount slurmAccount = user.getSlurmAccount();
        if (slurmAccount == null)
            throw new NoSlurmAccountException();
        //获取更新后的status和environment的值
        String jobInfo = slurmService.showJob(jobKey, slurmAccount);
        Map<String, String> resultMap = JobUtil.updateJobStatusAndEnv(jobKey, jobInfo, experimentJob.getEnvironment());
        //修改实体对应的属性的值，更新数据库中的信息
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> jobKey2Environment = objectMapper.readValue(resultMap.get("environment"), new TypeReference<Map<String, Object>>() {
        });
        Map<String, Object> environment = (Map<String, Object>) jobKey2Environment.get(jobKey);
        if(environment.get("command") != null)
            experimentJob.setScriptPath(environment.get("command").toString());
        experimentJob.setEnvironment(resultMap.get("environment"));
        experimentJob.setJobStatus(JobStatus.valueOf(resultMap.get("jobStatus")));
        experimentJobDAO.save(experimentJob);
        logger.info("Updating experimentJob: {}", experimentJob);
        logger.info("End update ExperimentJobStatusAndEnv");
        return experimentJob;
    }

    /**
     * 该方法用于根据id删除对应ExperimentJob
     * @param id 需要删除的ExperimentJob的id
     * @throws Exception
     */
    @Transactional
    public void removeExperimentJobById(long id) throws Exception {
        logger.info("Start remove ExperimentJobById");
        ExperimentJob experimentJob = experimentJobDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
        logger.info("findById id: {}", id);
        String username = experimentJob.getUser().getUsername();

        //1.删除out中的文件
        if(experimentJob.getExecutePath() != null)
            FileUtil.deleteFile(new File(experimentJob.getExecutePath()));

        //2.删除对应project关系
        List<Project> projects = experimentJob.getProjects();
        List<Long[]> removeList = new ArrayList<>();
        for (Project project : projects) {
            //将long类型的项目ID和实验ID加入到待移除list中
            removeList.add(new Long[]{project.getId(), experimentJob.getId()});
        }
        projectService.removeProjectExperimentJobRelation(removeList);

        //3.对应的graph解除关系
        Graph graph = experimentJob.getGraph();
        graph.getJobs().remove(experimentJob);
        graphDAO.save(graph);

        //4.解除与checkpoint的关系
        List<CheckPoint> checkPoints = experimentJob.getCheckPoints();
        for(CheckPoint checkPoint : checkPoints)
            checkPoint.setExperimentJob(null);
        experimentJob.setCheckPoints(null);

        //5.数据库删除experimentJob
        experimentJobDAO.delete(experimentJob);
        resourceUsageService.updateDiskCapacity(username);
        logger.info("End remove ExperimentJobById");

        //6.删除es对应索引
        Optional<ElasticExperimentJob> optional = experimentJobRepository.findById(experimentJob.getId().toString());
        if (optional.isPresent()) {
            experimentJobRepository.delete(optional.get());
            logger.info("删除es对应索引");
        }
    }

    /**
     * 此方法用于根据id终止对应的ExperimentJob
     * @param jobId 需要终止的ExperimentJob的id
     * @return 布尔类型变量，表示是否终止成功
     * @throws Exception
     */
    @Transactional
    public boolean slurmStopExperimentJob(long jobId) throws Exception {
        logger.info("Start stop Slurm ExperimentJob");
        ExperimentJob experimentJob = experimentJobDAO.findById(jobId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(jobId)));
        logger.info("findById jobId: {}", jobId);
        //1.从数据库中获取当前Job的状态
        String databaseJobStatus = experimentJob.getJobStatus().toString();
        String jobKey = experimentJob.getJobKey();
        //2.从slurm中获取当前job的状态
        String slurmJobStatus = jobService.getJobStatus(jobKey, JobType.EXPERIMENT_JOB, experimentJob.getUser());
        User user = experimentJob.getUser();
        SlurmAccount slurmAccount = user.getSlurmAccount();
        //3.如果与数据库中的不一致则返回false，并更新数据库
        if (!databaseJobStatus.equals(slurmJobStatus)) {
            updateExperimentJobStatusAndEnv(jobKey);
            logger.info("Fail to stop Slurm ExperimentJob");
            logger.info("End stop Slurm ExperimentJob");
            return false;
        } else {
            //一致则终止任务并查询slurm更新数据库并返回true
            int flag = Integer.parseInt(slurmService.stop(jobKey, slurmAccount));
            if (flag == 0) {
                updateExperimentJobStatusAndEnv(jobKey);
                //把jobKey从等待复制checkpoint队列中移出
                CheckPointScheduler.removeJobIfExited(jobKey);
                logger.info("Success to stop Slurm ExperimentJob");
                logger.info("End stop Slurm ExperimentJob");
                return true;
            } else {
                throw new BaseException(ResponseCode.CANCEL_JOB_ERROR);
            }
        }
    }

    /**
     * 该方法用于执行实验
     * @param graphVO 前端传来的图片信息
     * @param experimentJobVO 前端传来的实验信息
     * @param username 用户名
     * @throws Exception
     */
    @Transactional(rollbackFor = Exception.class, noRollbackFor = NoSlurmAccountException.class)
    public void executeExperiment(GraphVO graphVO, ExperimentJobVO experimentJobVO, String username) throws Exception {
        logger.info("Start execute Experiment");
        User user = userDAO.findByUsername(username);
        logger.info("findByUsername username: {}", username);
        SlurmAccount slurmAccount = user.getSlurmAccount();
        if (slurmAccount == null)
            throw new NoSlurmAccountException();
        Graph graph = GraphVOEntityMapper.MAPPER.graphVO2Graph(graphVO, graphDAO, componentDAO, daoUtil, datatypeDAO);
        logger.info("graphVO2Graph graphVO: {}", graphVO.toString());
        graphDAO.save(graph);

        //1.生成新的job信息，graph和job属于一对多关系，每次执行生成新的job
        //先将ExperimentJobVO转化为ExperimentJob
        logger.info("Start create a new job");
        ExperimentJob experimentJob = ExperimentJobVOEntityMapper.MAPPER.jobVO2Job(experimentJobVO, experimentJobDAO);
        logger.info("jobVO2Job experimentJobVO: {}", experimentJobVO.toString());
        //遍历graph中的component类型的节点
        for (Node node : graphVO.getNodeList()) {
            if (node.getComponentType() == NodeType.MODEL) {
                //将实验与该节点对应的component实体关联，默认实验图中只允许出现一个component
                Component component = componentDAO.findById(node.getComponentVO().getId()).orElseThrow(() -> new EntityNotFoundException(String.valueOf(node.getComponentVO().getId())));
                experimentJob.setComponent(component);
            }
        }

        experimentJob.setUser(user);
        logger.info("Success to create a new job");

        //2.更新job所用图的图信息
        logger.info("Start update graph of the job");
        Graph graph1 = graphDAO.findById(graph.getId()).orElseThrow(() -> new EntityNotFoundException());
        experimentJob.setGraph(graph1);
        experimentJobDAO.save(experimentJob);
        logger.info("Success to update graph of the job");

        //3.翻译实验对应的可执行代码，并保存根目录
        logger.info("Start translate the executable code and save the root directory");
        Map<String, Object> result = translationService.translateExperiment(graph, experimentJob, "");
        String rootPath = ((String) result.get("savePath")).replace("runtime_log", "");
        File outCodeFile = new File(rootPath, "Train.py");         //给用户下载入口程序都叫Train.py
        List componentFilePaths = (List) result.get("codeFilePathsList");     // 实验图中用到的所有节点的组件的文件路径
        //保存根目录
        experimentJob.setRootPath(rootPath);
        logger.info("the root directory: {}", rootPath);
        logger.info("Success to translate the executable code and save the root directory");

        //4.将实验的代码和用到的所有组件文件复制到 out/uuid 目录下，之后下载运行结果时，会将其和运行结果，一块拷到下载目录
        logger.info("Start copy the experimental code and all component files used to the out/uuid directory");
        FileUtil.copyComponentFiles(rootPath, componentFilePaths, filePath);
        String[] paths = {outCodeFile.getAbsolutePath(), (String) result.get("savePath")};
        logger.info("Success to copy the experimental code and all component files used to the out/uuid directory");

        //5.提交实验任务到slurm，执行实验并保存参数
        logger.info("Start submit the experiment task to slurm, execute the experiment and save the parameters");
        ObjectMapper objectMapper = new ObjectMapper();
        String args = SlurmUtil.getSlurmArgs(experimentJob.getName(), AizooConstans.SLURM_PYTHON_VERSION, paths, experimentJobVO.getEnvironment().get("slurmKwargs"), rootPath, null);
        experimentJob.setArgs(args);
        String jobKey = slurmService.startExperimentJob(args, slurmAccount);
        logger.info("Success to submit the experiment task to slurm, execute the experiment and save the parameters");

        //6.保存资源使用情况
        logger.info("Start save resource usage");
        Map<String, Object> slurmArgs = experimentJobVO.getEnvironment().get(AizooConstans.SLURM_RUN_ARGUMENTS_NAME);
        resourceUsageService.saveResourceUsageOfJob(slurmArgs, experimentJob.getId(), user);
        ResourceUsage ru = new ResourceUsage(ResourceType.EXPERIMENT_RUNNING_NUMBER, 1, experimentJob.getId(), user);
        resourceUsageDAO.save(ru);
        logger.info("Success to save resource usage");

        //7.保存jobKey、job的状态和该实验运行环境
        logger.info("Start save the Jobkey, the status of the job, and experiment args");
        experimentJob.setJobKey(jobKey);
        experimentJob.setJobStatus(JobStatus.valueOf(jobService.getJobStatus(jobKey, JobType.EXPERIMENT_JOB, user)));
        String environment = slurmService.showJob(jobKey, slurmAccount);
        //将前端传来的args存入environment
        Map<String, Object> environmentArgs = experimentJobVO.getEnvironment().get("args");
        Map<String, Map<String, Object>> environmentMap = objectMapper.readValue(environment, new TypeReference<Map<String, Map<String, Object>>>() {
        });
        environmentMap.put("args",environmentArgs);
        environment = objectMapper.writeValueAsString(environmentMap);
        //如果slurm返回的运行环境信息不为空则对实验运行环境进行更新
        if (!(environment == null || environment.isEmpty() || environment.equals("null")))
            experimentJob.setEnvironment(environment);
        experimentJobDAO.save(experimentJob);
        logger.info("Success to save the Jobkey, the status of the job, and experiment args");

        //8.保存nameSpace信息
        //将字符串变为Path对象，包括filePath,该实验用户名，checkpoint，实验ID
        logger.info("Start save nameSpace");
        Path path = Paths.get(filePath, experimentJob.getUser().getUsername(), "checkpoint", experimentJob.getId() + "");
        //将路径字符串中带有filePath/的部分去掉，并且将/替换为. 用于进行数据库操作
        //用修改后的字符串作为参数，查找数据库中的nameSpace
        Namespace nameSpace = namespaceDAO.findByNamespace(path.toString().replace(filePath + "/", "").replace("/", "."));
        if (nameSpace == null) {
            //如果数据库中不存在该nameSpace，则将其添加到数据库中
            nameSpace = new Namespace(path.toString().replace(filePath + "/", "").replace("/", "."));
            nameSpace.setPrivacy("private");
            nameSpace.setUser(experimentJob.getUser());
            namespaceDAO.save(nameSpace);
            logger.info("Success to save nameSpace");
        }
        //加入到等待更新状态、保存结果的jobs队列中，表示已完成但未保存的checkPoint
        CheckPointScheduler.addJob(jobKey);
        //加入到等待更新的资源使用情况队列中
        ResourceUsageScheduler.addJob(experimentJob.getId());
        logger.info("End execute Experiment");
    }
}
