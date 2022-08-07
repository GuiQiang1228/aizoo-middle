package aizoo.service;

import aizoo.Client;
import aizoo.common.GraphType;
import aizoo.domain.*;
import aizoo.grpcObject.mapper.GraphObjectEntityMapper;
import aizoo.grpcObject.mapper.JobObjectEntityMapper;
import aizoo.grpcObject.object.GraphObject;
import aizoo.grpcObject.object.JobObject;
import aizoo.repository.ServiceDAO;
import aizoo.utils.DAOUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Paths;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class TranslationService {
    private final static Logger logger = LoggerFactory.getLogger(TranslationService.class);

    @Autowired
    private Client client;

    @Autowired
    private DAOUtil daoUtil;

    @Value("${file.path}")
    String filePath;

    @Value("${save.path}")
    private String savePath;

    @Autowired
    ServiceDAO serviceDAO;

    /**
     * 初始化一个默认的Job
     * 配置环境参数和默认的描述参数
     *
     * @return 返回一个初始化的默认的Job
     * @throws
     */
    private static final Map<String, Object> DEFAULT_JOB = new HashMap<String, Object>() {{
        put("environment", new HashMap<String, Object>() {
                    {
                        put("args", new ArrayList<String>());
                        put("slurmKwargs", new ArrayList<String>());
                    }
                }
        );
        put("description", "");
    }};

    @Autowired
    ObjectMapper objectMapper;


    /**
     * 编译整个图用于编译下载功能
     * 这个图是个临时图，存放目录也与真正的编译并执行不一致
     * 他是放在compiled/{graphType}/graphID/uuid/下的，且文件名 为 {graphName}+datetime.py
     *
     * @param graph
     * @param job
     * @param username
     * @return 返回编译后的文件
     * @throws Exception
     */
    @Transactional(rollbackFor = Exception.class)
    public File compileGraphTemp(Graph graph, Job job, String username) throws Exception {
        logger.info("Start translate Graph");
        logger.info("translateGraph graph:{},job:{},username:{}", graph, job, username);

        // 获取图的类型的名称
        String type = graph.getGraphType().getName();
        
        // 获取图的类型
        GraphType graphType = graph.getGraphType();

        if (graphType == GraphType.COMPONENT) {
            type = graph.getComponent().getComponentType().getValue();
        }
        
        String dirPath = Paths.get(filePath, username, "compiled", type, String.valueOf(graph.getId()), UUID.randomUUID().toString()).toString();
        File dir = new File(dirPath);
        File codeFile = new File(dirPath, graph.getName() + "+" + new Date().getTime() + ".py");
        
        // 判断图类型，根据不同的图类型选择不同的编译的方式
        if (graphType == GraphType.COMPONENT) {
            translateComponent(graph, codeFile.getPath());
        } 
        else {
            if (graphType == GraphType.JOB) {
                translateExperiment(graph, job, codeFile.getPath());
            } else if (graphType == GraphType.SERVICE)
                translateService(graph, dirPath, null);
            else if (graphType == GraphType.APPLICATION) {
                translateApp(graph, dirPath);
                // 生成部分无用的文件夹，然后删除
                for (File file : Objects.requireNonNull(dir.listFiles()))
                    if (file.getName().equals("runtime_log") || file.getName().equals("results") || file.getName().equals("ml_model"))
                        file.delete();
            }
        }
        // 获取翻译文件
        File[] files = Objects.requireNonNull(dir.listFiles());
        logger.info("translateGraph return:{}", files[0]);
        logger.info("End translate Graph");
        return files[0];
    }

    /**
     * 将画好的 复合损失、组件、模型图 翻译为可执行python文件
     *
     * @param graph 图
     * @param path  代码保存的绝对路径(包含文件名)
     * @return String是保存翻译生成代码文件的路径
     * @throws JsonProcessingException
     */
    public String translateComponent(Graph graph, String path) {
        logger.info("Start translate Component");
        logger.info("translateComponent graph:{},path:{}", graph, path);
        // 类型转换
        GraphObject graphObject = GraphObjectEntityMapper.MAPPER.graph2GraphObject(graph, daoUtil);
        try {
            logger.info("objectMapper.writeValueAsString(graphObject):{}", objectMapper.writeValueAsString(graphObject));
            logger.info("translateComponent return:{}", client.interpretComponent(objectMapper.writeValueAsString(graphObject), path));
            logger.info("End translate Component");
            return client.interpretComponent(objectMapper.writeValueAsString(graphObject), path);
        } catch (JsonProcessingException e) {
            logger.error("translateComponent return:null");
            logger.error("End translate Component");
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * 将画好的实验图翻译为可执行python文件
     * @param graph 实验图
     * @param job
     * @param codePath 翻译代码文件的目录
     * @return Map<String, Object>: <{"save_path", 保存Job执行后的运行结果}, {"code_file_path_list", 图中各个组件模块的路径地址}>
     * @throws Exception
     */
    public Map<String, Object> translateExperiment(Graph graph, @Nullable Job job, String codePath) throws Exception {
        logger.info("Start translate Experiment");
        logger.info("translateExperiment graph:{},job:{},codePath:{}", graph, job, codePath);
        ExperimentJob experimentJob = (ExperimentJob) job;
        ObjectMapper objectMapper = new ObjectMapper();
        // 类型转换
        GraphObject graphObject = GraphObjectEntityMapper.MAPPER.graph2GraphObject(graph, daoUtil);
        String jobtoString = "";
        if (job != null) {
            JobObject jobObject = JobObjectEntityMapper.MAPPER.experimentJob2JobObject(experimentJob);
            jobtoString = objectMapper.writeValueAsString(jobObject);
        } else
            jobtoString = objectMapper.writeValueAsString(DEFAULT_JOB);
        // 调用Client类的interpretExperiment翻译实验图返回一个Map<String, Object>
        Map<String, Object> result = client.interpretExperiment(objectMapper.writeValueAsString(graphObject), jobtoString, savePath, codePath);
        logger.info("translateExperiment return:{}", result);
        logger.info("End translate Experiment");
        return result;
    }

    /**
     * 若方法没有传入参数job，则自动设参数job为null
     * 执行方法：自动将画好的实验图翻译为可执行python文件
     * @param graph 图
     * @param codePath 翻译代码文件的目录
     * @throws
     */
    public Map<String, Object> translateExperiment(Graph graph, String codePath) throws Exception {
        return translateExperiment(graph, null, codePath);
    }

    /**
     * 为图创建一个随机的Token并设置保存
     * @param graph 图
     * @throws
     */
    private void checkOrCreateToken(Graph graph) {
        aizoo.domain.Service service = graph.getService();
        // 若该图有服务且没有创建过Token时才创建一个Token
        if (service != null && service.getToken() == null) {
            String token = "";
            String str = "qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM1234567890";
            // 随机一个token字符串
            Random r = new Random();
            for (int i = 0; i < 6; i++)
                token = token + str.charAt(Math.abs(r.nextInt() % (str.length())));
            service.setToken(token);
            serviceDAO.save(service);
        }
    }

    /**
     * 将画好的服务图翻译为可执行python文件
     *
     * @param graph   服务图
     * @param codeDir 翻译代码的存放目录
     * @return paths数组: paths[0]是保存翻译生成代码文件的路径， paths[1]是保存作业执行日志文件的路径
     * @throws
     */
    public String[] translateService(Graph graph, String codeDir, @Nullable Integer port) throws Exception {
        logger.info("Start translate Service");
        // 为图创建一个Toke
        checkOrCreateToken(graph);
        logger.info("translateService graph:{},codeDir:{},port:{}", graph, codeDir, port);
        // 类型转换
        GraphObject graphObject = GraphObjectEntityMapper.MAPPER.graph2GraphObject(graph, daoUtil);
        // 若未设置端口则根据传入的参数设置端口
        if (port != null) 
            graphObject.setPort(port);
        logger.info("objectMapper.writeValueAsString(graphObject):{}", objectMapper.writeValueAsString(graphObject));
        String[] paths = client.interpretService(objectMapper.writeValueAsString(graphObject), codeDir);
        logger.info("End translate Service");
        return paths;
    }

    /**
     * 将画好的应用图翻译为可执行python文件
     * @param graph   图
     * @param job     应用
     * @param codeDir 保存翻译代码文件的目录
     * @return paths数组: paths[0]是保存翻译生成代码文件的路径, paths[1]是job的String对象
     * path[2]是保存作业执行日志文件的路径, path[4]是保存翻译代码文件的目录
     * @throws
     */
    public String[] translateApp(Graph graph, @Nullable Job job, String codeDir) throws Exception {
        logger.info("Start translate App");
        logger.info("translateService graph:{},codeDir:{},codeDir:{}", graph, job, codeDir);
        Application app = (Application) job;
        // 类型转换
        GraphObject graphObject = GraphObjectEntityMapper.MAPPER.graph2GraphObject(graph, daoUtil);
        String job2String = "";
        if (app != null) {
            JobObject jobObject = JobObjectEntityMapper.MAPPER.appplication2JobObject(app);
            job2String = objectMapper.writeValueAsString(jobObject);
        } else
            job2String = objectMapper.writeValueAsString(DEFAULT_JOB);
        logger.info("objectMapper.writeValueAsString(graphObject):{}", objectMapper.writeValueAsString(graphObject));
        //   翻译得到应用执行的代码
        String[] paths = client.interpretApplication(objectMapper.writeValueAsString(graphObject), job2String, savePath, codeDir);
        logger.info("End translate App");
        return paths;
    }

    /**
     * 若方法没有传入参数job，则自动设参数job为null
     * 执行方法：自动将画好的应用图翻译为可执行python文件
     * @param graph 应用图
     * @param codeDir 代码路径
     * @return paths数组: paths[0]是保存翻译生成代码文件的路径, paths[1]是job的String对象
     * path[2]是保存作业执行日志文件的路径, path[4]是保存翻译代码文件的目录
     * @throws
     */
    public String[] translateApp(Graph graph, String codeDir) throws Exception {
        translateApp(graph, null, codeDir);
        return null;
    }
}
