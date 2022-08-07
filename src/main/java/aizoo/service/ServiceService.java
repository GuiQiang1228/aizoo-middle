package aizoo.service;

import aizoo.Client;
import aizoo.common.*;
import aizoo.common.exception.NoSlurmAccountException;
import aizoo.domain.*;
import aizoo.elasticObject.ElasticServiceJob;
import aizoo.elasticRepository.ServiceJobRepository;
import aizoo.repository.*;
import aizoo.response.BaseException;
import aizoo.response.ResponseCode;
import aizoo.scheduler.ResourceUsageScheduler;
import aizoo.scheduler.ServiceIPScheduler;
import aizoo.utils.DAOUtil;
import aizoo.utils.FileUtil;
import aizoo.utils.JobUtil;
import aizoo.utils.SlurmUtil;
import aizoo.utils.AizooConstans;
import aizoo.viewObject.mapper.GraphVOEntityMapper;
import aizoo.viewObject.mapper.ServiceJobVOEntityMapper;
import aizoo.viewObject.mapper.ServiceVOEntityMapper;
import aizoo.viewObject.object.GraphVO;
import aizoo.viewObject.object.ServiceJobVO;
import aizoo.viewObject.object.ServiceVO;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.netty.shaded.io.netty.util.internal.StringUtil;
import org.apache.commons.io.FileUtils;
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
import java.util.*;
import java.util.regex.Matcher;

@Service("ServiceService")
public class ServiceService {

    private final static Logger logger = LoggerFactory.getLogger(ServiceService.class);

    @Value("${file.path}")
    String file_path;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private DatatypeDAO datatypeDAO;

    @Autowired
    private ServiceDAO serviceDAO;

    @Autowired
    private ComponentDAO componentDAO;

    @Autowired
    private DAOUtil daoUtil;

    @Autowired
    GraphDAO graphDAO;

    @Autowired
    NamespaceDAO namespaceDAO;

    @Autowired
    ServiceJobDAO serviceJobDAO;

    @Autowired
    JobService jobService;

    @Autowired
    SlurmService slurmService;

    @Autowired
    TranslationService translationService;

    @Autowired
    ResourceUsageService resourceUsageService;

    @Autowired
    ResourceUsageDAO resourceUsageDAO;

    @Autowired
    ServiceInputParameterDAO serviceInputParameterDAO;

    @Autowired
    ServiceOutputParameterDAO serviceOutputParameterDAO;

    @Autowired
    Client client;

    @Value("${file.path}")
    String filePath;

    @Autowired
    ProjectService projectService;

    @Autowired
    ServiceJobRepository serviceJobRepository;

    @Autowired
    DatasourceDAO datasourceDAO;

    /**
     * 执行ServiceJob服务任务的方法，主要包括以下过程：
     * 1. 判断组件是否是首次发布，如果不是则删除之前发布的输入输出节点
     * 2. 调用翻译器功能，翻译出serviceJob的代码
     * 3. 组织好服务图中的所有子组件的存放路径
     * 4. 复制服务执行时所有的文件到指定下载结果路径下
     * 5. 调用slurm端方法，提交serviceJob任务到服务器执行
     * 6. 保存本次执行任务，资源的使用情况
     * 7. jobKey加入定时查询器中，等待任务进入到RUNNING状态时将ip和port写入数据库
     *
     * @param graphVO      服务图信息
     * @param serviceVO    服务组件信息
     * @param serviceJobVO 服务任务信息
     * @param username     用户名
     * @throws Exception
     */
    @Transactional(rollbackFor = Exception.class, noRollbackFor = NoSlurmAccountException.class)
    public void executeService(GraphVO graphVO, ServiceVO serviceVO, ServiceJobVO serviceJobVO, String username) throws Exception {
        logger.info("Start execute Service");
        logger.info("graphVO = {}",graphVO.toString());
        logger.info("serviceVO = {}",serviceVO.toString());
        logger.info("serviceJobVO = {}",serviceJobVO.toString());
        logger.info("username = {}",username);
        User user = userDAO.findByUsername(username);
        SlurmAccount slurmAccount = user.getSlurmAccount();
        if (slurmAccount == null)
            throw new NoSlurmAccountException();
        aizoo.domain.Service service2 = serviceDAO.findById(serviceVO.getId()).orElseThrow(() -> new EntityNotFoundException(String.valueOf(serviceVO.getId())));

        // 该组件非首次发布先删除之前发布时的输入输出
        if (!StringUtil.isNullOrEmpty(service2.getPath())) {
            List<ServiceInputParameter> serviceInputParameters = service2.getInputs();
            for (ServiceInputParameter serviceInputParameter : serviceInputParameters) {
                serviceInputParameter.setService(null);
                serviceInputParameterDAO.delete(serviceInputParameter);
            }
            List<ServiceOutputParameter> serviceOutputParameters = service2.getOutputs();
            for (ServiceOutputParameter serviceOutputParameter : serviceOutputParameters) {
                serviceOutputParameter.setService(null);
                serviceOutputParameterDAO.delete(serviceOutputParameter);
            }
            service2.setInputs(null);
            service2.setOutputs(null);
            serviceDAO.save(service2);
        }


        Graph graph = GraphVOEntityMapper.MAPPER.graphVO2Graph(graphVO, graphDAO, componentDAO, daoUtil, datatypeDAO);
        graphDAO.save(graph);
        // 更新service信息
        logger.info("更新service信息");
        aizoo.domain.Service service = ServiceVOEntityMapper.MAPPER.ServiceVO2Service(serviceVO, serviceDAO, datatypeDAO);
        // 给每个新增的服务的output都加一个输出为self
        ServiceOutputParameter selfOutput = new ServiceOutputParameter();
        selfOutput.setIsSelf(true);
        selfOutput.setParameter(new Parameter(AizooConstans.SELF_NAME, AizooConstans.SELF_TITLE, AizooConstans.SELF_DESCRIPTION, AizooConstans.SELF_ORIGIN_NAME, datatypeDAO.findByName(AizooConstans.AIZOO_UNKNOWN)));
        service.getOutputs().add(selfOutput);

        // 生成服务对应的可执行代码并返回存放路径
        // 由namespace生成service的存放路径
        logger.info("生成服务对应的可执行代码并返回存放路径");
        String nsPath = serviceVO.getNamespace().replaceAll("\\.", Matcher.quoteReplacement(File.separator));
        String targetPath = Paths.get(file_path, nsPath).toString();

        // 翻译service的代码
        logger.info("翻译service的代码");
        Integer port = (Integer) serviceJobVO.getEnvironment().get("args").get("port");  // 获取端口号，service可指定端口，不指定时port=null
        String[] paths = translationService.translateService(graph, targetPath, port);
        service.setPath(paths[0]);

        ServiceJob serviceJob = ServiceJobVOEntityMapper.MAPPER.serviceJobVO2ServiceJob(serviceJobVO, serviceJobDAO);

        //保存根目录
        String rootPath = new File(paths[1]).getParent();
        serviceJob.setRootPath(rootPath);

        // 将组成服务的所有原子组件放到fileListMap中
        // 这里所有的文件都来自用户自己，文件按对应的组件的namespace标识的存放
        logger.info("将组成服务的所有原子组件放到fileListMap中");
        HashMap<String, String> fileListMap = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        for (Node node : graphVO.getNodeList()) {
            if (node.getComponentType() != NodeType.PARAMETER && node.getComponentType() != NodeType.OPERATOR_TEMPLATE && node.getComponentType() != NodeType.OPERATOR) {
                Component nodeComponent = componentDAO.findById(node.getComponentVO().getId()).orElseThrow(() -> new EntityNotFoundException(String.valueOf(node.getComponentVO().getId())));
                String nodeComponentVersion = "";
                if (nodeComponent.getComponentVersion() != null) {
                    nodeComponentVersion = nodeComponent.getComponentVersion().replace(".", "_");
                }
                // 添加fileList
                if (nodeComponent.getFileList() == null) {
                    fileListMap.put(nodeComponent.getName() + nodeComponentVersion + ".py", nodeComponent.getPath());
                } else {   // 保存子节点fileList中的所有数据
                    fileListMap.putAll(objectMapper.readValue(nodeComponent.getFileList(), new TypeReference<Map<String, String>>() {
                    }));
                }
            }
        }
        //自己的路径存放在复合组件的fileListMap中
        // 翻译出来的文件本身，这个文件只存在于{out.path}/{uuid}中，没有namespace，也没有复制到file.path中去
        fileListMap.put(serviceVO.getName() + ".py", service.getPath());
        service.setFileList(objectMapper.writeValueAsString(fileListMap));

        // 复制服务执行时需要的所有文件到提交提交job运行的目录下（翻译器给的输出文件保存路径的上一层）
        logger.info("复制服务执行时需要的所有文件到提交提交job运行的目录下");
        for (String path : fileListMap.values()) {
            File fromCodeFile = new File(path);
            File toCodeFile;
            if (!path.equals(paths[0])) {
                toCodeFile = new File(rootPath, path.replace(filePath, ""));
            } else {
                toCodeFile = new File(rootPath, serviceVO.getName() + ".py");
            }
            FileUtils.copyFile(fromCodeFile, toCodeFile);
        }
        // service是从vo转的，没有token，所以要从数据库里查出来，给它带上token
        aizoo.domain.Service service1 = serviceDAO.findById(serviceVO.getId()).orElseThrow(() -> new EntityNotFoundException(String.valueOf(serviceVO.getId())));
        service.setName(graph.getName());
        service.setTitle(graph.getName());
        service.setToken(service1.getToken());
        service.setPrivacy(graphVO.getGraphPrivacy());
        serviceDAO.save(service);


        Path executePath = Paths.get(paths[1].replace("runtime_log", ""), serviceVO.getName() + ".py");
        paths[0] = executePath.toString();
        // 1.path[0]是service代码存放地址,path[1]是log地址，先请求getSlurmArgs得到slurm所需要的的参数格式，
        // 2.再通过client.serviceStart请求slurm，slurm返回jobKey
        // 3.保存两个参数
        String args = SlurmUtil.getSlurmArgs(serviceJob.getName(), AizooConstans.SLURM_PYTHON_VERSION, paths, serviceJobVO.getEnvironment().get("slurmKwargs"), rootPath, null);
        serviceJob.setArgs(args);

        String jobKey = client.serviceStart(args, slurmAccount);

        // 保存serviceJob到数据库中
        logger.info("保存serviceJob到数据库中");

        serviceJob.setJobKey(jobKey);
        serviceJob.setUser(graph.getUser());
        serviceJob.setJobStatus(JobStatus.valueOf(jobService.getJobStatus(jobKey, JobType.SERVICE_JOB, user)));
        serviceJob.setService(service1);
        serviceJob.setGraph(graphDAO.findByGraphKey(graph.getGraphKey()));
        serviceJob.setDescription(serviceJobVO.getDescription());
        serviceJob.setUrl(graph.getName() + "/");
        String environment = slurmService.showJob(jobKey, slurmAccount);
        //将前端传来的args存入environment
        Map<String, Object> environmentArgs = serviceJobVO.getEnvironment().get("args");
        Map<String, Map<String, Object>> environmentMap = objectMapper.readValue(environment, new TypeReference<Map<String, Map<String, Object>>>() {
        });
        environmentMap.put("args",environmentArgs);
        environment = objectMapper.writeValueAsString(environmentMap);

        if (!(environment == null || environment.isEmpty() || environment.equals("null")))
            serviceJob.setEnvironment(environment);
        serviceJobDAO.save(serviceJob);

        //保存执行路径
        JSONObject jsonEnvironment = JSONObject.parseObject(serviceJob.getEnvironment());
        JSONObject job = JSONObject.parseObject(jsonEnvironment.getString(serviceJob.getJobKey()));
        //获取std_out所在的路径(.out文件路径)
        //path={filePath}/out/{uuid}/runtime_log/xxx.out
        String outPath = job.getString("std_out");
        //这里的filePath是通过@Value注入的配置文件中的默认存放路径，将其替换为空字符串后得到相对路径
        //替换之后path为 /out/{uuid}/xxx
        outPath = outPath.replace(filePath, "");
        //将相对路径根据/分割放入数组中
        String[] splitPath = outPath.split("/");
        //将其按/分隔后的数组内有五个元素，第一个是个空的str，第2,3个为执行路径所需的两层，即out/{uuid}，将其拼接到目标路径
        if (splitPath.length >= 3) {
            String path = filePath + '/' + splitPath[1] + '/' + splitPath[2];
            serviceJob.setExecutePath(path);
        }

        // 保存资源使用情况
        logger.info("保存资源使用情况");
        Map<String, Object> slurmArgs = serviceJobVO.getEnvironment().get("slurmKwargs");
        resourceUsageService.saveResourceUsageOfJob(slurmArgs, serviceJob.getId(), user);
        ResourceUsage ru = new ResourceUsage(ResourceType.SERVICE_RUNNING_NUMBER, 1, serviceJob.getId(), user);
        resourceUsageDAO.save(ru);

        // jobKey加入定时查询Set中等待RUNNING状态并将ip和port写入数据库
        logger.info("jobKey加入定时查询Set中等待RUNNING状态并将ip和port写入数据库");
        ServiceIPScheduler.addJob(serviceJob.getJobKey());
        ResourceUsageScheduler.addJob(serviceJob.getId());
        logger.info("End execute Service");
    }

    /**
     * 停止serviceJob的运行
     * 先查询数据库中serviceJob的状态、再通过slurm端获取服务器上serviceJob的最新状态，并进行比较
     * 如果状态不同则强制更新数据库中任务状态
     * 状态相同，则尝试通过slurm端停止任务运行，并更新数据库中的任务状态
     *
     * @param serviceJobId ServiceJob任务的id
     * @return boolean类型，若停止任务失败则返回false，否则返回true
     * @throws Exception
     */
    @Transactional
    public boolean slurmStopServiceJob(long serviceJobId) throws Exception {
        logger.info("Start slurm Stop Service Job");
        ServiceJob serviceJob = serviceJobDAO.findById(serviceJobId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(serviceJobId)));
        String databaseJobStatus = serviceJob.getJobStatus().toString();
        String jobKey = serviceJob.getJobKey();
        String slurmJobStatus = jobService.getJobStatus(jobKey, JobType.SERVICE_JOB, serviceJob.getUser());
        User user = serviceJob.getUser();
        SlurmAccount slurmAccount = user.getSlurmAccount();
        if (slurmAccount == null)
            throw new NoSlurmAccountException();
        if (!databaseJobStatus.equals(slurmJobStatus)) {
            updateServiceJobStatusAndEnv(jobKey);
            logger.info("return false, End slurm Stop Service Job");
            return false;
        } else {
            int flag = Integer.parseInt(slurmService.stop(jobKey, slurmAccount));
            if (flag == 0) {
                updateServiceJobStatusAndEnv(jobKey);
                //把jobKey从等待获取ip和端口的job队列中移出
                ServiceIPScheduler.removeJobIfExited(jobKey);
                logger.info("return true, End slurm Stop Service Job");
                return true;
            } else {
                throw new BaseException(ResponseCode.CANCEL_JOB_ERROR);
            }
        }
    }

    /**
     * 通过slurm查询任务的最新信息
     * 更新数据库中serviceJob的status和environment
     *
     * @param jobKey 任务的jobKey值
     * @throws Exception
     */
    @Transactional
    public void updateServiceJobStatusAndEnv(String jobKey) throws Exception {
        logger.info("Start update Service Job Status And Env");
        logger.info("updateServiceJobStatusAndEnv jobKey:{}", jobKey);
        ServiceJob serviceJob = serviceJobDAO.findByJobKey(jobKey);
        User user = serviceJob.getUser();
        SlurmAccount slurmAccount = user.getSlurmAccount();

        // 从slurm查询到的信息
        String jobInfo = slurmService.showJob(jobKey, slurmAccount);
        Map<String, String> resultMap = JobUtil.updateJobStatusAndEnv(jobKey, jobInfo, serviceJob.getEnvironment());
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> jobKey2Environment = objectMapper.readValue(resultMap.get("environment"), new TypeReference<Map<String, Object>>() {
        });
        Map<String, Object> environment = (Map<String, Object>) jobKey2Environment.get(jobKey);
        if (environment.get("command") != null)
            serviceJob.setScriptPath(environment.get("command").toString());
        serviceJob.setEnvironment(resultMap.get("environment"));
        serviceJob.setJobStatus(JobStatus.valueOf(resultMap.get("jobStatus")));
        serviceJobDAO.save(serviceJob);
        logger.info("End update Service Job Status And Env");
    }

    /**
     * 获取serviceJob的详细信息，包括serviceJob任务信息和它用到的service服务组件信息
     *
     * @param serviceJobId ServiceJob任务的id
     * @return Map<String, Object>类型，格式为：{"serviceJob":"服务任务的信息：serviceJob","service":"服务组件的信息：serviceVO"}
     */
    public Map<String, Object> getServiceVOAndServiceJobVO(Long serviceJobId) {
        logger.info("Start get ServiceVO And Service JobVO");
        logger.info("getServiceVOAndServiceJobVO:{}", serviceJobId);
        ServiceJob serviceJob = serviceJobDAO.findById(serviceJobId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(serviceJobId)));
        ServiceJobVO serviceJobVO = ServiceJobVOEntityMapper.MAPPER.serviceJob2ServiceJobVO(serviceJob);
        aizoo.domain.Service service = serviceJob.getService();
        ServiceVO serviceVO = ServiceVOEntityMapper.MAPPER.Service2ServiceVO(service);
        Map<String, Object> result = new HashMap<>();
        result.put("serviceJob", serviceJobVO);
        result.put("service", serviceVO);
        logger.info("getServiceVOAndServiceJobVO return:{}", result);
        logger.info("End get ServiceVO And Service JobVO");
        return result;
    }

    /**
     * 删除 ServiceJob以及out目录下相关的文件、与project的关系、es索引
     *
     * @param serviceJobId ServiceJob任务的id
     * @param username     用户名
     * @return boolean类型，删除过程成功执行完成则返回true，若用户名参数与serviceJob所属用户不同，则返回false拒绝删除
     * @throws Exception
     */
    @Transactional
    public boolean deleteServiceJob(long serviceJobId, String username) throws Exception {
        ServiceJob serviceJob = serviceJobDAO.findById(serviceJobId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(serviceJobId)));
        if (!username.equals(serviceJob.getUser().getUsername())) {
            return false;
        }
        //删除out中的文件
        logger.info("删除out中的文件");
        if (serviceJob.getExecutePath() != null)
            FileUtil.deleteFile(new File(serviceJob.getExecutePath()));

        //删除对应project关系
        logger.info("删除对应project关系");
        List<Project> projects = serviceJob.getProjects();
        List<Long[]> removeList = new ArrayList<>();
        for (Project project : projects) {
            removeList.add(new Long[]{project.getId(), serviceJob.getId()});
        }
        projectService.removeProjectServiceJobRelation(removeList);
        serviceJobDAO.delete(serviceJob);
        //删除对应es索引
        logger.info("删除对应es索引");
        Optional<ElasticServiceJob> optional = serviceJobRepository.findById(serviceJob.getId().toString());
        if (optional.isPresent())
            serviceJobRepository.delete(optional.get());
        return true;
    }
}
