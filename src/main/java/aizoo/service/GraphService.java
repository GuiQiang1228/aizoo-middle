package aizoo.service;

import aizoo.common.*;
import aizoo.common.exception.ReleaseGraphException;
import aizoo.domain.*;
import aizoo.elasticObject.ElasticComponent;
import aizoo.elasticObject.ElasticGraph;
import aizoo.elasticObject.ElasticService;
import aizoo.elasticRepository.ComponentRepository;
import aizoo.elasticRepository.GraphRepository;
import aizoo.elasticRepository.ServiceRepository;
import aizoo.repository.*;
import aizoo.utils.*;
import aizoo.viewObject.mapper.*;
import aizoo.viewObject.object.*;
import aizoo.viewObject.pagebleUtils.VO2EntityMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.EnumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service("GraphService")
public class GraphService {

    @Autowired
    private GraphDAO graphDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private DatatypeDAO datatypeDAO;

    @Autowired
    private ServiceDAO serviceDAO;

    @Autowired
    private ComponentDAO componentDAO;

    @Autowired
    NamespaceDAO namespaceDAO;


    @Autowired
    DAOUtil daoUtil;

    @Autowired
    ApplicationDAO applicationDAO;

    @Autowired
    ComponentInputParameterDAO componentInputParameterDAO;

    @Autowired
    ComponentOutputParameterDAO componentOutputParameterDAO;

    @Autowired
    ServiceInputParameterDAO serviceInputParameterDAO;

    @Autowired
    ServiceOutputParameterDAO serviceOutputParameterDAO;

    @Autowired
    ExperimentJobDAO experimentJobDAO;

    @Autowired
    TranslationService translationService;

    @Value("${file.path}")
    String filePath;

    @Autowired
    ProjectService projectService;

    @Autowired
    DatasourceDAO datasourceDAO;

    @Autowired
    GraphRepository graphRepository;

    @Autowired
    ComponentRepository componentRepository;

    @Autowired
    ServiceRepository serviceRepository;

    private static final Logger logger = LoggerFactory.getLogger(GraphService.class);

    // ============================= 暂未用到，只在测试文件 ServiceTest.GraphServiceTest.java 中用到
    /**
     * 验证图名称是否存在
     *
     * @param name 待验证的图名称
     * @param type 图类型
     * @param userName 当前登录用户名
     * @return boolean类型，验证成功：true，验证失败：false
     */
    public boolean validGraphName(String name, String type, String userName) {
        logger.info("Start valid GraphName");
        logger.info("name: {}", name);
        logger.info("type: {}", type);
        logger.info("userName: {}", userName);
        if (EnumUtils.isValidEnum(ComponentType.class, type)) {
            logger.info("End valid GraphName");
            return componentDAO.existsByNameAndUserUsername(name, userName);
        } else if (EnumUtils.isValidEnum(GraphType.class, type)) {
            logger.info("End valid GraphName");
            return graphDAO.existsByNameAndUserUsername(name, userName);
        }
        logger.info("End valid GraphName");
        return false;
    }

    /**
     * 验证组件版本是否存在
     *
     * @param username 当前登录用户名称
     * @param name 组件名称
     * @param componentVersion 组件版本号
     * @return boolean类型，验证成功：true，验证失败：false
     */
    public boolean isValidComponentVersion(String username, String name, String componentVersion) {
        logger.info("Start Valid ComponentVersion");
        logger.info("username: {}", username);
        logger.info("name: {}", name);
        logger.info("componentVersion: {}", componentVersion);
        if (componentDAO.existsByComponentVersionAndNameAndUserUsername(componentVersion, name, username)) {
            logger.info("Fail to verification component version");
            logger.info("End Valid ComponentVersion");
            return false;
        }
        logger.info("Success to verification component version");
        logger.info("End Valid ComponentVersion");
        return true;
    }
    // =============================

    /**
     * 验证 type 是否属于 ComponentType or GraphType
     * 参数来自 aizoo.controller.GraphController.createGraph
     *
     * @param type 传入的type类型
     * @return True : type 属于 ComponentType or GraphType (否则返回 False )
     */
    public boolean isValidGraph(String type) {
        logger.info("Start isValidGraph");
        logger.info("type: {}", type);
        if (EnumUtils.isValidEnum(ComponentType.class, type)) {
            logger.info("End isValidGraph");
            return true;
        } else if (EnumUtils.isValidEnum(GraphType.class, type)) {
            logger.info("End isValidGraph");
            return true;
        }
        logger.info("End isValidGraph");
        return false;
    }

    /**
     * 将 componentVO 调整组织为 component
     * 将输入输出的originName同步为name，使originName和修改后的name一致
     * 为每一个发布的复合组件output都加一个输出为self
     * 参数来自：aizoo.controller.GraphController.compileGraph
     *
     * @param componentVO 传入的 ComponentVO 数据结构
     * @return Component 调整组织后的复合组件
     */
    public Component componentCompilePrepare(ComponentVO componentVO) {
        // 对于复合组件，它的输入输出由子组件继承而来，所以可能出现改名的情况。
        // 它的originName应该保证跟改名后的name是一致的，作为第一次组件保存的输入输出的端点名
        logger.info("Start prepare ComponentCompile");
        logger.info("componentVO: {}", componentVO.toString());
        // 将输入输出的originName与name同步
        for (Map<String, Object> input : componentVO.getInputs())
            input.put("originName", input.get("name"));
        for (Map<String, Object> output : componentVO.getOutputs())
            output.put("originName", output.get("name"));

        // 保存该复合组件为component
        Component component = ComponentVOEntityMapper.MAPPER.componentVO2Component(componentVO, datatypeDAO, componentDAO, namespaceDAO, userDAO);

        // 给每个发布的复合组件的output都加一个输出为self
        ComponentOutputParameter selfOutput = new ComponentOutputParameter();
        selfOutput.setIsSelf(true);
        logger.info("testStart/n");
        // ====
        Datatype datatype = datatypeDAO.findByName(AizooConstans.AIZOO_UNKNOWN);
        // Datatype datatype = datatypeDAO.findById(Long.valueOf(8)).orElseThrow(() -> new EntityNotFoundException());
        // ====
        logger.info("dataType: {}", datatype);
        selfOutput.setParameter(new Parameter(AizooConstans.SELF_NAME, AizooConstans.SELF_TITLE, AizooConstans.SELF_DESCRIPTION, AizooConstans.SELF_ORIGIN_NAME, datatype));
        logger.info("testFinish");
        selfOutput.setComponent(component);
        component.getOutputs().add(selfOutput);
        logger.info("完成准备的组件: {}", component);
        logger.info("End prepare ComponentCompile");
        return component;
    }

    /**
     * 保存复合组件
     * 参数由 aizoo.controller.GraphController.releaseComponent 传入
     *
     * @param graphVO 图数据结构，具体见 aizoo\viewObject\object\GraphVO.java
     * @param componentVO 组件数据结构，具体见 aizoo\viewObject\object\ComponentVO.java
     */
    @Transactional
    public void release2ComponentAndGraph(GraphVO graphVO, ComponentVO componentVO) {
        // 保存该复合组件为component
        logger.info("Start release2ComponentAndGraph");
        logger.info("graphVO: {}", graphVO.toString());
        logger.info("componentVO: {}", componentVO.toString());
        Component component = ComponentVOEntityMapper.MAPPER.componentVO2Component(componentVO, datatypeDAO, componentDAO, namespaceDAO, userDAO);

        // 由新建图时为该组件选择的namespace生成存放目录
        Path componentDir = Paths.get(filePath, component.getNamespace().getNamespace().split("\\."));

        // 转换版本号 1.0.0 → 1_0_0
        String componentVersion = component.getComponentVersion();
        if (componentVersion != null) {
            componentVersion = component.getComponentVersion().replace(".", "_");
        }

        // 组件设置路径名称：根目录名称 + 组件名 + 组件版本号 + .py后缀
        Path componentFilePath = Paths.get(componentDir.toString(), component.getName() + componentVersion + ".py");
        File componentFile = new File(componentFilePath.toString());
        component.setPath(componentFile.getAbsolutePath().replace("\\", "/"));

        // fileNameAndDirMap : <文件名，存放路径>，最终转化成json字符串存入component的fileList属性中
        // 文件名格式：组件名 + 组件版本号 + .py
        // 存放路径格式：根目录名称 + 组件名 + 组件版本号 + .py后缀
        // 根目录名称： filePath变量
        HashMap<String, String> fileNameAndDirMap = new HashMap<>();
        // childComponentIdList : 存放复合组件的下一层子组件id，最终转换成json字符串存入component的childComponentIdList属性中
        Set<Long> childComponentIdList = new HashSet<>();

        // 将组成复合组件的所有的子组件以及自己的路径存放在复合组件的importList中,用于翻译器来翻译import部分
        // 更新组件的复合组件的fileNameAndDirMap，其包含组件自身的路径以及所有内层子组件的路径
        ObjectMapper objectMapper = new ObjectMapper();
        // 添加原子组件的存放路径和文件名
        for (Node node : graphVO.getNodeList()) {
            if (node.getComponentType() != NodeType.DATASOURCE
                    && node.getComponentType() != NodeType.OPERATOR
                    && node.getComponentType() != NodeType.OPERATOR_TEMPLATE
                    && node.getComponentType() != NodeType.PARAMETER
            ) {
                Long id = node.getComponentVO().getId();
                Component nodeComponent = componentDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
                try {
                    String nodeComponentVersion = "";
                    if (nodeComponent.getComponentVersion() != null) {
                        nodeComponentVersion = nodeComponent.getComponentVersion().replace(".", "_");
                    }
                    // 若原子组件的fileList为空，fileNameAndDirMap添加原子组件自身的存放路径和文件名
                    if (nodeComponent.getFileList() == null) {
                        fileNameAndDirMap.put(nodeComponent.getName() + nodeComponentVersion + ".py", nodeComponent.getPath());
                    } else { // 若原子组件的fileList不为空，fileNameAndDirMap添加fileList中所有python代码文件的存放路径和文件名
                        fileNameAndDirMap.putAll(objectMapper.readValue(nodeComponent.getFileList(), new TypeReference<Map<String, String>>() {}));
                    }
                    // 添加复合组件的子组件id
                    childComponentIdList.add(nodeComponent.getId());
                } catch (Exception e) {
                    logger.error("添加原子组件存放路径和路径名失败,id: {},错误信息:{}", id,e);
                    e.printStackTrace();
                }
            }
        }
        // 添加复合组件本身的文件名和存放路径
        // component.getPath() 格式为：目录名称 + 组件名 + 组件版本号 + .py后缀
        fileNameAndDirMap.put(component.getName() + componentVersion + ".py", component.getPath());
        try {
            // 将 fileNameAndDirMap 对象变成json字符串存入component实体的fileList属性中
            component.setFileList(objectMapper.writeValueAsString(fileNameAndDirMap));
            // 将 childComponentIdList 对象变成json字符串存入component实体的childComponentIdList属性中
            component.setChildComponentIdList(objectMapper.writeValueAsString(childComponentIdList));
        } catch (JsonProcessingException e) {
            logger.error("添加复合组件本身的文件名和存放路径失败,错误信息:", e);
            e.printStackTrace();
        }
        logger.info("发布组件的保存路径: {} ", componentFile.getAbsolutePath());

        // 给每个发布的复合组件的output都加一个输出为self
        ComponentOutputParameter selfOutput = new ComponentOutputParameter();
        selfOutput.setIsSelf(true);
        // ====
//        Datatype datatype = datatypeDAO.findByName(AizooConstans.AIZOO_UNKNOWN);
         Datatype datatype = datatypeDAO.findById(AizooConstans.AIZOO_UNKNOWN_ID).orElseThrow(() -> new EntityNotFoundException());
        // ====
        logger.info("dataType: {}", datatype);
        selfOutput.setParameter(new Parameter(AizooConstans.SELF_NAME, AizooConstans.SELF_TITLE, AizooConstans.SELF_DESCRIPTION, AizooConstans.SELF_ORIGIN_NAME, datatype));
        selfOutput.setComponent(component);
        component.getOutputs().add(selfOutput);
        component.setReleased(true);
        componentDAO.save(component);

        // 保存该复合组件对应的图，更新graph对应的component
        Graph graph = GraphVOEntityMapper.MAPPER.graphVO2Graph(graphVO, graphDAO, componentDAO, daoUtil, datatypeDAO);
        graph.setGraphVersion(component.getComponentVersion());
        graph.setReleased(true);
        graph.setComponent(componentDAO.findById(component.getId()).orElseThrow(() -> new EntityNotFoundException(String.valueOf(component.getId()))));
        graphDAO.save(graph);

        // 翻译得到组件图的代码
        translationService.translateComponent(graph, componentFile.getAbsolutePath());
        logger.info("End release2ComponentAndGraph");
    }

    /**
     * 发布新版本的图
     * 如果图类型为service，则将对应的service也发布
     * 参数来自：aizoo.controller.ApplicationController.applicationExecute、
     * aizoo.controller.ExperimentController.experimentExecute
     * aizoo.controller.ServiceController.serviceExecute
     *
     * @param graphVO 传入的 GraphVO 类型数据结构
     * @throws Exception
     */
    @Transactional
    public void releaseGraphVersion(GraphVO graphVO) throws Exception {
        logger.info("Start release GraphVersion");
        logger.info("graphVO: {}", graphVO.toString());
        try {
            Graph graph = graphDAO.findById(graphVO.getId()).orElseThrow(() -> new EntityNotFoundException(String.valueOf(graphVO.getId())));
            if (graph.isReleased())  // 图已经发布，则略过该环节
                return;
            graph.setReleased(true);
            graph.setGraphVersion(graphVO.getGraphVersion());
            // 如果图类型为service，则将对应的service也发布
            if (graph.getGraphType() == GraphType.SERVICE) {
                aizoo.domain.Service service = graph.getService();
                service.setServiceVersion(graph.getGraphVersion());
                service.setReleased(true);
                serviceDAO.save(service);
            }
            graphDAO.save(graph);
            logger.info("End release GraphVersion");
        } catch (Exception e) {
            logger.error("Fail to release GraphVersion,graphVO: {},错误信息:{}", graphVO.toString(),e);
            throw new ReleaseGraphException();
        }
    }

    /**
     * 通过类型获取图列表分页信息
     * 参数由 aizoo.controller.GraphController.getGraphListByType 传入
     * Graph 分为 COMPONENT、SERVICE、JOB、APPLICATION
     * COMPONENT 分为 DATASET、MODULE、MODEL、LOSS
     *
     * @param pageNum 当前页码，默认为0
     * @param pageSize 每页大小，默认为10
     * @param username 当前登录的用户名称
     * @param type 请求类型
     * @return Page<GraphVO> 所请求图列表的分页信息
     */
    public Page<GraphVO> getGraphListByType(Integer pageNum, Integer pageSize, String username, String type) {
        logger.info("Start get GraphList");
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.Direction.DESC, "updateTime");
        logger.info("PageRequest pageNum: {},pageSize: {} ", pageNum, pageSize);
        logger.info("username: {}, type: {}", username, type);
        Page<Graph> graphsPage;
        // 获取 Graph数据 或者 Component数据
        if (EnumUtils.isValidEnum(ComponentType.class, type)) {
            graphsPage = graphDAO.findByComponentComponentTypeAndUserUsername(ComponentType.valueOf(type), username, pageable);
        } else {
            graphsPage = graphDAO.findAllByUserUsernameAndGraphType(username, GraphType.valueOf(type), pageable);
        }
        logger.info("End get GraphList");
        return VO2EntityMapper.mapEntityPage2VOPage(GraphVOEntityMapper.MAPPER::graph2GraphVO, graphsPage);
    }

    /**
     * 创建一张图
     * 参数来自 aizoo.controller.GraphController.createGraph
     *
     * @param graphName 传入的图名称
     * @param type 传入的类型
     * @param namespace 传入的命名空间
     * @param username 传入的当前用户名称
     * @param privacy 传入的权限
     * @param desc 传入的图描述
     * @return GraphVO 具体数据结构见aizoo.viewObject.object.GraphVO
     */
    @Transactional
    public GraphVO createGraph(String graphName, String type, @Nullable String namespace, String username, String privacy, String desc) {
        logger.info("Start create Graph");
        // 通过用户名从数据库获取用户所有信息
        User user = userDAO.findByUsername(username);
        logger.info("graphName: {}, type: {} ", graphName, type);
        logger.info("findByNamespace namespace: {}", namespace);
        logger.info("findByUsername: {}", username);
        logger.info("privacy: {},desc: {}", privacy, desc);

        // 通过图名称创建一个图对象g
        Graph g = new Graph(graphName);
        // 如果 type 属于 GraphType
        if (EnumUtils.isValidEnum(GraphType.class, type)) {
            if (GraphType.valueOf(type) == GraphType.JOB) {
                g.setGraphType(GraphType.JOB);
            }
            if (GraphType.valueOf(type) == GraphType.SERVICE) {
                Namespace namespaces = namespaceDAO.findByNamespace(namespace);
                g.setGraphType(GraphType.SERVICE);
                aizoo.domain.Service service = new aizoo.domain.Service();
                service.setGraph(g);
                service.setName(graphName);
                service.setUser(user);
                service.setPrivacy(privacy);
                service.setNamespace(namespaces);
                service.setDescription(desc);
                serviceDAO.save(service);
                g.setService(service);
            }
            if (GraphType.valueOf(type) == GraphType.APPLICATION) {
                g.setGraphType(GraphType.APPLICATION);
            }
        } else if (EnumUtils.isValidEnum(ComponentType.class, type)) { // 如果 type 属于 ComponentType
            Namespace namespaces = namespaceDAO.findByNamespace(namespace);
            g.setGraphType(GraphType.COMPONENT);
            Component component = new Component();
            component.setGraph(g);
            component.setComposed(true);
            component.setName(graphName);
            component.setComponentType(ComponentType.valueOf(type));
            component.setUser(user);
            component.setPrivacy(privacy);
            component.setNamespace(namespaces);
            component.setDescription(desc);
            componentDAO.save(component);
            g.setComponent(component);
        }
        g.setUser(user);
        g.setDescription(desc);
        g.setGraphKey(type + "-" + UUID.randomUUID().toString());
        // =========
        g.setNodeList(new ArrayList<>().toString());
        g.setLinkList(new ArrayList<>().toString());
        // =========
        graphDAO.save(g);
        logger.info("created Graph: {}", GraphVOEntityMapper.MAPPER.graph2GraphVO(g).toString());
        logger.info("End create Graph");
        return GraphVOEntityMapper.MAPPER.graph2GraphVO(g);
    }

    /**
     * 通过 graphId 获取 graph信息，并复用
     * namespace、privacy信息不复用，初始化为null
     * 参数来自 aizoo.controller.GraphController.reuseGraph
     *
     * @param graphId 待复用的图id
     * @param username 当前登录用户名
     * @return Long类型，新建graph的id
     * @throws RuntimeException
     */
    @Transactional
    public Long reuseGraph(String graphId, String username) throws RuntimeException {
        logger.info("Start reuse Graph");
        logger.info("graphId: {},username: {}", graphId, username);
        if (graphId == null) {
            throw new RuntimeException();
        } else {
//            long id = Long.valueOf(graphId).longValue();
            long id = Long.parseLong(graphId);
            Graph graph = graphDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
            String namespace = null;
            String privacy = null;
            String desc = graph.getDescription();
            String type = graph.getGraphType().toString();
            String graphName = graph.getName();
            if (graph.getGraphType() == GraphType.COMPONENT) {
                Component component = graph.getComponent();
                type = component.getComponentType().toString();
                namespace = component.getNamespace().getNamespace();
                privacy = component.getPrivacy();
            } else if (graph.getGraphType() == GraphType.SERVICE) {
                aizoo.domain.Service service = graph.getService();
                namespace = service.getNamespace().getNamespace();
                privacy = service.getPrivacy();
            }
            // 通过复用，新建一个图对象
            GraphVO g = createGraph(graphName, type, namespace, username, privacy, desc);
            Long newGraphId = g.getId();

            Graph newGraph = graphDAO.findById(newGraphId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(newGraphId)));
            newGraph.setLinkList(graph.getLinkList());
            newGraph.setNodeList(graph.getNodeList());
            newGraph.setOriginJson(graph.getOriginJson());
            graphDAO.save(newGraph);
            logger.info("生成新图的ID newGraphId: {}", newGraphId);
            logger.info("End reuse Graph");
            return newGraphId;
        }
    }

    /**
     * 根据图id删除任务 or 组件 or 模型
     * 参数来自 aizoo.controller.GraphController.deleteDesignGraph
     *
     * @param id 传入的id
     * @param username 传入的当前登录用户名
     * @throws Exception
     */
    @Transactional
    public void deleteGraphById(long id, String username) throws Exception {
        logger.info("Start delete Graph");
        logger.info("id: {},username: {}", id, username);
        Graph graph = graphDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
        String graphType = null;
        if (graph.getGraphType() == GraphType.JOB) {
            // 只要job有任何关联，都是不允许删除的
            graphType = PictureType.JOB.getValue();
        } else if (graph.getGraphType() == GraphType.COMPONENT) {
            Component component = graph.getComponent();
            // 若该图对应的组件已发布则删除对应的文件
            if (component.isReleased()) {
                FileUtil.deleteFile(new File(component.getPath()));
            }
            // 删除component对应的input，output
            List<ComponentInputParameter> componentInputParameters = component.getInputs();
            for (ComponentInputParameter componentInputParameter : componentInputParameters) {
                componentInputParameter.setComponent(null);
                componentInputParameterDAO.delete(componentInputParameter);
            }
            List<ComponentOutputParameter> componentOutputParameters = component.getOutputs();
            for (ComponentOutputParameter componentOutputParameter : componentOutputParameters) {
                componentOutputParameter.setComponent(null);
                componentOutputParameterDAO.delete(componentOutputParameter);
            }
            graph.setComponent(null);
            component.setNamespace(null);
            component.setUser(null);

            // 解除job与模型的绑定关系
            if (component.getComponentType() == ComponentType.MODEL) {
                List<ExperimentJob> jobs = experimentJobDAO.findByComponentId(component.getId());
                if (jobs != null)
                    jobs.stream().forEach(job -> {
                        job.setComponent(null);
                        experimentJobDAO.save(job);
                    });
            }

            // 解除被fork的外键关联
            List<Component> componentsForkedBy = component.getForkBy();
            if (componentsForkedBy != null) {
                componentsForkedBy.stream().forEach(c -> {
                    c.setForkFrom(null);
                    componentDAO.save(c);
                });
            }
            List<Project> projects = component.getProjects();
            List<Long[]> removeList = new ArrayList<>();
            for (Project project : projects) {
                removeList.add(new Long[]{project.getId(), component.getId()});
            }
            projectService.removeProjectComponentRelation(removeList);
            componentDAO.delete(component);

            //删除component对应es索引
            Optional<ElasticComponent> optional = componentRepository.findById(component.getId().toString());
            if(optional.isPresent())
                componentRepository.delete(optional.get());
            graphType = component.getComponentType().getValue();
        } else if (graph.getGraphType() == GraphType.SERVICE) {
            aizoo.domain.Service service = graph.getService();
            if (service.isReleased()) {
                FileUtil.deleteFile(new File(service.getPath()));
            }
            List<ServiceInputParameter> serviceInputParameters = service.getInputs();
            for (ServiceInputParameter serviceInputParameter : serviceInputParameters) {
                serviceInputParameter.setService(null);
                serviceInputParameterDAO.delete(serviceInputParameter);
            }
            List<ServiceOutputParameter> serviceOutputParameters = service.getOutputs();
            for (ServiceOutputParameter serviceOutputParameter : serviceOutputParameters) {
                serviceOutputParameter.setService(null);
                serviceOutputParameterDAO.delete(serviceOutputParameter);
            }
            graph.setComponent(null);
            graph.setService(null);
            service.setNamespace(null);
            service.setUser(null);
            service.setGraph(null);
            List<Project> projects = service.getProjects();
            List<Long[]> removeList = new ArrayList<>();
            for (Project project : projects) {
                removeList.add(new Long[]{project.getId(), service.getId()});
            }
            projectService.removeProjectServiceRelation(removeList);
            serviceDAO.delete(service);

            //删除service对应es索引
            Optional<ElasticService> optional = serviceRepository.findById(service.getId().toString());
            if(optional.isPresent())
                serviceRepository.delete(optional.get());
            graphType = PictureType.SERVICE.getValue();
        } else if (graph.getGraphType() == GraphType.APPLICATION) {
            graphType = PictureType.APPLICATION.getValue();
        }

        // 删除对应的图片
        Path path = Paths.get(filePath, username, "picture", graphType, graph.getId() + "_" + graph.getName() + ".png");
        File pictureFile = new File(path.toString());
        FileUtil.deleteFile(pictureFile);

        // 获取和待删除的graph相关的projectId，用removeList暂存，然后统一删除
        List<Project> projects = graph.getProjects();
        List<Long[]> removeList = new ArrayList<>();
        for (Project project : projects) {
            removeList.add(new Long[]{project.getId(), graph.getId()});
        }
        projectService.removeProjectGraphRelation(removeList);
        graphDAO.delete(graph);

        //删除graph对应es索引
        Optional<ElasticGraph> optional = graphRepository.findById(graph.getId().toString());
        if(optional.isPresent())
            graphRepository.delete(optional.get());
        logger.info("End delete Graph");
    }

    /**
     * 通过图id获取图数据
     * 参数来自 aizoo.controller.GraphController.getGraphByGraphId
     *
     * @param graphId 图id
     * @return map(component类型), key: graph、namespace、componentId
     * @return map(service类型), key: graph、namespace、serviceId
     * @return map(job or application类型), key: graph
     */
    public Map<String, Object> openGraph(Long graphId) {
        logger.info("Start open Graph");
        // 通过图id从数据库中查出图数据
        Graph graph = graphDAO.findById(graphId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(graphId)));
        logger.info("findById graphId: {}", graphId);

        // ================== map中的key对应的值，有的设为null，有的什么都没有设
        // map对象用于组织返回结果
        // map内容：graph、namespace、componentId、serviceId
        HashMap<String, Object> map = new HashMap<>();
        map.put("graph", GraphVOEntityMapper.MAPPER.graph2GraphVO(graph));
        // 如果 graph.getGraphType 为 COMPONENT("component", "组件")
        if (graph.getGraphType() == GraphType.COMPONENT) {
            map.put("namespace", NamespaceVOEntityMapper.MAPPER.namespace2NamespaceVO(graph.getComponent().getNamespace()));
            map.put("componentId", graph.getComponent().getId());
            return map;
        }
        // 如果 graph.getGraphType 为 SERVICE("service","服务")
        if (graph.getGraphType() == GraphType.SERVICE) {
            map.put("namespace", NamespaceVOEntityMapper.MAPPER.namespace2NamespaceVO(graph.getService().getNamespace()));
            map.put("componentId", null);
            map.put("serviceId", graph.getService().getId());
            return map;
        }
        // graph.getGraphType 的 其余类型 JOB("job", "实验")、APPLICATION("application","应用")
        map.put("namespace", null);
        map.put("componentId", null);
        map.put("serviceId", null);
        // ====================

        logger.info("打开的图的信息: {}", map.toString());
        logger.info("End open Graph");
        return map;
    }

    /**
     * 根据图id下载截图
     * 参数来自 aizoo.controller.GraphController.downloadAllGraphPicStream
     *
     * @param graphId 图id
     * @param username 需要查看缩略图的用户名（不是当前登录的用户名）
     * @return ResponseEntity<byte[]> 图片字节流信息
     * @throws IOException
     */
    @Transactional
    public ResponseEntity<byte[]> downloadAllGraphPicStream(long graphId, String username) throws IOException {
        Optional<Graph> graphOptional = graphDAO.findById(graphId);
        if (!graphOptional.isPresent())
            return null;
        Graph graph = graphOptional.get();
        String pictureSavePath = "";
        GraphType graphType = graph.getGraphType();
        // 实验图下载缩略图
        // 截图的下载地址为 filePath/user/picture/graphType/graphId_graphName.png
        if (graphType == GraphType.JOB) { // 实验图下载缩略图
            pictureSavePath = Paths.get(filePath, username, "picture",
                    PictureType.JOB.getValue(), graph.getId() + "_" + graph.getName() + ".png").toString();
        } else if (graphType == GraphType.SERVICE) { // 服务图下载缩略图
            pictureSavePath = Paths.get(filePath, username, "picture",
                    PictureType.SERVICE.getValue(), graph.getId() + "_" + graph.getName() + ".png").toString();
        } else if (graphType == GraphType.APPLICATION) { // 应用图下载缩略图
            pictureSavePath = Paths.get(filePath, username, "picture",
                    PictureType.APPLICATION.getValue(), graph.getId() + "_" + graph.getName() + ".png").toString();
        } else { // 复合组件缩略图
            Component component = graph.getComponent();
            // 截图的下载地址为 filePath/user/picture/graphType/graphId_graphNameComponentVersion.png
            pictureSavePath = Paths.get(filePath, username, "picture",
                    component.getComponentType().getValue(), graph.getId() + "_" + graph.getName() + ".png").toString();
        }
        File file = new File(pictureSavePath);
        //http头部
        HttpHeaders httpHeaders = new HttpHeaders();
        //application/octet-stream ： 二进制流数据
        httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        if (!file.exists())
            return new ResponseEntity<>(new byte[]{}, httpHeaders, HttpStatus.CREATED);
        return new ResponseEntity<>(FileUtils.readFileToByteArray(file), httpHeaders, HttpStatus.CREATED);
    }

    /**
     * 根据图id修改对应的命名空间
     * component、service两种类型分开修改
     * 参数来自 aizoo.controller.GraphController.modifyGraphNamespace
     *
     * @param id 图id
     * @param namespace 待修改的命名空间
     */
    @Transactional
    public void modifyGraphNamespace(long id, String namespace) {
        logger.info("Start modify GraphNamespace");
        Graph graph = graphDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
        logger.info("findById id: {}", id);
        logger.info("findByNamespace namespace: {}", namespace);
        if (graph.getGraphType().equals(GraphType.COMPONENT)) {
            Component component = graph.getComponent();
            Namespace namespaces = namespaceDAO.findByNamespace(namespace);
            component.setNamespace(namespaces);
            componentDAO.save(component);
        } else if (graph.getGraphType().equals(GraphType.SERVICE)) {
            aizoo.domain.Service service = graph.getService();
            Namespace namespaces = namespaceDAO.findByNamespace(namespace);
            service.setNamespace(namespaces);
            serviceDAO.save(service);
        }
        graphDAO.save(graph);
        logger.info("End modify GraphNamespace");
    }

    /**
     * 根据图id修改对应的图名称
     * component、service两种类型分开修改
     * 参数来自 aizoo.controller.GraphController.modifyGraphNamespace
     *
     * @param id 图id
     * @param name 待修改的图名称
     */
    @Transactional
    public void modifyGraphName(long id, String name) {
        logger.info("Start modify GraphName");
        logger.info("findById id: {}", id);
        logger.info("setName name: {}", name);
        Graph graph = graphDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
        graph.setName(name);
        if (graph.getGraphType().equals(GraphType.COMPONENT)) {
            Component component = graph.getComponent();
            component.setName(name);
            componentDAO.save(component);
        } else if (graph.getGraphType().equals(GraphType.SERVICE)) {
            aizoo.domain.Service service = graph.getService();
            service.setName(name);
            serviceDAO.save(service);
        }
        graphDAO.save(graph);
        logger.info("End modify GraphName");
    }

    /**
     * 递归检查graph的子节点是否都存在
     *
     * @param id
     * @return 双层Map，第一层的string共有三种类型：component, service, datasource，前两个可能会有嵌套情况出现，第二层为<objectId, object>形式，
     * 将所有不存在的object添加进来返回给前端，采用这种形式主要是为了去重
     */
    public HashMap<String, HashMap<Long, Object>> checkGraph(long id) {
        Graph graph = graphDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
        GraphVO graphVO = GraphVOEntityMapper.MAPPER.graph2GraphVO(graph);
        //将graph转换为VO之后再拿取NodeList
        List<Node> nodeList = graphVO.getNodeList();
        //初始化返回结果
        HashMap<String, HashMap<Long, Object>> object = new HashMap<>();
        object.put("component", new HashMap<>());
        object.put("service", new HashMap<>());
        object.put("datasource", new HashMap<>());
        //遍历所有子节点
        for (Node node : nodeList) {
            ComponentVO componentVO = node.getComponentVO();
            ServiceVO serviceVO = node.getServiceVO();
            DatasourceVO datasourceVO = node.getDatasourceVO();
            //事实上这三种只会有一种有值，其他两种为null
            if (componentVO != null) {
                Optional<Component> optional = componentDAO.findById(componentVO.getId());
                //若该node为component类型，判断其是否存在
                if (!optional.isPresent()) {  //如果不存在，并且目前添加进的内容中没有它，将其添加进map
                    if (!object.get("component").keySet().contains(componentVO.getId()))
                        object.get("component").put(componentVO.getId(), componentVO);
                } else {
                    Component component = optional.get();
                    if (component.getGraph() != null) {  //如果存在嵌套情况，递归进行检查
                        HashMap<Long, Object> hashMapComponent = checkGraph(component.getGraph().getId()).get("component");  //获取递归得到的返回结果
                        for (Long key : hashMapComponent.keySet()) {   //依次判断，如果当前添加进的内容中没有它，将其添加进map
                            if (!object.get("component").keySet().contains(key))
                                object.get("component").put(key, hashMapComponent.get(key));
                        }
                        HashMap<Long, Object> hashMapService = checkGraph(component.getGraph().getId()).get("service");
                        for (Long key : hashMapService.keySet()) {
                            if (!object.get("service").keySet().contains(key))
                                object.get("service").put(key, hashMapService.get(key));
                        }
                    }
                }
            }
            if (serviceVO != null) {
                Optional<aizoo.domain.Service> optional = serviceDAO.findById(serviceVO.getId());
                if (!optional.isPresent()) {  //service类型同上
                    if (!object.get("service").keySet().contains(serviceVO.getId()))
                        object.get("service").put(serviceVO.getId(), serviceVO);
                } else {
                    aizoo.domain.Service service = optional.get();
                    if (service.getGraph() != null) {
                        HashMap<Long, Object> hashMapComponent = checkGraph(service.getGraph().getId()).get("component");
                        for (Long key : hashMapComponent.keySet()) {
                            if (!object.get("component").keySet().contains(key))
                                object.get("component").put(key, hashMapComponent.get(key));
                        }
                        HashMap<Long, Object> hashMapService = checkGraph(service.getGraph().getId()).get("service");
                        for (Long key : hashMapService.keySet()) {
                            if (!object.get("service").keySet().contains(key))
                                object.get("service").put(key, hashMapService.get(key));
                        }
                    }
                }
            }
            if (datasourceVO != null) {
                Optional<Datasource> optional = datasourceDAO.findById(datasourceVO.getId());
                if (!optional.isPresent()) {
                    if (!object.get("datasource").keySet().contains(datasourceVO.getId()))
                        object.get("datasource").put(datasourceVO.getId(), datasourceVO);
                }
            }
        }
        return object;
    }

    /**
     * 检查图是否存在
     *
     * @param id 图id
     *
     * @return true:存在   false:不存在
     */
    @Transactional
    public boolean checkGraphExist(long id) {
        Optional<Graph> optional = graphDAO.findById(id);
        if(!optional.isPresent())
            return false;
        return true;
    }

    /**
     * 修改图描述，同时修改对应component/service的描述
     *
     * @param id  图id
     * @param description  需修改描述的内容
     */
    public void modifyDescription(long id, String description){
        Graph graph = graphDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
        graph.setDescription(description);
        // 保存修改描述后的图
        graphDAO.save(graph);
        if(graph.getComponent() != null){
            Component component = graph.getComponent();
            component.setDescription(description);
            componentDAO.save(component);
        }
        else if(graph.getService() != null){
            aizoo.domain.Service service = graph.getService();
            service.setDescription(description);
            serviceDAO.save(service);
        }
    }

    /**
     * 图点击保存时重新保存一遍fileList
     *
     * @param graphVO graph对应的VO
     * @throws JsonProcessingException
     */
    public void saveFileList(GraphVO graphVO) throws JsonProcessingException {
        Graph graph = graphDAO.findById(graphVO.getId()).orElseThrow(()->new EntityNotFoundException(String.valueOf(graphVO.getId())));
        if(graphVO.getGraphType().equals(GraphType.COMPONENT)){
            Component component = graph.getComponent();
            // fileNameAndDirMap : <文件名，存放路径>，最终转化成json字符串存入component的fileList属性中
            // 文件名格式：组件名 + 组件版本号 + .py
            // 存放路径格式：根目录名称 + 组件名 + 组件版本号 + .py后缀
            // 根目录名称： filePath变量
            HashMap<String, String> fileNameAndDirMap = new HashMap<>();
            // childComponentIdList : 存放复合组件的下一层子组件id，最终转换成json字符串存入component的childComponentIdList属性中
            Set<Long> childComponentIdList = new HashSet<>();

            // 将组成复合组件的所有的子组件以及自己的路径存放在复合组件的importList中,用于翻译器来翻译import部分
            // 更新组件的复合组件的fileNameAndDirMap，其包含组件自身的路径以及所有内层子组件的路径
            ObjectMapper objectMapper = new ObjectMapper();
            // 添加原子组件的存放路径和文件名
            for (Node node : graphVO.getNodeList()) {
                if (node.getComponentType() != NodeType.DATASOURCE
                        && node.getComponentType() != NodeType.OPERATOR
                        && node.getComponentType() != NodeType.OPERATOR_TEMPLATE
                        && node.getComponentType() != NodeType.PARAMETER
                ) {
                    Long id = node.getComponentVO().getId();
                    Component nodeComponent = componentDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
                    try {
                        String nodeComponentVersion = "";
                        if (nodeComponent.getComponentVersion() != null) {
                            nodeComponentVersion = nodeComponent.getComponentVersion().replace(".", "_");
                        }
                        // 若原子组件的fileList为空，fileNameAndDirMap添加原子组件自身的存放路径和文件名
                        if (nodeComponent.getFileList() == null) {
                            fileNameAndDirMap.put(nodeComponent.getName() + nodeComponentVersion + ".py", nodeComponent.getPath());
                        } else { // 若原子组件的fileList不为空，fileNameAndDirMap添加fileList中所有python代码文件的存放路径和文件名
                            fileNameAndDirMap.putAll(objectMapper.readValue(nodeComponent.getFileList(), new TypeReference<Map<String, String>>() {}));
                        }
                        // 添加复合组件的子组件id
                        childComponentIdList.add(nodeComponent.getId());
                    } catch (Exception e) {
                        logger.error("添加原子组件存放路径和路径名失败,id: {},错误信息:{}", id,e);
                        e.printStackTrace();
                    }
                }
            }
            String componentVersion = component.getComponentVersion();
            // 添加复合组件本身的文件名和存放路径
            // component.getPath() 格式为：目录名称 + 组件名 + 组件版本号 + .py后缀
            fileNameAndDirMap.put(component.getName() + componentVersion + ".py", component.getPath());
            try {
                // 将 fileNameAndDirMap 对象变成json字符串存入component实体的fileList属性中
                component.setFileList(objectMapper.writeValueAsString(fileNameAndDirMap));
                // 将 childComponentIdList 对象变成json字符串存入component实体的childComponentIdList属性中
                component.setChildComponentIdList(objectMapper.writeValueAsString(childComponentIdList));
                componentDAO.save(component);
            } catch (JsonProcessingException e) {
                logger.error("保存fileList和childIdList失败,错误信息:{}", e);
                e.printStackTrace();
            }
        }
        else if(graphVO.getGraphType().equals(GraphType.SERVICE)){
            aizoo.domain.Service service = graph.getService();
            HashMap<String, String> fileListMap = new HashMap<>();
            ObjectMapper objectMapper = new ObjectMapper();
            for (Node node : graphVO.getNodeList()) {
                if (node.getComponentType() != NodeType.PARAMETER && node.getComponentType() != NodeType.OPERATOR_TEMPLATE && node.getComponentType() != NodeType.OPERATOR) {
                    Component nodeComponent = componentDAO.findById(node.getComponentVO().getId()).orElseThrow(() -> new EntityNotFoundException(String.valueOf(node.getComponentVO().getId())));
                    String nodeComponentVersion = "";
                    if (nodeComponent.getComponentVersion() != null) {
                        nodeComponentVersion = nodeComponent.getComponentVersion().replace(".", "_");
                    }
                    // 如果fileList为空只添加复合组件本身的文件
                    if (nodeComponent.getFileList() == null) {
                        fileListMap.put(nodeComponent.getName() + nodeComponentVersion + ".py", nodeComponent.getPath());
                    } else {   // 保存子节点fileList中的所有数据
                        fileListMap.putAll(objectMapper.readValue(nodeComponent.getFileList(), new TypeReference<Map<String, String>>() {
                        }));
                    }
                }
            }
            try{
                fileListMap.put(service.getName() + ".py", service.getPath());
                service.setFileList(objectMapper.writeValueAsString(fileListMap));
                serviceDAO.save(service);
            } catch (JsonProcessingException e) {
                logger.error("保存fileList失败,错误信息:{}", e);
                e.printStackTrace();
            }
        }
    }
}
