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
     * ?????????????????????ExperimentJob?????????
     * @param curStatus ??????????????????
     * @param experimentJob ???????????????ExperimentJob
     * @return ??????????????????ExperimentJob??????
     */
    @Transactional
    public ExperimentJob updateStatus(String curStatus, ExperimentJob experimentJob) {
        logger.info("Start update Status");
        logger.info("curStatus: {}", curStatus);
        logger.info("experimentJob: {}", experimentJob.toString());
        //?????????????????????????????????????????????????????????????????????ExperimentJob????????????
        JobStatus currentStatus = JobStatus.valueOf(curStatus);
        experimentJob.setJobStatus(currentStatus);
        experimentJobDAO.save(experimentJob);
        logger.info("Updating experimentJob status: {}", experimentJob);
        logger.info("End update Status");
        return experimentJob;
    }

    /**
     * ????????????job???status??????environment
     * @param jobKey ?????????????????????job
     * @return ??????????????????ExperimentJob??????
     * @throws Exception
     */
    @Transactional
    public ExperimentJob updateExperimentJobStatusAndEnv(String jobKey) throws Exception {
        logger.info("Start update ExperimentJobStatusAndEnv");
        logger.info("jobKey: {}", jobKey);
        //??????jobkey?????????????????????ExperimentJob????????????
        ExperimentJob experimentJob = experimentJobDAO.findByJobKey(jobKey);
        //?????????????????????slurm??????
        User user = experimentJob.getUser();
        SlurmAccount slurmAccount = user.getSlurmAccount();
        if (slurmAccount == null)
            throw new NoSlurmAccountException();
        //??????????????????status???environment??????
        String jobInfo = slurmService.showJob(jobKey, slurmAccount);
        Map<String, String> resultMap = JobUtil.updateJobStatusAndEnv(jobKey, jobInfo, experimentJob.getEnvironment());
        //???????????????????????????????????????????????????????????????
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
     * ?????????????????????id????????????ExperimentJob
     * @param id ???????????????ExperimentJob???id
     * @throws Exception
     */
    @Transactional
    public void removeExperimentJobById(long id) throws Exception {
        logger.info("Start remove ExperimentJobById");
        ExperimentJob experimentJob = experimentJobDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
        logger.info("findById id: {}", id);
        String username = experimentJob.getUser().getUsername();

        //1.??????out????????????
        if(experimentJob.getExecutePath() != null)
            FileUtil.deleteFile(new File(experimentJob.getExecutePath()));

        //2.????????????project??????
        List<Project> projects = experimentJob.getProjects();
        List<Long[]> removeList = new ArrayList<>();
        for (Project project : projects) {
            //???long???????????????ID?????????ID??????????????????list???
            removeList.add(new Long[]{project.getId(), experimentJob.getId()});
        }
        projectService.removeProjectExperimentJobRelation(removeList);

        //3.?????????graph????????????
        Graph graph = experimentJob.getGraph();
        graph.getJobs().remove(experimentJob);
        graphDAO.save(graph);

        //4.?????????checkpoint?????????
        List<CheckPoint> checkPoints = experimentJob.getCheckPoints();
        for(CheckPoint checkPoint : checkPoints)
            checkPoint.setExperimentJob(null);
        experimentJob.setCheckPoints(null);

        //5.???????????????experimentJob
        experimentJobDAO.delete(experimentJob);
        resourceUsageService.updateDiskCapacity(username);
        logger.info("End remove ExperimentJobById");

        //6.??????es????????????
        Optional<ElasticExperimentJob> optional = experimentJobRepository.findById(experimentJob.getId().toString());
        if (optional.isPresent()) {
            experimentJobRepository.delete(optional.get());
            logger.info("??????es????????????");
        }
    }

    /**
     * ?????????????????????id???????????????ExperimentJob
     * @param jobId ???????????????ExperimentJob???id
     * @return ?????????????????????????????????????????????
     * @throws Exception
     */
    @Transactional
    public boolean slurmStopExperimentJob(long jobId) throws Exception {
        logger.info("Start stop Slurm ExperimentJob");
        ExperimentJob experimentJob = experimentJobDAO.findById(jobId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(jobId)));
        logger.info("findById jobId: {}", jobId);
        //1.???????????????????????????Job?????????
        String databaseJobStatus = experimentJob.getJobStatus().toString();
        String jobKey = experimentJob.getJobKey();
        //2.???slurm???????????????job?????????
        String slurmJobStatus = jobService.getJobStatus(jobKey, JobType.EXPERIMENT_JOB, experimentJob.getUser());
        User user = experimentJob.getUser();
        SlurmAccount slurmAccount = user.getSlurmAccount();
        //3.??????????????????????????????????????????false?????????????????????
        if (!databaseJobStatus.equals(slurmJobStatus)) {
            updateExperimentJobStatusAndEnv(jobKey);
            logger.info("Fail to stop Slurm ExperimentJob");
            logger.info("End stop Slurm ExperimentJob");
            return false;
        } else {
            //??????????????????????????????slurm????????????????????????true
            int flag = Integer.parseInt(slurmService.stop(jobKey, slurmAccount));
            if (flag == 0) {
                updateExperimentJobStatusAndEnv(jobKey);
                //???jobKey???????????????checkpoint???????????????
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
     * ???????????????????????????
     * @param graphVO ???????????????????????????
     * @param experimentJobVO ???????????????????????????
     * @param username ?????????
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

        //1.????????????job?????????graph???job????????????????????????????????????????????????job
        //??????ExperimentJobVO?????????ExperimentJob
        logger.info("Start create a new job");
        ExperimentJob experimentJob = ExperimentJobVOEntityMapper.MAPPER.jobVO2Job(experimentJobVO, experimentJobDAO);
        logger.info("jobVO2Job experimentJobVO: {}", experimentJobVO.toString());
        //??????graph??????component???????????????
        for (Node node : graphVO.getNodeList()) {
            if (node.getComponentType() == NodeType.MODEL) {
                //??????????????????????????????component??????????????????????????????????????????????????????component
                Component component = componentDAO.findById(node.getComponentVO().getId()).orElseThrow(() -> new EntityNotFoundException(String.valueOf(node.getComponentVO().getId())));
                experimentJob.setComponent(component);
            }
        }

        experimentJob.setUser(user);
        logger.info("Success to create a new job");

        //2.??????job?????????????????????
        logger.info("Start update graph of the job");
        Graph graph1 = graphDAO.findById(graph.getId()).orElseThrow(() -> new EntityNotFoundException());
        experimentJob.setGraph(graph1);
        experimentJobDAO.save(experimentJob);
        logger.info("Success to update graph of the job");

        //3.?????????????????????????????????????????????????????????
        logger.info("Start translate the executable code and save the root directory");
        Map<String, Object> result = translationService.translateExperiment(graph, experimentJob, "");
        String rootPath = ((String) result.get("savePath")).replace("runtime_log", "");
        File outCodeFile = new File(rootPath, "Train.py");         //?????????????????????????????????Train.py
        List componentFilePaths = (List) result.get("codeFilePathsList");     // ?????????????????????????????????????????????????????????
        //???????????????
        experimentJob.setRootPath(rootPath);
        logger.info("the root directory: {}", rootPath);
        logger.info("Success to translate the executable code and save the root directory");

        //4.????????????????????????????????????????????????????????? out/uuid ?????????????????????????????????????????????????????????????????????????????????????????????
        logger.info("Start copy the experimental code and all component files used to the out/uuid directory");
        FileUtil.copyComponentFiles(rootPath, componentFilePaths, filePath);
        String[] paths = {outCodeFile.getAbsolutePath(), (String) result.get("savePath")};
        logger.info("Success to copy the experimental code and all component files used to the out/uuid directory");

        //5.?????????????????????slurm??????????????????????????????
        logger.info("Start submit the experiment task to slurm, execute the experiment and save the parameters");
        ObjectMapper objectMapper = new ObjectMapper();
        String args = SlurmUtil.getSlurmArgs(experimentJob.getName(), AizooConstans.SLURM_PYTHON_VERSION, paths, experimentJobVO.getEnvironment().get("slurmKwargs"), rootPath, null);
        experimentJob.setArgs(args);
        String jobKey = slurmService.startExperimentJob(args, slurmAccount);
        logger.info("Success to submit the experiment task to slurm, execute the experiment and save the parameters");

        //6.????????????????????????
        logger.info("Start save resource usage");
        Map<String, Object> slurmArgs = experimentJobVO.getEnvironment().get(AizooConstans.SLURM_RUN_ARGUMENTS_NAME);
        resourceUsageService.saveResourceUsageOfJob(slurmArgs, experimentJob.getId(), user);
        ResourceUsage ru = new ResourceUsage(ResourceType.EXPERIMENT_RUNNING_NUMBER, 1, experimentJob.getId(), user);
        resourceUsageDAO.save(ru);
        logger.info("Success to save resource usage");

        //7.??????jobKey???job?????????????????????????????????
        logger.info("Start save the Jobkey, the status of the job, and experiment args");
        experimentJob.setJobKey(jobKey);
        experimentJob.setJobStatus(JobStatus.valueOf(jobService.getJobStatus(jobKey, JobType.EXPERIMENT_JOB, user)));
        String environment = slurmService.showJob(jobKey, slurmAccount);
        //??????????????????args??????environment
        Map<String, Object> environmentArgs = experimentJobVO.getEnvironment().get("args");
        Map<String, Map<String, Object>> environmentMap = objectMapper.readValue(environment, new TypeReference<Map<String, Map<String, Object>>>() {
        });
        environmentMap.put("args",environmentArgs);
        environment = objectMapper.writeValueAsString(environmentMap);
        //??????slurm????????????????????????????????????????????????????????????????????????
        if (!(environment == null || environment.isEmpty() || environment.equals("null")))
            experimentJob.setEnvironment(environment);
        experimentJobDAO.save(experimentJob);
        logger.info("Success to save the Jobkey, the status of the job, and experiment args");

        //8.??????nameSpace??????
        //??????????????????Path???????????????filePath,?????????????????????checkpoint?????????ID
        logger.info("Start save nameSpace");
        Path path = Paths.get(filePath, experimentJob.getUser().getUsername(), "checkpoint", experimentJob.getId() + "");
        //???????????????????????????filePath/???????????????????????????/?????????. ???????????????????????????
        //????????????????????????????????????????????????????????????nameSpace
        Namespace nameSpace = namespaceDAO.findByNamespace(path.toString().replace(filePath + "/", "").replace("/", "."));
        if (nameSpace == null) {
            //??????????????????????????????nameSpace?????????????????????????????????
            nameSpace = new Namespace(path.toString().replace(filePath + "/", "").replace("/", "."));
            nameSpace.setPrivacy("private");
            nameSpace.setUser(experimentJob.getUser());
            namespaceDAO.save(nameSpace);
            logger.info("Success to save nameSpace");
        }
        //?????????????????????????????????????????????jobs??????????????????????????????????????????checkPoint
        CheckPointScheduler.addJob(jobKey);
        //???????????????????????????????????????????????????
        ResourceUsageScheduler.addJob(experimentJob.getId());
        logger.info("End execute Experiment");
    }
}
