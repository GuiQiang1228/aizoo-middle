package aizoo.service;

import aizoo.Client;
import aizoo.common.*;
import aizoo.common.exception.NoSlurmAccountException;
import aizoo.domain.*;
import aizoo.elasticObject.ElasticApplication;
import aizoo.elasticRepository.ApplicationRepository;
import aizoo.repository.*;
import aizoo.response.BaseException;
import aizoo.response.ResponseCode;
import aizoo.scheduler.ApplicationResultScheduler;
import aizoo.scheduler.ResourceUsageScheduler;
import aizoo.utils.*;
import aizoo.viewObject.mapper.ApplicationVOEntityMapper;
import aizoo.viewObject.mapper.GraphVOEntityMapper;
import aizoo.viewObject.object.ApplicationVO;
import aizoo.viewObject.object.GraphVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.io.File;
import java.nio.file.Paths;
import java.util.*;

@Service
public class ApplicationService {

    @Value("${download.dir}")
    String downloadDir;

    @Autowired
    ApplicationDAO applicationDAO;

    @Autowired
    UserDAO userDAO;

    @Autowired
    GraphDAO graphDAO;

    @Autowired
    DatatypeDAO datatypeDAO;

    @Autowired
    ComponentDAO componentDAO;

    @Autowired
    NamespaceDAO namespaceDAO;

    @Autowired
    VisualContainerDAO visualContainerDAO;

    @Autowired
    ApplicationResultDAO applicationResultDAO;

    @Autowired
    JobService jobService;

    @Autowired
    SlurmService slurmService;

    @Autowired
    ResourceUsageService resourceUsageService;

    @Autowired
    TranslationService translationService;

    @Autowired
    ResourceUsageDAO resourceUsageDAO;

    @Autowired
    DAOUtil daoUtil;

    @Value("${file.path}")
    String filePath;

    @Autowired
    Client client;

    @Value("${save.path}")
    private String savePath;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ProjectService projectService;

    @Autowired
    ApplicationRepository applicationRepository;

    @Autowired
    ServiceDAO serviceDAO;

    @Autowired
    DatasourceDAO datasourceDAO;

    private static final Logger logger = LoggerFactory.getLogger(ApplicationService.class);

    /**
     * 根据id删除指定的application
     *
     * @param id 需删除应用的id
     * @return true:删除成功
     * @throws Exception
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean removeAppById(long id) throws Exception {
        logger.info("Start remove App By Id");
        //根据id从数据库查找到对应的application
        Application app = applicationDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
        logger.info("根据id从数据库查找到对应的application, id:{}", id);
        //删除application对应的application results
        logger.info("删除application对应的application results");
        List<ApplicationResult> appResults = app.getAppResults();
        if (appResults != null) {
            // 删除result对应的文件
            String username = app.getUser().getUsername();
            String dirPath = Paths.get(downloadDir, username, "app_result", app.getId().toString()).toString();
            File dir = new File(dirPath);
            if (dir.exists())
                FileUtil.deleteFile(dir);
            applicationResultDAO.deleteAll(appResults);
        }
        // 解除app与result的关联，避免在删除时出现没删掉的情况
        app.setAppResults(null);
        //删除out中的文件，即app执行路径中的文件
        logger.info("删除out中的文件");
        if(app.getExecutePath() != null)
            FileUtil.deleteFile(new File(app.getExecutePath()));
        //删除对应project关系
        logger.info("删除对应project关系");
        List<Project> projects = app.getProjects();
        List<Long[]> removeList = new ArrayList<>();
        for (Project project : projects) {
            removeList.add(new Long[]{project.getId(), app.getId()});
        }
        projectService.removeProjectApplicationRelation(removeList);
        //数据库中删除application
        logger.info("数据库中删除application");
        applicationDAO.delete(app);
        //删除es对应索引
        logger.info("删除es对应索引");
        Optional<ElasticApplication> optional = applicationRepository.findById(app.getId().toString());
        if (optional.isPresent())
            applicationRepository.delete(optional.get());
        logger.info("End remove App By Id");
        return true;
    }

    /**
     * 翻译应用、将翻译出来的应用代码提交到slurm执行
     *
     * @param graphVO  前端传递的graphVO
     * @param appVO    前端传递的applicationVO
     * @param username 当前登录的用户的用户名，由principal获得
     * @throws Exception
     */
    @Transactional(rollbackFor = Exception.class, noRollbackFor = NoSlurmAccountException.class)
    public void executeApplication(GraphVO graphVO, ApplicationVO appVO, String username) throws Exception {
        logger.info("Start execute Application");
        //通过用户名在数据库找到改用户的实体
        User user = userDAO.findByUsername(username);
        logger.info("findByUsername username: {}", username);
        SlurmAccount slurmAccount = user.getSlurmAccount();
        //判断该用户的slurm账户是否存在，如果不存在的话抛出异常
        if (slurmAccount == null)
            throw new NoSlurmAccountException();
        //将前端传递的graphVO转换为graph实体
        Graph graph = GraphVOEntityMapper.MAPPER.graphVO2Graph(graphVO, graphDAO, componentDAO, daoUtil, datatypeDAO);
        logger.info("graphVO2Graph graphVO: {}", graphVO.toString());

        //  生成新的app信息，graph和app属于一对多关系，每次执行生成新的app
        Application app = ApplicationVOEntityMapper.MAPPER.applicationVO2Application(appVO, userDAO);
        logger.info("applicationVO2Application appVO: {}", appVO.toString());
        graphDAO.save(graph);
        app.setGraph(graphDAO.findById(graphVO.getId()).orElseThrow(() -> new EntityNotFoundException(String.valueOf(graphVO.getId()))));
        app.setUser(user);
        applicationDAO.save(app);

        //   翻译得到应用执行的代码
        String[] paths = translationService.translateApp(graph, app, "");

        //保存根目录
        String rootPath = new File(paths[0]).getParent();
        app.setRootPath(rootPath);

        // 将组成服务的所有原子组件放到fileListMap中
        // 这里所有的文件都来自用户自己，文件按对应的组件的namespace标识的存放
        logger.info("将组成应用的所有原子组件放到fileListMap中");
        HashMap<String, String> fileListMap = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        for (Node node : graphVO.getNodeList()) {
            if (node.getComponentType() != NodeType.PARAMETER && node.getComponentType() != NodeType.OPERATOR_TEMPLATE && node.getComponentType() != NodeType.OPERATOR && node.getComponentVO()!=null) {
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
        logger.info("组成应用的所有原子组件 fileListMap: {}", fileListMap.toString());

        // 复制服务执行时需要的所有文件到提交提交job运行的目录下（翻译器给的输出文件保存路径的上一层）
        logger.info("复制应用执行时需要的所有文件到提交job运行的目录下");
        for (String path : fileListMap.values()) {
            File fromCodeFile = new File(path);
            File toCodeFile;
            if (!path.equals(paths[0])) {
                toCodeFile = new File(rootPath, path.replace(filePath, ""));
                FileUtils.copyFile(fromCodeFile, toCodeFile);
            }
        }

        // 复制lib文件到执行文件目录下
        File fromLibDir = new File(filePath,"lib");
        File toLibDir = new File(paths[1].replace("runtime_log", ""));
        FileUtils.copyDirectoryToDirectory(fromLibDir,toLibDir);

        //   保存运行参数以及slurm参数
        String args = SlurmUtil.getSlurmArgs(app.getName(), AizooConstans.SLURM_PYTHON_VERSION, paths, appVO.getEnvironment().get("slurmKwargs"), rootPath ,null);
        app.setArgs(args);

        //   获取并保存jobkey
        String jobKey = client.applicationStart(args, slurmAccount);
        app.setJobKey(jobKey);
        //保存获取并保存job的状态
        app.setJobStatus(JobStatus.valueOf(jobService.getJobStatus(jobKey, JobType.APPLICATION, user)));
        //保存job的environment信息
        String environment = slurmService.showJob(jobKey, slurmAccount);
        //将前端传来的args存入environment
        Map<String, Object> environmentArgs = appVO.getEnvironment().get("args");
        Map<String, Map<String, Object>> environmentMap = objectMapper.readValue(environment, new TypeReference<Map<String, Map<String, Object>>>() {
        });
        environmentMap.put("args",environmentArgs);
        environment = objectMapper.writeValueAsString(environmentMap);

        if (!(environment == null || environment.isEmpty() || environment.equals("null")))
            app.setEnvironment(environment);

        applicationDAO.save(app);

        // 保存资源使用情况
        Map<String, Object> slurmArgs = appVO.getEnvironment().get("slurmKwargs");
        resourceUsageService.saveResourceUsageOfJob(slurmArgs, app.getId(), user);
        ResourceUsage ru = new ResourceUsage(ResourceType.APPLICATION_RUNNING_NUMBER, 1, app.getId(), user);
        resourceUsageDAO.save(ru);

        //将job加入到等待更新状态、保存结果的jobs队列中
        ApplicationResultScheduler.addJob(jobKey);
        //将job加入还没释放的资源的队列中
        ResourceUsageScheduler.addJob(app.getId());
        logger.info("End execute Application");
    }


    /**
     * 更新Application运行的status以及environment
     *
     * @param jobKey 需要更新的application的jobKey
     * @throws Exception
     */
    @Transactional
    public void updateApplicationJobStatusAndEnv(String jobKey) throws Exception {
        logger.info("Start update ApplicationJobStatusAndEnv");
        logger.info("jobKey: {}", jobKey);

        //通过jobKey来查找对应的application
        Application app = applicationDAO.findByJobKey(jobKey);
        User user = app.getUser();
        SlurmAccount slurmAccount = null;
        if (user != null)
            slurmAccount = user.getSlurmAccount();
        //如果slurm账户不存在，抛出异常
        if (slurmAccount == null)
            throw new NoSlurmAccountException();
        //根据slurm service查到的jobInfo，更新数据库里的原始的environment  以及job的状态
        String jobInfo = slurmService.showJob(jobKey, slurmAccount);
        Map<String, String> resultMap = JobUtil.updateJobStatusAndEnv(jobKey, jobInfo, app.getEnvironment());
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> jobKey2Environment = objectMapper.readValue(resultMap.get("environment"), new TypeReference<Map<String, Object>>() {
        });
        Map<String, Object> environment = (Map<String, Object>) jobKey2Environment.get(jobKey);
        if(environment.get("command") != null)
            app.setScriptPath(environment.get("command").toString());
        app.setEnvironment(resultMap.get("environment"));
        app.setJobStatus(JobStatus.valueOf(resultMap.get("jobStatus")));
        applicationDAO.save(app);
        logger.info("End update ApplicationJobStatusAndEnv");
    }

    /**
     * 停止指定的application job
     *
     * @param applicationJobId 需停止的application job的id
     * @return true:停止application成功   false:停止application失败
     * @throws Exception
     */
    @Transactional
    public boolean slurmStopApplication(long applicationJobId) throws Exception {
        logger.info("Start slurm Stop Application");
        //通过id在数据库找到对应的application
        Application application = applicationDAO.findById(applicationJobId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(applicationJobId)));
        logger.info("findById applicationJobId: {}", applicationJobId);
        //获取其对应的数据库中存的jobStatus以及jobKey
        String databaseJobStatus = application.getJobStatus().toString();
        String jobKey = application.getJobKey();
        //获取目前的jobStatus
        String slurmJobStatus = jobService.getJobStatus(jobKey, JobType.SERVICE_JOB, application.getUser());
        //如果数据库中的状态与现查到的状态不一样，进行更新，并返回停止application失败
        if (!databaseJobStatus.equals(slurmJobStatus)) {
            updateApplicationJobStatusAndEnv(jobKey);
            logger.info("Fail to stop slurm application");
            logger.info("End slurm Stop Application");
            return false;
        } else {      //如果状态一样，停止该application并返回停止成功
            User user = application.getUser();
            SlurmAccount slurmAccount = user.getSlurmAccount();
            int flag = Integer.parseInt(slurmService.stop(jobKey, slurmAccount));
            if (flag == 0) {
                updateApplicationJobStatusAndEnv(jobKey);
                //把jobKey从等待复制应用结果的job队列中移出
                ApplicationResultScheduler.removeJobIfExited(jobKey);
                logger.info("Success to stop slurm application");
                logger.info("End slurm Stop Application");
                return true;
            } else {
                throw new BaseException(ResponseCode.CANCEL_JOB_ERROR);
            }
        }
    }
}
