package aizoo.controller;

import aizoo.Client;
import aizoo.aspect.ResourceCheck;
import aizoo.common.GraphType;
import aizoo.common.ResourceType;
import aizoo.domain.*;
import aizoo.repository.*;
import aizoo.response.BaseResponse;
import aizoo.service.WebSocket;
import aizoo.utils.DAOUtil;
import aizoo.utils.ListEntity2ListVO;
import aizoo.viewObject.mapper.ComponentVOEntityMapper;
import aizoo.viewObject.mapper.ExperimentJobVOEntityMapper;
import aizoo.viewObject.mapper.GraphVOEntityMapper;
import aizoo.viewObject.mapper.ServiceVOEntityMapper;
import aizoo.viewObject.object.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@BaseResponse
@RestController
@RequestMapping("/test")
public class TestController {
    @Autowired
    private UserDAO userDAO;
    @Autowired
    private GraphDAO graphDAO;
    @Autowired
    private DAOUtil daoUtil;

    @Autowired
    private ComponentDAO componentDAO;

    @Autowired
    private DatatypeDAO datatypeDAO;

    @Autowired
    private ExperimentJobDAO experimentJobDAO;

    @Autowired
    NamespaceDAO namespaceDAO;

    @Autowired
    CheckPointDAO checkPointDAO;

    @Autowired
    ServiceDAO serviceDAO;

    @Autowired
    Client client;

    @Autowired
    ObjectMapper objectMapper;

    private static Client rpcClient;

    @Autowired
    public void setClient(Client client) {
        TestController.rpcClient = client;
    }

    /**
     * 测试根据用户名查找数据库
     *
     * @param name 用户名
     */

    @RequestMapping(value = "/testCascade/{name}", method = RequestMethod.GET)
    public void testCascade(@PathVariable(value = "name") String name) {

        //1.测试根据用户名查找用户对象并进行删除
        User u1 = userDAO.findByUsername(name);
        userDAO.delete(u1);//不能直接删，需要先改外键
    }


    /**
     * 将GraphvO实体转化为Graph实体
     *
     * @param graphVO 格式为{name, graphKey, graphType, componentType, nodeList, linkList, originJson, id}
     * @return 保存的Graph实体
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/saveGraph", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public GraphVO saveGraph(@RequestBody GraphVO graphVO) {
        Graph graph = GraphVOEntityMapper.MAPPER.graphVO2Graph(graphVO, graphDAO, componentDAO, daoUtil, datatypeDAO);
        System.out.println(graph);
        graphDAO.save(graph);
        Graph graph1 = graphDAO.findByGraphKey(graphVO.getGraphKey());
        return GraphVOEntityMapper.MAPPER.graph2GraphVO(graph1);
    }

    /**
     * 将experimentJobVO保存到数据库中并返回job的状态
     *
     * @param experimentJobVO 格式为{name, jobKey, jobStatus, description, environment, graphName, id}
     * @return jobStatus.name()返回job状态
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/saveJob", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public ExperimentJobVO saveJob(@RequestBody ExperimentJobVO experimentJobVO) {
        ExperimentJob experimentJob1 = ExperimentJobVOEntityMapper.MAPPER.jobVO2Job(experimentJobVO, experimentJobDAO);
        experimentJobDAO.save(experimentJob1);
        ExperimentJob experimentJob2 = experimentJobDAO.findByJobKey("130");
        return ExperimentJobVOEntityMapper.MAPPER.job2JobVO(experimentJob2);
    }

    /**
     * 获得用户super的checkpoint列表
     *
     * @return checkpoint列表
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/checkPointVO", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    public List<CheckPointVO> checkPointVO() {
        List<CheckPoint> checkPoint = checkPointDAO.findByUserUsername("super");
        return ListEntity2ListVO.checkPoint2CheckPointVO(checkPoint);
    }

    /**
     * 获得用户super的服务器节点列表
     *
     * @return 服务器列表
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/servicePoint", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    public List<ServiceVO> servicePointVO() {
        List<Service> services = serviceDAO.findByUserUsername("super");
        return ListEntity2ListVO.service2ServiceVO(services);
    }

    /**
     * 将serviceVO 实体转化为service实体并返回
     *
     * @param serviceVO 实体
     * @return
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/servicePointVO", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public Service servicePointVO(@RequestBody ServiceVO serviceVO) {
        return ServiceVOEntityMapper.MAPPER.ServiceVO2Service(serviceVO, serviceDAO, datatypeDAO);
    }

    /**
     * 获取用户"super"的component列表
     *
     * @return component列表
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/component", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    public List<ComponentVO> componentVO() {
        List<Component> components = componentDAO.findByUserUsername("super");
        return ListEntity2ListVO.component2ComponentVO(components);
    }

    /**
     * 将componentVO实体转化为component实体并返回
     *
     * @param componentVO 组件实体
     * @return component实体
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/componentVO", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public Component componentVO(@RequestBody ComponentVO componentVO) {
        return ComponentVOEntityMapper.MAPPER.componentVO2Component(componentVO, datatypeDAO, componentDAO, namespaceDAO, userDAO);
    }

    /**
     * 获取用户"super"图列表
     *
     * @return Graph列表
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/graph", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    public List<GraphVO> graphVO() {
        List<Graph> graphs = graphDAO.findByUserUsernameAndGraphType("super", GraphType.COMPONENT);
        return ListEntity2ListVO.graph2GraphVO(graphs);
    }

    /**
     * 将GraphVO实体转化为Graph实体并进行保存
     *
     * @param graphVO
     * @return Graph实体
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/graphVO", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public Graph graphVO(@RequestBody GraphVO graphVO) {
        Graph graph = GraphVOEntityMapper.MAPPER.graphVO2Graph(graphVO, graphDAO, componentDAO, daoUtil, datatypeDAO);
        graphDAO.save(graph);
        return graph;
    }

    /**
     * 根据用户名找到用户实体清除Experimentjob列表并返回用户实体
     *
     * @param username 用户名
     * @return User实体
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/user", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    public User userVO(@RequestParam String username) {
        User user = userDAO.findByUsername(username);
        user.getExperimentJobs().clear();
        userDAO.save(user);
        return user;
    }

    //测试文件
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/resource", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResourceCheck(resourceTypes = {ResourceType.CPU, ResourceType.DISK})
    public void test(int age, @RequestParam("fileSize") int fileSize, String type) {
        System.out.println("测试专用");
    }

    /**
     * 获取checkPoint实体
     *
     * @param checkpointName  检查节点名称
     * @param experimentJobId 实验任务的id
     * @param namespace       命名空间
     * @return checkPoint实体包括id和命名空间
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/checkPointSearch", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    public List<CheckPoint> checkPointSearch(String checkpointName, long experimentJobId, String namespace) {
        return checkPointDAO.findByNameAndExperimentJobIdAndAndNamespaceNamespace(checkpointName, experimentJobId, namespace);
    }


    /**
     * 测试parseJobData返回值是否正确
     * @param jobKey
     * @return parseJobData返回值，样例见Websocket.parseJobData.return.md
     * @throws Exception
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/parseJobDataTest", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    public Object parseJobDataTest(String jobKey) throws Exception {
        // 1. 根据jobKey在数据库里查到对应job
        ExperimentJob experimentJob = experimentJobDAO.findByJobKey(jobKey);

        // 2. 向slurm server发请求，
        User user = experimentJob.getUser();
        SlurmAccount slurmAccount = user.getSlurmAccount();
        String jobUsageStr = client.getJobUsage(jobKey, slurmAccount);
        String jobSummaryStr = client.getJobSummary(jobKey, slurmAccount);
        Map<String, List> newJobUsage = objectMapper.readValue(jobUsageStr, new TypeReference<Map<String, List>>() {
        });
        Map<String, Object> newJobSummary = objectMapper.readValue(jobSummaryStr, new TypeReference<Map<String, Object>>() {
        });
        // 获取Job的loss和metric
        String rootPath = experimentJob.getRootPath();
        Map<String, String> rootPathMap = new HashMap<>();
        rootPathMap.put("job_root_path", rootPath);
        String jobLossAndMetric = rpcClient.getJobLossAndMetric(jobKey, slurmAccount, rootPathMap.toString());
        Map<String, List> newJobLossAndMetric = objectMapper.readValue(jobLossAndMetric, new TypeReference<Map<String, List>>() {
        });
        // 3. 解析并组织数据
        Map<String, Object> jobData = WebSocket.parseJobData(experimentJob, newJobUsage, newJobSummary, newJobLossAndMetric);
        jobData.put("isLogFile", false);   // 本条数据不是日志文件
        return jobData;
    }

}
