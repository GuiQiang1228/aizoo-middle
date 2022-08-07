package aizoo.service;

import aizoo.common.GraphType;
import aizoo.common.Node;
import aizoo.common.NodeType;
import aizoo.domain.*;
import aizoo.common.ComponentType;
import aizoo.elasticObject.ElasticComponent;
import aizoo.elasticRepository.ComponentRepository;
import aizoo.repository.*;
import aizoo.utils.*;
import aizoo.viewObject.mapper.ComponentVOEntityMapper;
import aizoo.viewObject.mapper.DatasourceVOEntityMapper;
import aizoo.viewObject.mapper.GraphVOEntityMapper;
import aizoo.viewObject.mapper.ServiceVOEntityMapper;
import aizoo.viewObject.object.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.netty.shaded.io.netty.util.internal.StringUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.EnumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import aizoo.Client;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;

@Service("ComponentService")
public class ComponentService {


    @Value("${file.path}")
    String file_path;

    @Value("${save.path}")
    String save_path;

    @Value("${notebook.ip}")
    String notebookIp;

    @Value("${notebook.port}")
    String notebookPort;

    @Value("${lib.service.path}")
    String serviceTemplatePath;

    @Autowired
    ValidationService validationService;

    @Autowired
    TranslationService translationService;

    @Autowired
    ComponentDAO componentDAO;

    @Autowired
    DatasourceDAO datasourceDAO;

    @Autowired
    NamespaceDAO namespaceDAO;

    @Autowired
    UserDAO userDAO;

    @Autowired
    VisualContainerDAO visualContainerDAO;

    @Autowired
    ServiceDAO serviceDAO;

    @Autowired
    GraphDAO graphDAO;

    @Autowired
    DatatypeDAO datatypeDAO;

    @Autowired
    DAOUtil daoUtil;

    @Autowired
    ComponentInputParameterDAO componentInputParameterDAO;

    @Autowired
    ComponentOutputParameterDAO componentOutputParameterDAO;

    @Autowired
    NamespaceService namespaceService;

    @Autowired
    GraphService graphService;

    @Autowired
    ForkUtils forkUtils;

    @Autowired
    Client client;

    @Autowired
    ComponentVOEntityMapper componentVOEntityMapper;

    @Autowired
    GraphVOEntityMapper graphVOEntityMapper;

    @Autowired
    DatasourceVOEntityMapper datasourceVOEntityMapper;

    @Autowired
    ServiceVOEntityMapper serviceVOEntityMapper;

    @Autowired
    ProjectService projectService;

    @Autowired
    ComponentRepository componentRepository;

    @Autowired
    DatasourceService datasourceService;

    private static final List<ComponentType> USED_FOR_MODULE_OR_MODEL_GRAPH = Arrays.asList(ComponentType.PROCESS, ComponentType.FUNCTION, ComponentType.MODULE);

    private static final List<ComponentType> USED_FOR_LOSS_GRAPH = Arrays.asList(ComponentType.PROCESS, ComponentType.FUNCTION, ComponentType.MODULE, ComponentType.LOSS);

    private static final Logger logger = LoggerFactory.getLogger(ComponentService.class);


    /**
     * 用户上传组件
     * @param fileInfo 文件信息
     * @param componentVO 组件信息
     * @throws Exception
     */
    @Transactional
    public void uploadComponent(TFileInfoVO fileInfo, ComponentVO componentVO) throws Exception {
        logger.info("Start upload Component");
        Component component = ComponentVOEntityMapper.MAPPER.componentVO2Component(componentVO, datatypeDAO, componentDAO, namespaceDAO, userDAO);//有组件名字和版本
        logger.info("fileInfo: {}",fileInfo.toString());
        logger.info("componentVO: {}",componentVO.toString());


        String componentVersion = component.getComponentVersion();
        if (componentVersion != null) {
            componentVersion = component.getComponentVersion().replace(".", "_");
        }
        // 给每个新增的组件的output都加一个输出为self
        ComponentOutputParameter selfOutput = new ComponentOutputParameter();
        selfOutput.setIsSelf(true);
        selfOutput.setParameter(new Parameter(AizooConstans.SELF_NAME, AizooConstans.SELF_TITLE, AizooConstans.SELF_DESCRIPTION, AizooConstans.SELF_ORIGIN_NAME, datatypeDAO.findByName(AizooConstans.AIZOO_UNKNOWN)));
        component.getOutputs().add(selfOutput);
//      set组件的path
        String filename = component.getName() + componentVersion + ".py";
        String file = Paths.get(file_path, componentVO.getUsername(), "temp", fileInfo.getUniqueIdentifier(), filename).toString();
        String tempFolder = Paths.get(file_path, componentVO.getUsername(), "temp", fileInfo.getUniqueIdentifier()).toString();
        String nsPath = componentVO.getNamespace().replaceAll("\\.", Matcher.quoteReplacement(File.separator));
        String targetPath = Paths.get(file_path, nsPath).toString();
        logger.info("upload Component filePath: {}", targetPath);
        component.setPath(Paths.get(targetPath, filename).toString().replace("\\", "/"));
//        set组件的inputs和outputs
        Map<String, String> filePathMap = new HashMap<>();
        for (ComponentInputParameter componentInputParameter : component.getInputs()) {
            componentInputParameter.setComponent(component);
        }
        for (ComponentOutputParameter componentOutputParameter : component.getOutputs()) {
            componentOutputParameter.setComponent(component);
        }
//        set组件的title composed graph version 信息
        component.setTitle(component.getName());
        component.setComposed(false);
        component.setGraph(null);
        component.setComponentVersion(componentVO.getComponentVersion());
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            FileUtil.merge(file, tempFolder, filename);
            if (fileInfo.getName().endsWith(".zip")) {
//                若为压缩文件则解压到名命名空间下并返回所有文件列表
                filePathMap = FileUtil.unZipFile(file_path, file, tempFolder, componentVO.getNamespace());
                String fileList = objectMapper.writeValueAsString(filePathMap);
                component.setFileList(fileList);
            } else {
                FileUtil.copyFile(file, targetPath);
                filePathMap.put(filename, component.getPath());
                String fileList = objectMapper.writeValueAsString(filePathMap);
                component.setFileList(fileList);
            }
            componentDAO.save(component);
        } catch (Exception e) {
            logger.error("Upload Component Failed,Fail Information: {}", e);
            e.printStackTrace();
//            若数据库更新失败，则删除目录下本次上传的文件
            for (String key : filePathMap.keySet()) {
                File f = new File(filePathMap.get(key));
                FileUtil.deleteFile(f);
            }
            throw e;
        } finally {
//            将临时目录下的文件和文件夹删除
            String tempPath = Paths.get(file_path, componentVO.getUsername(), "temp", fileInfo.getUniqueIdentifier()).toString();
            FileUtils.deleteDirectory(new File(tempPath));
            logger.info("Delete Component temp folder and file");
        }
    }



    /**
     * 模糊查询，左侧搜索框
     * @param privacy 组件权限信息
     * @param username 当前用户名
     * @param type 组件类型
     * @param keyword 查询的关键字
     * @return
     */
    public List<Component> getComponentByKeyword(String privacy, String username, String type, String keyword) {
        logger.info("Start find Component By Keyword, username: {}, keyword: {}", username, keyword);
//        查询的为公开组件，含系统公开，其他用户公开的且当前用户未fork过的组件
        List<Component> componentList;
        if (privacy.equals("public")) {
            componentList = componentDAO.findByComponentTypeAndPrivacyAndUserUsernameNotAndTitleLikeAndPathIsNotNull(ComponentType.valueOf(type), privacy, username, "%" + keyword + "%");
//            当前用户fork过则剔除
            //          componentList.removeIf(component -> componentDAO.existsByForkFromAndUserUsername(component, username));
        }
//        查询的为当前用户的组件
        else {
            componentList = componentDAO.findByComponentTypeAndUserUsernameAndTitleLikeAndPathIsNotNull(ComponentType.valueOf(type), username, "%" + keyword + "%");
        }
        logger.info("End find Component By Keyword");
        return componentList;
    }




    /**
     * 全局模糊查询，左侧搜索框
     * @param username 当前用户名
     * @param keyword 查询的关键字
     * @param type 组件类型
     * @return
     */
    public HashMap<String, List<Object>> getAllComponentByKeyword(String username, String keyword, String type) {
//        type包括MODULE,MODEL,LOSS,JOB,SERVICE,APPLICATION
//        不同类型的图可查询的组件类别不同，且不一定都只查component
        HashMap<String, List<Object>> resultList = new HashMap<>();
        if (EnumUtils.isValidEnum(GraphType.class, type)) {
//            JOB要查component表 除MODULE PARAMETER以外的其他类型，datasource表
            if (GraphType.JOB == GraphType.valueOf(type)) {
//            查询的为公开组件，含系统公开，其他用户公开的且当前用户未fork过的组件,并且除MODULE PARAMETER类型
                List<Component> publicComponentList = componentDAO.findByComponentTypeIsNotInAndPrivacyAndUserUsernameNotAndTitleLikeAndPathIsNotNull(Arrays.asList(ComponentType.MODULE, ComponentType.PARAMETER, ComponentType.FUNCTION), "public", username, "%" + keyword + "%");
//            当前用户fork过则剔除
                //             publicComponentList.removeIf(component -> componentDAO.existsByForkFromAndUserUsername(component, username));
//            数据资源暂不支持fork 所以公共里暂不加入
                List<Component> privateComponentList = componentDAO.findByComponentTypeIsNotInAndPrivacyAndUserUsernameAndTitleLikeAndPathIsNotNull(Arrays.asList(ComponentType.MODULE, ComponentType.PARAMETER, ComponentType.FUNCTION), "private", username, "%" + keyword + "%");
                List<Datasource> privateDatasource = datasourceDAO.findByUserUsernameAndTitleLikeAndPrivacy(username, "%" + keyword + "%", "private");
                resultList.put("public", ListEntity2ListVO.component2ComponentVOObject(publicComponentList));
                resultList.put("private", ListEntity2ListVO.componentAndDatasource2ComponentVOAndDatasourceVOObject(privateComponentList, privateDatasource));
            }
//            SERVICE查component表 包括PROCESS MODEL FUNCTION PARAMETER
            else if (GraphType.SERVICE == GraphType.valueOf(type)) {
//            查询的为公开组件，含系统公开，其他用户公开的且当前用户未fork过的组件,包括PROCESS MODEL FUNCTION PARAMETER
                List<Component> publicComponentList = componentDAO.findByComponentTypeInAndPrivacyAndUserUsernameNotAndTitleLikeAndPathIsNotNull(Arrays.asList(ComponentType.PROCESS, ComponentType.MODEL, ComponentType.FUNCTION, ComponentType.PARAMETER), "public", username, "%" + keyword + "%");
//            当前用户fork过则剔除
//                publicComponentList.removeIf(component -> componentDAO.existsByForkFromAndUserUsername(component, username));
//            查询的为当前用户的组件,包括PROCESS MODEL FUNCTION PARAMETER
                List<Component> privateComponentList = componentDAO.findByComponentTypeInAndPrivacyAndUserUsernameAndTitleLikeAndPathIsNotNull(Arrays.asList(ComponentType.PROCESS, ComponentType.MODEL, ComponentType.FUNCTION, ComponentType.PARAMETER), "private", username, "%" + keyword + "%");
                resultList.put("public", ListEntity2ListVO.component2ComponentVOObject(publicComponentList));
                resultList.put("private", ListEntity2ListVO.component2ComponentVOObject(privateComponentList));
            }
//        APPLICATION查container component表 包括PROCESS FUNCTION DATALOADER service表 datasource表
            else if (GraphType.APPLICATION == GraphType.valueOf(type)) {
//            查询的为公开组件，含系统公开，其他用户公开的且当前用户未fork过的组件,包括PROCESS FUNCTION DATALOADER
                List<Component> publicComponentList = componentDAO.findByComponentTypeInAndPrivacyAndUserUsernameNotAndTitleLikeAndPathIsNotNull(Arrays.asList(ComponentType.PROCESS, ComponentType.FUNCTION, ComponentType.DATALOADER), "public", username, "%" + keyword + "%");
//            当前用户fork过则剔除
//                publicComponentList.removeIf(component -> componentDAO.existsByForkFromAndUserUsername(component, username));
//            查询的为当前用户的组件,包括PROCESS FUNCTION DATALOADER
                List<Component> privateComponentList = componentDAO.findByComponentTypeInAndPrivacyAndUserUsernameAndTitleLikeAndPathIsNotNull(Arrays.asList(ComponentType.PROCESS, ComponentType.FUNCTION, ComponentType.DATALOADER), "private", username, "%" + keyword + "%");
//            可视化放在个人结果里（放在公共说明需要fork 但是个人又不能fork）
                List<VisualContainer> visualContainerList = visualContainerDAO.findByTitleLike("%" + keyword + "%");
//            service暂时不公开
                List<aizoo.domain.Service> serviceList = serviceDAO.findByUserUsernameAndTitleLikeAndPrivacy(username, "%" + keyword + "%", "private");
//            datasource暂时不公开
                List<Datasource> datasourceList = datasourceDAO.findByUserUsernameAndTitleLikeAndPrivacy(username, "%" + keyword + "%", "private");
                resultList.put("public", ListEntity2ListVO.component2ComponentVOObject(publicComponentList));
                resultList.put("private", ListEntity2ListVO.componentAndVisualContainerAndServiceAndDatasource2ComponentVOAndVisualContainerAndServiceVOAndDatasourceVOObject(privateComponentList, visualContainerList, serviceList, datasourceList));
            }
        } else if (EnumUtils.isValidEnum(ComponentType.class, type)) {  // type为 MODULE/MODEL/LOSS时
            ComponentType componentType = ComponentType.valueOf(type);
            List<ComponentType> componentTypes = null;
            if (ComponentType.MODEL == componentType || ComponentType.MODULE == componentType)
                componentTypes = USED_FOR_MODULE_OR_MODEL_GRAPH;
            else
                componentTypes = USED_FOR_LOSS_GRAPH;
//            查询的为公开组件，含系统公开，其他用户公开的且当前用户未fork过的组件
            List<Component> publicComponentList = componentDAO.findByPrivacyAndUserUsernameNotAndTitleLikeAndPathIsNotNullAndComponentTypeIn("public", username, "%" + keyword + "%", componentTypes);
//            当前用户fork过则剔除
//            publicComponentList.removeIf(component -> componentDAO.existsByForkFromAndUserUsername(component, username));
//            查询的为当前用户的组件
            List<Component> privateComponentList = componentDAO.findByUserUsernameAndTitleLikeAndPathIsNotNullAndComponentTypeIn(username, "%" + keyword + "%", componentTypes);
            resultList.put("public", ListEntity2ListVO.component2ComponentVOObject(publicComponentList));
            resultList.put("private", ListEntity2ListVO.component2ComponentVOObject(privateComponentList));
        }
        return resultList;
    }

    /***
     * 根据组件类型获取组件列表
     * @param username 当前用户的用户名
     * @param privacy     public/private
     *                 为 public时，传回公共数据，private传回用户数据
     * @param type     组件类型
     * @return 前端需要展示的组件或数据资源
     */
    public List<Component> getComponentByType(String username, String privacy, String type) {
        logger.info("Start Find Component By Type,username: {}, type: {}", username, type);
//       请求类型的隐私性（public包含系统和用户公开（不含当前用户公开和当前用户已经fork过的组件），private为用户自己的组件，无论公开与否
//       用户请求的为公开组件
        List<Component> componentList = null;
        if (type.equals("OPERATOR_TEMPLATE")) {// 这里如果是运算符模板，则返回运算符模板的list（真正的list需要用户输入input数量后再请求返回）
            componentList = componentDAO.findByComponentType(ComponentType.OPERATOR_TEMPLATE);
            return componentList;
        }
        if (privacy.equals("public")) {
            componentList = componentDAO.findByComponentTypeAndUserUsernameNotAndPrivacyAndPathIsNotNull(ComponentType.valueOf(type), username, privacy);
//                如果用户fork过则剔除
            componentList.removeIf(component -> componentDAO.existsByForkFromAndUserUsername(component, username));
        }
//            用户请求的为个人数据
        else if (privacy.equals("private")) {
            componentList = componentDAO.findByComponentTypeAndUserUsernameAndPathIsNotNull(ComponentType.valueOf(type), username);
        } else if (StringUtil.isNullOrEmpty(privacy)) {
            componentList = componentDAO.findByComponentType(ComponentType.PARAMETER);
        }
        return componentList;
    }


    /**
     * 判断该组件是否完整，能否进行fork操作
     *
     * @param sourceId 服务id或者组件id，实验图或应用图id
     * @param type 组件类型
     * @return true/false 能否进行fork
     */
    public boolean allowedFork(Long sourceId, String type) {
        if (type.equals("SERVICE")) {
            if (!serviceDAO.existsById(sourceId)) {//判断该service是否存在
                return false;
            }
            aizoo.domain.Service service = serviceDAO.findById(sourceId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(sourceId)));
            Long sourceGraphId = service.getGraph().getId();
            if (!graphDAO.existsById(sourceGraphId)) {//判断该图是否存在
                return false;
            }
            Graph sourceGraph = graphDAO.findById(sourceGraphId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(sourceGraphId)));
            GraphVO sourceGraphVO = graphVOEntityMapper.graph2GraphVO(sourceGraph);
            List<Node> nodeList = sourceGraphVO.getNodeList();
            if (nodeList != null) {
                for (Node node : nodeList) {//只需要判断nodelist中的COMPONENT类型是否全部存在
                    if ((node.getComponentType().toString() != ("DATASOURCE")) && (node.getComponentType().toString() != ("PARAMETER"))) {
                        if (!allowedFork(node.getComponentVO().getId(), "COMPONENT")) {
                            return false;//中间有一个子组件不存在就返回false，否则继续遍历
                        }
                    }
                }
            }
            return true;
        } else if (type.equals("COMPONENT")) {
            if (!componentDAO.existsById(sourceId)) {//判断该组件是否存在
                return false;
            }
            Component sourceComponent = componentDAO.findById(sourceId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(sourceId)));
            if (sourceComponent.isComposed()) {//如果是复合组件还需要判断它的子组件
                Long sourceGraphId = sourceComponent.getGraph().getId();
                if (!graphDAO.existsById(sourceGraphId)) {//判断图是否存在
                    return false;
                }
                Graph sourceGraph = graphDAO.findById(sourceGraphId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(sourceGraphId)));
                GraphVO sourceGraphVO = graphVOEntityMapper.graph2GraphVO(sourceGraph);
                List<Node> nodeList = sourceGraphVO.getNodeList();
                if (nodeList != null) {
                    for (Node node : nodeList) {//只需要判断nodelist中的COMPONENT类型是否全部存在
                        if (node.getComponentType().toString() != ("DATASOURCE")) {
                            if (!allowedFork(node.getComponentVO().getId(), "COMPONENT")) {
                                return false;
                            }
                        }
                    }
                }
            }
            return true;
        } else if (type.equals("JOB")) {     //判断实验图
            if (!graphDAO.existsById(sourceId)) {//判断图是否存在
                return false;
            }
            Graph sourceGraph = graphDAO.findById(sourceId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(sourceId)));
            GraphVO sourceGraphVO = graphVOEntityMapper.graph2GraphVO(sourceGraph);
            List<Node> nodeList = sourceGraphVO.getNodeList();
            if (nodeList != null) {
                for (Node node : nodeList) {//只需要判断nodelist中的COMPONENT类型是否全部存在
                    if (node.getComponentType().toString() != ("DATASOURCE")) {
                        if (!allowedFork(node.getComponentVO().getId(), "COMPONENT")) {
                            return false;
                        }
                    }
                }
            }
            return true;
        } else if (type.equals("APPLICATION")) {
            if (!graphDAO.existsById(sourceId)) {//判断图是否存在
                return false;
            }
            Graph sourceGraph = graphDAO.findById(sourceId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(sourceId)));
            GraphVO sourceGraphVO = graphVOEntityMapper.graph2GraphVO(sourceGraph);
            List<Node> nodeList = sourceGraphVO.getNodeList();
            if (nodeList != null) {
                for (Node node : nodeList) {//只需要判断nodelist中的COMPONENT类型是否全部存在
                    if ((node.getComponentType().toString() != ("DATASOURCE")) && (node.getComponentType().toString() != ("VISUALCONTAINER"))) {
                        if (node.getComponentType().toString().equals("SERVICE")) {//如果node是service类型
                            if (!allowedFork(node.getServiceVO().getId(), "SERVICE")) {
                                return false;
                            }
                        } else {//剩下的是COMPONENT类型
                            if (!allowedFork(node.getComponentVO().getId(), "COMPONENT")) {
                                return false;
                            }
                        }
                    }
                }
            }
            //完成所有的nodelist遍历，则返回true
            return true;
        } else {//如果是其他类型的组件，不允许fork
            return false;
        }
    }

    /**
     * 在图中修改的数据，fork后也需要更新到component的nodelist中
     *
     * @param childComponent1 待修改的组件
     * @param sourceVO 图更新之后的componentVO
     * @return 修改更新信息之后的组件
     * @throws JsonProcessingException
     */
    public Component inputOutputRenameFromGraph(Component childComponent1, ComponentVO sourceVO) throws JsonProcessingException {
        logger.info("Start Rename input output,sourceComponentId: {}",sourceVO.getId());
        Component sourceNodeComponent = componentDAO.findById(sourceVO.getId()).orElseThrow(() -> new EntityNotFoundException(String.valueOf(sourceVO.getId())));
        Component sourceComponent1 = new Component();
        BeanUtils.copyProperties(sourceNodeComponent, sourceComponent1);
//        set title properties discription input output 信息
        childComponent1.setTitle(sourceVO.getTitle());
        ObjectMapper objectMapper = new ObjectMapper();
        String properties = objectMapper.writeValueAsString(sourceVO.getProperties());
        childComponent1.setProperties(properties);
        childComponent1.setDescription(sourceVO.getDescription());
        List<Map<String, Object>> sourceInputList = sourceVO.getInputs();
        List<Map<String, Object>> sourceOutputList = sourceVO.getOutputs();
        List<ComponentInputParameter> newInputList = childComponent1.getInputs();
        List<ComponentOutputParameter> newOutputList = childComponent1.getOutputs();
//        把newList中的和sourceList中的同名输入输出的title进行比较，如果不同则替换；由于是同一个node的input output 不存在重名的情况

        for (ComponentInputParameter newInput : newInputList) {
            for (Map<String, Object> map : sourceInputList) {
                if (((newInput.getParameter().getTitle()).equals(map.get("title").toString())) && (!((newInput.getParameter().getName()).equals(map.get("name").toString())))) {
                    newInput.getParameter().setName(map.get("name").toString());
                }
            }
        }
        childComponent1.setInputs(newInputList);
        for (ComponentOutputParameter newOutput : newOutputList) {
            for (Map<String, Object> map : sourceOutputList) {
                if (((newOutput.getParameter().getTitle()).equals(map.get("title").toString())) && (!((newOutput.getParameter().getName()).equals(map.get("name").toString())))) {
                    newOutput.getParameter().setName(map.get("name").toString());
                }
            }
        }
        childComponent1.setOutputs(newOutputList);
        return childComponent1;
    }


    /**
     * fork过程中组织一个新的组件
     * @param targetUser 当前用户
     * @param finalTargetComponent 待组织的组件
     * @param sourceComponent 待fork的组件
     * @param isFirstAtom 用户最初选择fork的组件的是否为原子组件
     * @return
     */
    public Component buildNewComponent(User targetUser, Component finalTargetComponent, Component sourceComponent,
                                       boolean isFirstAtom) {

        logger.info("buildNewComponent start,sourceComponentid={},  finalTargetComponentid={}", sourceComponent.getId(), finalTargetComponent.getId());
        Long sourceComponentId = sourceComponent.getId();
        String targetNamespace = "";
//          新建命名空间 username.type.version(fork+forkfromuser+number).forkfrom的命名空间
        targetNamespace = namespaceService.forkRegisterNamespace(sourceComponentId, isFirstAtom, targetUser, "COMPONENT");
        Namespace namespace = namespaceDAO.findByNamespace(targetNamespace);

        logger.info("buildNewComponent forkRegisterNamespace complete,sourceComponentid={},  finalTargetComponentid={}", sourceComponent.getId(), finalTargetComponent.getId());
//        set 目标组件的一系列信息
        finalTargetComponent.setNamespace(namespace);
        finalTargetComponent.setName(sourceComponent.getName());
        finalTargetComponent.setForkFrom(sourceComponent);
        finalTargetComponent.setTitle(sourceComponent.getTitle());
        finalTargetComponent.setFramework(sourceComponent.getFramework());
        finalTargetComponent.setFrameworkVersion(sourceComponent.getFrameworkVersion());
        finalTargetComponent.setDescription(sourceComponent.getDescription());
        finalTargetComponent.setComponentVersion(sourceComponent.getComponentVersion());
        finalTargetComponent.setUser(targetUser);
        finalTargetComponent.setPrivacy("private");
        finalTargetComponent.setReleased(sourceComponent.isReleased());
        finalTargetComponent.setComposed(sourceComponent.isComposed());
        finalTargetComponent.setComponentType(sourceComponent.getComponentType());
        finalTargetComponent.setProperties(sourceComponent.getProperties());
        logger.info("buildNewComponent setValues complete,sourceComponentid={},  finalTargetComponentid={}", sourceComponent.getId(), finalTargetComponent.getId());

//      set它的input和output
        finalTargetComponent.setInputs(buildNewInputList(sourceComponent, finalTargetComponent));
        logger.info("buildNewComponent setinputs complete,sourceComponentid={},  finalTargetComponentid={}", sourceComponent.getId(), finalTargetComponent.getId());

        finalTargetComponent.setOutputs(buildNewOutputList(sourceComponent, finalTargetComponent));
        logger.info("buildNewComponent setoutputs complete,sourceComponentid={},  finalTargetComponentid={}", sourceComponent.getId(), finalTargetComponent.getId());
//        finalTargetComponent.setGraph(targetGraph);
        componentDAO.save(finalTargetComponent);
        return finalTargetComponent;
    }

    /**
     * 组织fork后的新的nodelist
     *
     * @param sourceNode 待fork的组件node
     * @param targetUser 当前用户
     * @param forkedComponent 在这一次fork中已经fork过的组件
     * @return 组织过后的新node
     * @throws IOException
     */
    public HashMap<String, Object> buildNewNode(Node sourceNode, User targetUser, HashMap<Long, Long> forkedComponent, HashMap<Long, Long> forkedService, HashMap<Long, Long> forkedDatasource) throws Exception {
        Component childComponent = null;
        Datasource childDatasource = null;
        aizoo.domain.Service childService = null;
        //  fork过来的默认为私有的
        String componentPrivacy = "private";
        boolean isFirstAtom = false;
        String username = targetUser.getUsername();
        logger.info("buildNewNode start... id={}", sourceNode.getId());
        if (sourceNode.getComponentType().toString().equals("DATASOURCE")) {
            //  1. 如果是datasource，判断是否需要fork
            //如果之前fork过了
            if (forkedDatasource.containsKey(sourceNode.getDatasourceVO().getId()) && forkedDatasource.get(sourceNode.getDatasourceVO().getId()) != null) {
                Long id = forkedDatasource.get(sourceNode.getDatasourceVO().getId());
                childDatasource = datasourceDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
            } else {
                Datasource sourceDatasource = datasourceVOEntityMapper.datasourceVO2Datasource(sourceNode.getDatasourceVO(), datasourceDAO, namespaceDAO, userDAO, datatypeDAO);
                childDatasource = datasourceService.forkDatasource(targetUser, sourceDatasource);
                forkedDatasource.put(sourceDatasource.getId(), childDatasource.getId());
            }
        } else if (sourceNode.getComponentType().toString().equals("PARAMETER") || sourceNode.getComponentType().toString().equals("OPERATOR")) {
            //  2. 如果是parameter或operator需要把节点加入nodelist，但是该参数不用fork，是所有人公用的，且标记该组件被fork过
            Long sourceId = sourceNode.getComponentVO().getId();
            childComponent = componentDAO.findById(sourceId).orElseThrow(() ->
                    new EntityNotFoundException(String.valueOf(sourceId)));
            forkedComponent.put(sourceId, sourceId);
        } else if (sourceNode.getComponentType().toString().equals("SERVICE")) {
            //  3. 如果是service
            try {
                childService = forkService(sourceNode.getServiceVO().getId(), username, forkedComponent, forkedService, forkedDatasource);
            } catch (Exception e) {
                e.printStackTrace();
            }
            childComponent = null;
        } else if (sourceNode.getComponentType().toString().equals("VISUALCONTAINER")) {
            //  4. 如果是可视化容器 则不拷贝
            childComponent = null;
        } else {
            //  5. 如果是组件，判断是否需要fork
            boolean flag = true;
            if (forkedComponent.containsKey(sourceNode.getComponentVO().getId()) && forkedComponent.get(sourceNode.getComponentVO().getId()) != null) {
                //  fork过这个组件且target不为null,则不进行fork
                flag = false;
            }
            if (flag) {
                //  子组件的description是空
                Component c = newFork(targetUser, sourceNode.getComponentVO().getId(), componentPrivacy, null, isFirstAtom, forkedComponent, forkedService, forkedDatasource);
                childComponent = componentDAO.findById(c.getId()).orElseThrow(() -> new EntityNotFoundException(String.valueOf(c.getId())));
            } else {
                //  如果之前fork过了，那么查找对应的value为对应的childComponent
                Long id = forkedComponent.get(sourceNode.getComponentVO().getId());
                childComponent = componentDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
            }
        }
        HashMap<String, Object> newNode = new HashMap<>();
        //  set node的键值
        newNode.put("componentType", sourceNode.getComponentType());
        newNode.put("componentVersion", sourceNode.getComponentVersion());
        newNode.put("checkPointId", sourceNode.getCheckPointId());
        newNode.put("dataloaderType", sourceNode.getDataloaderType());
        newNode.put("serviceJobId", sourceNode.getServiceJobId());
        newNode.put("parameterName", sourceNode.getParameterName());
        //  数据库中nodelist中的component存的是实体，不是vo
        if (childComponent != null) {
            Component childComponent1 = new Component();
            BeanUtils.copyProperties(childComponent, childComponent1);
            ComponentVO sourceVO = sourceNode.getComponentVO();
            newNode.put("component", inputOutputRenameFromGraph(childComponent1, sourceVO));
            newNode.put("exposedOutput", forkUtils.updateExposedOutput(childComponent, sourceNode));
        } else {
            newNode.put("component", childComponent);
            newNode.put("exposedOutput", sourceNode.getExposedOutput());
        }
        if (sourceNode.getDatasourceVO() != null) { //判断被fork的node的datasource是否为空
            newNode.put("datasource", childDatasource);
        } else {
            newNode.put("datasource", null);
        }

        newNode.put("service", childService);
        newNode.put("variable", sourceNode.getVariable());
        newNode.put("id", sourceNode.getId());
        VisualContainerVO containerVO = sourceNode.getVisualContainerVO();
        VisualContainer nodeContainer = null;
        if (containerVO != null) // 判断被fork的node的containerVO是否为空
            nodeContainer = daoUtil.findVisualContainerById(containerVO.getId());
        newNode.put("visualContainer", nodeContainer);
//        saveOutput已经不用了
        newNode.put("saveOutput", null);
        return newNode;
    }


    /**
     * 组织fork后的Graph
     *
     * @param targetUser 当前用户
     * @param newNodeList 组织过后的新的nodelist
     * @param targetService 新图的service键值
     * @param sourceGraph 待fork的graph
     * @param forkedComponent  在这一次fork中已经fork过的组件
     * @return 组织后的新graph
     * @throws JsonProcessingException
     */
    @Transactional
    public Graph buildNewGraph(User targetUser, List<HashMap<String, Object>> newNodeList, aizoo.domain.Service targetService,
                               Component targetComponent, Graph sourceGraph, HashMap<Long, Long> forkedComponent, HashMap<Long, Long> forkedService, HashMap<Long, Long> forkedDatasource) throws JsonProcessingException {
        Graph finalTargetGraph = new Graph();
//        set fork后的graph的一系列信息
        logger.info("Start Build New Graph,sourceGraphId: {}", sourceGraph.getId());
        finalTargetGraph.setComponent(targetComponent);
        finalTargetGraph.setUser(targetUser);
        ObjectMapper objectMapper = new ObjectMapper();
        finalTargetGraph.setNodeList(objectMapper.writeValueAsString(newNodeList));
        finalTargetGraph.setLinkList(sourceGraph.getLinkList());
        finalTargetGraph.setGraphKey(sourceGraph.getGraphType().toString() + "-" + UUID.randomUUID().toString());
        finalTargetGraph.setName(sourceGraph.getName());
        logger.info("OriginJson={}", sourceGraph.getOriginJson());
        String targetOriginJson = forkUtils.buildOriginJson(sourceGraph.getOriginJson(), forkedComponent, forkedService, forkedDatasource);
        finalTargetGraph.setOriginJson(targetOriginJson);
        finalTargetGraph.setDescription(sourceGraph.getDescription());
        finalTargetGraph.setGraphType(sourceGraph.getGraphType());
        finalTargetGraph.setGraphVersion(sourceGraph.getGraphVersion());
        finalTargetGraph.setReleased(sourceGraph.isReleased());
        finalTargetGraph.setGraphVersion(sourceGraph.getGraphVersion());
//        set graph对应的ApplitcationjobId
        finalTargetGraph.setApplications(null);
//        set graph对应的serviceId
        finalTargetGraph.setService(targetService);
        graphDAO.save(finalTargetGraph);
        return finalTargetGraph;
    }

    /**
     * 组织fork之后的service
     *
     * @param targetService 待返回的新service
     * @param sourceService 待fork的service
     * @param targetUser 当前用户
     * @return 组织之后的service
     */
    public aizoo.domain.Service buildNewService(aizoo.domain.Service targetService, aizoo.domain.Service sourceService,
                                                User targetUser) {
//      根据对应信息新建namespace
        String targetNamespace = "";
        targetNamespace = namespaceService.forkRegisterNamespace(sourceService.getId(), true, targetUser, "SERVICE");
        Namespace namespace = namespaceDAO.findByNamespace(targetNamespace);
        targetService.setNamespace(namespace);
//        set fork后的service的一系列信息
        targetService.setDescription(sourceService.getDescription());
        targetService.setPrivacy("private");
        targetService.setName(sourceService.getName());
        targetService.setTitle(sourceService.getTitle());
        //  set path 根据为该服务自动注册的namespace生成存放目录
        Path serviceDir = Paths.get(file_path, namespace.getNamespace().split("\\."));
        String serviceVersion = sourceService.getServiceVersion();
        if (serviceVersion != null) {
            serviceVersion = serviceVersion.replace(".", "_");
        }
        Path serviceFilePath = Paths.get(serviceDir.toString(), sourceService.getName() + serviceVersion + ".py");
        File serviceFile = new File(serviceFilePath.toString());
        targetService.setPath(serviceFile.getAbsolutePath().replace("\\", "/"));
        targetService.setReleased(sourceService.isReleased());
        targetService.setFramework(sourceService.getFramework());
        targetService.setFrameworkVersion(sourceService.getFrameworkVersion());
        targetService.setServiceVersion(sourceService.getServiceVersion());
        targetService.setForkFrom(sourceService);
        targetService.setUser(targetUser);

        // 组织sevice的 input output
        List<ServiceInputParameter> myServiceInputParameters = buildNewInputList(sourceService, targetService);

        targetService.setInputs(myServiceInputParameters);
        List<ServiceOutputParameter> myServiceOutputParameters = buildNewOutputList(sourceService, targetService);
        targetService.setOutputs(myServiceOutputParameters);

        serviceDAO.save(targetService);
        return targetService;
    }

    /**
     * 组织forkService后的input
     *
     * @param sourceService 待fork的service
     * @param targetService fork之后的新service
     * @return 组织之后的inputList
     */
    public List<ServiceInputParameter> buildNewInputList(aizoo.domain.Service sourceService, aizoo.domain.Service targetService) {
        // 旧的inputs
        List<ServiceInputParameter> serviceInputParameters = sourceService.getInputs();
        // 新的inputs
        List<ServiceInputParameter> myServiceInputParameters = new ArrayList<>();
        for (ServiceInputParameter serviceInputParameter : serviceInputParameters) {
            Parameter p = serviceInputParameter.getParameter();
            logger.info("source serviceInputParameter={}", p.getName());
            ServiceInputParameter myServiceInputParameter = new ServiceInputParameter(
                    serviceInputParameter.getParameter(), targetService, serviceInputParameter.getParameterIoType());
            logger.info("target  serviceInputParameter={}", myServiceInputParameter.getParameter().getName());
            myServiceInputParameters.add(myServiceInputParameter);
        }
        return myServiceInputParameters;
    }

    /**
     * 组织forkService后的的output
     *
     * @param sourceService 待fork的service
     * @param targetService fork之后的新service
     * @return 组织之后的outputList
     */
    public List<ServiceOutputParameter> buildNewOutputList(aizoo.domain.Service sourceService, aizoo.domain.Service targetService) {
        // 新的outputs
        List<ServiceOutputParameter> myServiceOutputParameters = new ArrayList<>();
        // 旧的outputs
        List<ServiceOutputParameter> serviceOutputParameters = sourceService.getOutputs();
        for (ServiceOutputParameter serviceOutputParameter : serviceOutputParameters) {
            if (!serviceOutputParameter.getIsSelf()) {
                Parameter p = serviceOutputParameter.getParameter();
                logger.info("source serviceOutputParameter={}", p.getName());
                ServiceOutputParameter myServiceOutputParameter = new ServiceOutputParameter(
                        serviceOutputParameter.getParameter(), targetService, serviceOutputParameter.getParameterIoType(), serviceOutputParameter.getIsSelf());
                logger.info("target  serviceOutputParameter={}", myServiceOutputParameter.getParameter().getName());
                myServiceOutputParameters.add(myServiceOutputParameter);
            }
        }

        // 给每个新增的服务的output都加一个输出为self
        ServiceOutputParameter selfOutput = new ServiceOutputParameter();
        selfOutput.setIsSelf(true);
        selfOutput.setParameter(new Parameter(AizooConstans.SELF_NAME, AizooConstans.SELF_TITLE, AizooConstans.SELF_DESCRIPTION, AizooConstans.SELF_ORIGIN_NAME, datatypeDAO.findByName(AizooConstans.AIZOO_UNKNOWN)));
        myServiceOutputParameters.add(selfOutput);
        return myServiceOutputParameters;
    }

    /**
     * 重新组织forkComponent的input
     *
     * @param sourceComponent 待fork的component
     * @param finalTargetComponent fork之后的新component
     * @return 组织之后的inputList
     */
    public List<ComponentInputParameter> buildNewInputList(Component sourceComponent, Component finalTargetComponent) {
        // 旧的inputs
        List<ComponentInputParameter> componentInputParameters = sourceComponent.getInputs();
        // 新的inputs
        List<ComponentInputParameter> myComponentInputParameters = new ArrayList<>();
        for (ComponentInputParameter componentInputParameter : componentInputParameters) {
            ComponentInputParameter myComponentInputParameter = new ComponentInputParameter(
                    componentInputParameter.getParameter(), finalTargetComponent, componentInputParameter.getParameterIoType());
            componentInputParameterDAO.save(myComponentInputParameter);
            myComponentInputParameters.add(myComponentInputParameter);
        }
        return myComponentInputParameters;
    }

    /**
     * 重新组织forkComponent的output
     *
     * @param sourceComponent 待fork的component
     * @param finalTargetComponent fork之后的新component
     * @return 组织之后的outputList
     */
    public List<ComponentOutputParameter> buildNewOutputList(Component sourceComponent, Component finalTargetComponent) {
        // 新的outputs
        List<ComponentOutputParameter> myComponentOutputParameters = new ArrayList<>();
        // 旧的outputs
        List<ComponentOutputParameter> componentOutputParameters = sourceComponent.getOutputs();
        for (ComponentOutputParameter componentOutputParameter : componentOutputParameters) {
            if (!componentOutputParameter.getIsSelf()) {
                ComponentOutputParameter myComponentOutputParameter = new ComponentOutputParameter(
                        componentOutputParameter.getParameter(), finalTargetComponent, componentOutputParameter.getParameterIoType(), componentOutputParameter.getIsSelf());
                componentOutputParameterDAO.save(myComponentOutputParameter);
                myComponentOutputParameters.add(myComponentOutputParameter);
            }
        }
        return myComponentOutputParameters;
    }

    /**
     * 拷贝组件和service的缩略图
     *
     * @param sourceId 待fork的serviceId
     * @param sourceGraph 待fork的graph
     * @param username 当前用户
     * @param targetGraph fork之后的graph
     * @param type 图的类型
     * @throws IOException
     */
    public void copyPicturePNG(Long sourceId, Graph sourceGraph, String username, Graph targetGraph, String type) throws IOException {
        // 待fork的缩略图路径
        String sourcePictureFullPath = null;
        // fork后的缩略图路径
        String targetPicturePath = null;
        // 根据不同的类型生成不同的路径
        if (type.equals("COMPONENT")) {
            Component sourceComponent = componentDAO.findById(sourceId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(sourceId)));
            String sourceUserName = sourceComponent.getUser().getUsername();
            sourcePictureFullPath = Paths.get(file_path, sourceUserName, "picture",
                    sourceComponent.getComponentType().getValue(), sourceGraph.getId() + "_" + sourceGraph.getName() + ".png").toString();//带文件名的完整路径
            targetPicturePath = Paths.get(file_path, username, "picture",
                    sourceComponent.getComponentType().getValue()).toString();//仅仅是路径
        } else if (type.equals("SERVICE")) {
            aizoo.domain.Service sourceService = serviceDAO.findById(sourceId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(sourceId)));
            String sourceUserName = sourceService.getUser().getUsername();
            sourcePictureFullPath = Paths.get(file_path, sourceUserName, "picture",
                    "service", sourceGraph.getId() + "_" + sourceGraph.getName() + ".png").toString();//带文件名的完整路径
            targetPicturePath = Paths.get(file_path, username, "picture",
                    "service").toString();//仅仅是路径
        }
        File sourcePictureFile = new File(sourcePictureFullPath);
        // 如果被fork的缩略图不存在
        if (!sourcePictureFile.exists()) {
            logger.info("缩略图不存在：{}", sourcePictureFullPath);
            return;
        }
        try {
            // 执行拷贝缩略图
            String myPictureSavePath = FileUtil.copyFile(sourcePictureFullPath, targetPicturePath);//将对应路径的文件拷贝过来
            File myPictureFile = new File(myPictureSavePath);
            String newname = (targetGraph.getId() + "_" + targetGraph.getName() + ".png");
            File newfile = new File(myPictureFile.getParent() + File.separator + newname);
            //  需要将新的文件改名
            myPictureFile.renameTo(newfile);
        } catch (FileNotFoundException e) {
            logger.error("缩略图拷贝失败,{}", e);
        }
    }

    /**
     * 补充service的filelist信息
     *
     * @param finalTargetGraph fork之后的graph
     * @param targetService 待fork的service
     * @return 重新组织之后的新service的filelist
     * @throws JsonProcessingException
     */
    public HashMap<String, String> buildNewServiceFileList(Graph finalTargetGraph, aizoo.domain.Service targetService) throws JsonProcessingException {
        HashMap<String, String> fileList = new HashMap<>();
        //  将组成服务的所有原子组件以及自己的路径存放在复合组件的fileList中
        ObjectMapper objectMapper1 = new ObjectMapper();
        GraphVO targetGraphVO = graphVOEntityMapper.graph2GraphVO(finalTargetGraph);
        for (Node node : targetGraphVO.getNodeList()) {
            if (node.getComponentType() != NodeType.PARAMETER && node.getComponentType() != NodeType.OPERATOR && node.getComponentType() != NodeType.OPERATOR_TEMPLATE) {
                Component nodeComponent = componentDAO.findById(node.getComponentVO().getId()).orElseThrow(() -> new EntityNotFoundException(String.valueOf(node.getComponentVO().getId())));
                String nodeComponentVersion = "";
                if (nodeComponent.getComponentVersion() != null) {
                    nodeComponentVersion = nodeComponent.getComponentVersion().replace(".", "_");
                }
                //  添加fileList
                if (nodeComponent.getFileList() == null) {
                    fileList.put(nodeComponent.getName() + nodeComponentVersion + ".py", nodeComponent.getPath());
                } else {   //   保存子节点fileList中的所有数据
                    fileList.putAll(objectMapper1.readValue(nodeComponent.getFileList(), new TypeReference<Map<String, String>>() {
                    }));
                }
            }
        }
        fileList.put(targetService.getName() + ".py", targetService.getPath());
        return fileList;
    }

    /**
     * forkComponent
     *
     * @param targetUser        当前用户
     * @param sourceComponentId 被fork的组件
     * @param componentPrivacy 组件权限 public/private
     * @param description 组件描述
     * @param isFirstAtom     用户最初选择fork的组件的是否为原子组件
     * @return fork之后的新组件
     * @throws IOException
     */

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Component newFork(User targetUser, Long sourceComponentId, String componentPrivacy, String description, boolean isFirstAtom, HashMap<Long, Long> forkedComponent, HashMap<Long, Long> forkedService, HashMap<Long, Long> forkedDatasource) throws Exception {

        Component finalTargetComponent = new Component();
        Component sourceComponent = componentDAO.findById(sourceComponentId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(sourceComponentId)));
        String username = targetUser.getUsername();
        if ((!sourceComponent.isComposed())) {
            logger.info("newFork atomic fork start.....");
                /*
                一. 原子组件
                 */
            //  1. 重新组织component
            Component targetComponent = new Component();
            targetComponent.setGraph(null);
            logger.info("setGraph complete");

            targetComponent = buildNewComponent(targetUser, targetComponent, sourceComponent, isFirstAtom);
            logger.info("buildNewComponent complete");

            //  1) 补充filelist和path信息
            String targetNamespace = targetComponent.getNamespace().getNamespace();
            //  fork之后的文件路径  会根据windows系统和linux系统生成正反斜杠
            Path myForkFileDir = Paths.get(file_path, targetNamespace.split("\\."));
            logger.info("newFork atomic  myForkFileDir={}", myForkFileDir.toString());

            //  加上文件名和版本号
            Path myForkPath = Paths.get(String.valueOf(myForkFileDir), sourceComponent.getPath().substring(sourceComponent.getPath().lastIndexOf("/") + 1));
            //  拷贝文件,forkFromPaths为源文件集，myForkFilePaths为fork后的文件集
            String sourceComponentFilePaths = sourceComponent.getFileList();
//            logger.info("sourceComponentFilePaths ={}", sourceComponentFilePaths);

            String myForkFilePaths = FileUtil.forkFileCopy(sourceComponentFilePaths, myForkFileDir.toString());//将对应路径的文件拷贝过来
//            logger.info("forkFileCopy complete source ={},target={},myForkFilePaths=", sourceComponentFilePaths, myForkFileDir.toString(), myForkFilePaths);

            String path = myForkPath.toString().replace("\\", "/");
            targetComponent.setFileList(myForkFilePaths);
            targetComponent.setPath(path);

            componentDAO.save(targetComponent);

            //  2. 在内存中加入此component
            forkedComponent.put(sourceComponentId, targetComponent.getId());
            return targetComponent;
        } else {
                /*
                二. 复合组件
                 */

            Graph sourceGraph = sourceComponent.getGraph();
            GraphVO sourceGraphVO = graphVOEntityMapper.graph2GraphVO(sourceGraph);
            //  1. 组织新的nodelist
            List<Node> nodeList = sourceGraphVO.getNodeList();
            List<HashMap<String, Object>> newNodeList = new ArrayList<>();
            for (Node sourceNode : nodeList) {
                HashMap<String, Object> newNode = buildNewNode(sourceNode, targetUser, forkedComponent, forkedService, forkedDatasource);
                ;
                newNodeList.add(newNode);
            }
            logger.info("组织nodelist成功");

            componentDAO.save(finalTargetComponent);

            //  2. 重新组织graph
            Graph targetGraph = buildNewGraph(targetUser, newNodeList, null, finalTargetComponent, sourceGraph, forkedComponent, forkedService, forkedDatasource);
            logger.info("组织graph成功!graphid={}", targetGraph.getId());

            //  3. 重新组织component
            finalTargetComponent = buildNewComponent(targetUser, finalTargetComponent, sourceComponent, false);
            finalTargetComponent.setGraph(targetGraph);
            logger.info("组织component成功");

            //  4. 执行翻译这个复合组件，更新复合组件的filelist path
            ComponentVO componentVO = componentVOEntityMapper.component2ComponentVO(finalTargetComponent);
            GraphVO graphVO = graphVOEntityMapper.graph2GraphVO(targetGraph);
            if (sourceComponent.isReleased()) {
                graphService.release2ComponentAndGraph(graphVO, componentVO);
            }
            //  1) 在执行release的过程中，由于VO的设置导致forkFrom数据丢失，因此手动添加
            finalTargetComponent.setForkFrom(sourceComponent);
            componentDAO.save(finalTargetComponent);
            logger.info("翻译发布复合组件成功");

            //  5. 复合组件拷贝它的缩略图
            copyPicturePNG(sourceComponentId, sourceGraph, username, targetGraph, "COMPONENT");
            logger.info("拷贝缩略图成功");
            //  6. 将此component的id添加到内存中，避免重复fork
            forkedComponent.put(sourceComponentId, finalTargetComponent.getId());
            return finalTargetComponent;
        }
    }

    /**
     * fork实验图
     *
     * @param sourceGraphId 被fork的graphId
     * @param username 当前用户
     * @param forkedComponent 这一次fork当中已经fork过的组件
     * @return fork之后的新graph
     * @throws IOException
     */

    public Graph forkExperimentGraph(Long sourceGraphId, String username, HashMap<Long, Long> forkedComponent, HashMap<Long, Long> forkedService, HashMap<Long, Long> forkedDatasource) throws Exception {
        logger.info("Start fork ExperimentGraph,sourceGraphId: {}",sourceGraphId);
        Graph sourceGraph = graphDAO.findById(sourceGraphId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(sourceGraphId)));
        GraphVO sourceGraphVO = graphVOEntityMapper.graph2GraphVO(sourceGraph);
        User targetUser = userDAO.findByUsername(username);

        //  1. 组织新的nodelist
        List<Node> nodeList = sourceGraphVO.getNodeList();
        List<HashMap<String, Object>> newNodeList = new ArrayList<>();
        for (Node sourceNode : nodeList) {
            HashMap<String, Object> newNode = buildNewNode(sourceNode, targetUser, forkedComponent, forkedService, forkedDatasource);
            ;
            newNodeList.add(newNode);
        }

        //  2. 组织新的experimentGraph
        Graph finalTargetGraph = buildNewGraph(targetUser, newNodeList, null, null, sourceGraph, forkedComponent, forkedService, forkedDatasource);
        return finalTargetGraph;
    }

    /**
     * fork service
     *
     * @param sourceServiceId 被fork的serviceId
     * @param username 当前用户
     * @param forkedComponent 这一次fork当中已经fork过的组件
     * @return fork后的新service
     * @throws Exception
     */

    @Transactional
    public aizoo.domain.Service forkService(Long sourceServiceId, String username, HashMap<Long, Long> forkedComponent, HashMap<Long, Long> forkedService, HashMap<Long, Long> forkedDatasource) throws Exception {
        logger.info("forkService start...");
        aizoo.domain.Service targetService = new aizoo.domain.Service();
        User targetUser = userDAO.findByUsername(username);

        //  1. sourceservice的forkBy更新
        aizoo.domain.Service sourceService = serviceDAO.findById(sourceServiceId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(sourceServiceId)));
        List<aizoo.domain.Service> list = sourceService.getForkBy();
        list.add(targetService);
        sourceService.setForkBy(list);
        serviceDAO.save(sourceService);
        logger.info("setForkBy....");

        Graph sourceGraph = sourceService.getGraph();
        GraphVO sourceGraphVO = graphVOEntityMapper.graph2GraphVO(sourceGraph);

        //  2. 组织新的nodelist
        List<Node> nodeList = sourceGraphVO.getNodeList();
        List<HashMap<String, Object>> newNodeList = new ArrayList<>();
        for (Node sourceNode : nodeList) {
            HashMap<String, Object> newNode = buildNewNode(sourceNode, targetUser, forkedComponent, forkedService, forkedDatasource);
            newNodeList.add(newNode);
        }
        logger.info("buildNewNode....");

        //  3. 组织新的graph
        Graph finalTargetGraph = buildNewGraph(targetUser, newNodeList, targetService, null, sourceGraph, forkedComponent, forkedService, forkedDatasource);
        graphDAO.save(finalTargetGraph);
        logger.info("buildNewGraph...");


        //  4. 组织新的service
        targetService = buildNewService(targetService, sourceService, targetUser);
        targetService.setGraph(finalTargetGraph);
        //  1) 补充service信息filelist
        logger.info("service input len={}", finalTargetGraph.getService().getInputs().size());
        logger.info("service output len={}", finalTargetGraph.getService().getOutputs().size());
        HashMap<String, String> fileList = buildNewServiceFileList(finalTargetGraph, targetService);
        ObjectMapper objectMapper = new ObjectMapper();
        targetService.setFileList(objectMapper.writeValueAsString(fileList));
        serviceDAO.save(targetService);
        logger.info("组织新的service成功");


        //  5. 翻译service，生成文件
        logger.info("targetService namespace={}", targetService.getNamespace().getNamespace());
        logger.info(targetService.getNamespace().getNamespace().replaceAll("\\.", Matcher.quoteReplacement(File.separator)));
        String nsPath = targetService.getNamespace().getNamespace().replaceAll("\\.", Matcher.quoteReplacement(File.separator));
        logger.info("nsPath={}", nsPath);
        String targetPath = Paths.get(file_path, nsPath).toString();
        logger.info("nsPath={}", nsPath);
        translationService.translateService(finalTargetGraph, targetPath, null);

        //  6. 拷贝它的缩略图
        copyPicturePNG(sourceServiceId, sourceGraph, username, finalTargetGraph, "SERVICE");

        //  6. 将此service的id添加到内存中，避免重复fork
        forkedService.put(sourceServiceId, targetService.getId());
        return targetService;
    }

    /**
     * fork 应用图
     *
     * @param applicationId 被fork的applicationId
     * @param username 当前用户
     * @param forkedComponent 这一次fork过程中已经fork过的组件
     * @return fork之后的新application
     * @throws Exception
     */
    @Transactional
    public Graph forkApplication(Long applicationId, String username, HashMap<Long, Long> forkedComponent, HashMap<Long, Long> forkedService, HashMap<Long, Long> forkedDatasource) throws Exception {
        logger.info("forkApplication--id={}", applicationId);
        Graph sourceGraph = graphDAO.findById(applicationId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(applicationId)));
        GraphVO sourceGraphVO = graphVOEntityMapper.graph2GraphVO(sourceGraph);
        User targetUser = userDAO.findByUsername(username);

        //  组织新的nodelist
        List<Node> nodeList = sourceGraphVO.getNodeList();
        List<HashMap<String, Object>> newNodeList = new ArrayList<>();
        for (Node sourceNode : nodeList) {
            logger.info("forkApplication--sourceNode={}", sourceNode.getId());
            HashMap<String, Object> newNode = buildNewNode(sourceNode, targetUser, forkedComponent, forkedService, forkedDatasource);
            ;
            newNodeList.add(newNode);
        }

        //  组织新的graph
        Graph finalTargetGraph = buildNewGraph(targetUser, newNodeList, null, null, sourceGraph, forkedComponent, forkedService, forkedDatasource);
        graphDAO.save(finalTargetGraph);

        return finalTargetGraph;
    }

    /**
     * 删除用户上传的组件
     * @param id
     * @throws Exception
     */
    @Transactional
    public void deleteUploadComponent(Long id) throws Exception {
        logger.info("Start Delete upload Component, ComponentId: {}", id);
        Component component = componentDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
//        删除该组件的所有文件
        ComponentUtil.removeComponentFiles(component);
//        删除用户上传的组件，无对应的graph，但有对应的namespace
        component.setNamespace(null);
//        删除inputs
        List<ComponentInputParameter> componentInputParameters = component.getInputs();
        for (ComponentInputParameter componentInputParameter : componentInputParameters) {
            componentInputParameter.setComponent(null);
            componentInputParameterDAO.delete(componentInputParameter);
        }
//        删除outputs
        List<ComponentOutputParameter> componentOutputParameters = component.getOutputs();
        for (ComponentOutputParameter componentOutputParameter : componentOutputParameters) {
            componentOutputParameter.setComponent(null);
            componentOutputParameterDAO.delete(componentOutputParameter);
        }
//        将fork过这个组件的对应组件的forkFrom字段设为null
        List<Component> componentList = component.getForkBy();
        if (componentList != null) {
            for (Component component1 : componentList) {
                component1.setForkFrom(null);
                componentDAO.save(component1);
            }
        }
//        删除对应project关系
        List<Project> projects = component.getProjects();
        List<Long[]> removeList = new ArrayList<>();
        for (Project project : projects) {
            removeList.add(new Long[]{project.getId(), component.getId()});
        }
        projectService.removeProjectComponentRelation(removeList);
        componentDAO.delete(component);
        //删除es对应索引
        Optional<ElasticComponent> optional = componentRepository.findById(component.getId().toString());
        if(optional.isPresent())
            componentRepository.delete(optional.get());
    }

    /**
     * 新建一个组件
     * @param componentVO 前端传来的componentVO
     * @return 返回新建的component
     * @throws Exception
     */
    @Transactional(rollbackFor = Exception.class)
    public String buildComponent(ComponentVO componentVO) throws Exception {
        Component component = ComponentVOEntityMapper.MAPPER.componentVO2Component(componentVO, datatypeDAO, componentDAO, namespaceDAO, userDAO);//有组件名字和版本
        String componentVersion = component.getComponentVersion();
        if (componentVersion != null) {
            componentVersion = component.getComponentVersion().replace(".", "_");
        }
        // 给每个新增的组件的output都加一个输出为self
        ComponentOutputParameter selfOutput = new ComponentOutputParameter();
        selfOutput.setIsSelf(true);
        selfOutput.setParameter(new Parameter(AizooConstans.SELF_NAME, AizooConstans.SELF_TITLE, AizooConstans.SELF_DESCRIPTION, AizooConstans.SELF_ORIGIN_NAME, datatypeDAO.findByName(AizooConstans.AIZOO_UNKNOWN)));
        component.getOutputs().add(selfOutput);

//        set component的path信息
        String filename = component.getName() + componentVersion + ".py";
        String nsPath = componentVO.getNamespace().replaceAll("\\.", Matcher.quoteReplacement(File.separator));
        String targetPath = Paths.get(file_path, nsPath).toString();
        component.setPath(Paths.get(targetPath, filename).toString().replace("\\", "/"));

        Map<String, String> filePathMap = new HashMap<>();
//        set input output的对应关系
        for (ComponentInputParameter componentInputParameter : component.getInputs()) {
            componentInputParameter.setComponent(component);
        }
        for (ComponentOutputParameter componentOutputParameter : component.getOutputs()) {
            componentOutputParameter.setComponent(component);
        }
//        set title composed graph version 信息
        component.setTitle(component.getName());
        component.setComposed(false);
        component.setGraph(null);
        component.setComponentVersion(componentVO.getComponentVersion());
        ObjectMapper objectMapper = new ObjectMapper();
//        set filelist信息
        try {
            FileUtil.buildFile(targetPath, filename);
            filePathMap.put(filename, component.getPath());
            String fileList = objectMapper.writeValueAsString(filePathMap);
            component.setFileList(fileList);
            componentDAO.save(component);
        } catch (Exception e) {
            logger.error("Build Component Failed!,componentVO: {}, Exception: {}", componentVO.toString(), e);
            e.printStackTrace();
            //若数据库更新失败，则删除目录下本次新建的文件
            for (String key : filePathMap.keySet()) {
                File f = new File(filePathMap.get(key));
                FileUtil.deleteFile(f);
            }
            throw e;
        }
        String path = NotebookUtils.getComponentNotebookPath(component.getNamespace(), filename);
        return path;
    }

    /**
     * 返回修改组件路径
     * @param id 组件id
     * @return 组件路径
     */


    public String modifyComponent(long id) throws Exception{
        Component component = componentDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
        String componentVersion = component.getComponentVersion();
        if (componentVersion != null)
            componentVersion = componentVersion.replace(".", "_");
        else
            componentVersion = "";  // 说明组件没有版本信息，则不需要再文件名后加版本名
        String filename = component.getName() + componentVersion + ".py";
        Namespace namespace = component.getNamespace();
        String path = NotebookUtils.getComponentNotebookPath(namespace, filename);
        return path;
    }

    /**
     * 修改上传管理页面用户拥有的组件的描述信息
     * @param componentVO 前端传来的componentVO
     * @throws JsonProcessingException
     */
    @Transactional
    public void updateDesc(ComponentVO componentVO) throws JsonProcessingException {
//        更新description
        componentDAO.updateDesc(componentVO.getId(), componentVO.getDescription());
//        更新inputs outputs
        List<Map<String, Object>> maps = componentVO.getInputs();
        for (Map<String, Object> componentInputParameter : maps) {
            Object id = componentInputParameter.get("id");
            String desc = (String) componentInputParameter.get("description");
            componentInputParameterDAO.updateDesc(Long.valueOf(String.valueOf(id)), desc);
        }
        for (Map<String, Object> componentOutputParameter : componentVO.getOutputs()) {
            Object id = componentOutputParameter.get("id");
            String desc = (String) componentOutputParameter.get("description");
            componentOutputParameterDAO.updateDesc(Long.valueOf(String.valueOf(id)), desc);
        }
//        更新properties example
        Map<String, Object> properties = componentVO.getProperties();
        ObjectMapper objectMapper = new ObjectMapper();
        Component component = componentDAO.findById(componentVO.getId()).orElseThrow(() -> new EntityNotFoundException(String.valueOf(componentVO.getId())));
        component.setProperties(objectMapper.writeValueAsString(properties));
        component.setExample(componentVO.getExample());
        componentDAO.save(component);
    }

    /**
     * 获取图中所有节点的代码路径
     * @param graph 图信息
     * @return 图中所有节点的Path
     * @throws JsonProcessingException
     */
    public Set getComponentFilePaths(Graph graph) throws JsonProcessingException {
        GraphVO graphVO = GraphVOEntityMapper.MAPPER.graph2GraphVO(graph);
        ObjectMapper objectMapper = new ObjectMapper();
        Set<String> componentFilePaths = new HashSet<>();
        for (Node node : graphVO.getNodeList()) { //原子组件的存放路径和文件名
            if (NodeType.isComponentType(node.getComponentType())
                    && node.getComponentType() != NodeType.DATASOURCE
                    && node.getComponentType() != NodeType.PARAMETER
                    && node.getComponentType() != NodeType.OPERATOR_TEMPLATE
                    && node.getComponentType() != NodeType.OPERATOR

            ) {
                Component nodeComponent = componentDAO.findById(node.getComponentVO().getId()).orElseThrow(() -> new EntityNotFoundException("no component entity found"));
                if (nodeComponent.getFileList() == null) {
                    // 将子节点path添加
                    componentFilePaths.add(nodeComponent.getPath());
                } else {
                    // 保存子节点fileList中的所有数据
                    Map<String, String> fileList = objectMapper.readValue(nodeComponent.getFileList(), new TypeReference<Map<String, String>>() {
                    });
                    componentFilePaths.addAll(fileList.values());
                }
            } else if (node.getComponentType() == NodeType.VISUALCONTAINER) {
                // 可视化容器路径的获取
                VisualContainer container = visualContainerDAO.findById(node.getVisualContainerVO().getId()).orElseThrow(() -> new EntityNotFoundException("no visualContainer entity found"));
                if (container.getPath() != null)
                    componentFilePaths.add(container.getPath());
                if (container.getTemplatePath() != null)
                    componentFilePaths.add(container.getTemplatePath());
            } else if (node.getComponentType() == NodeType.SERVICE) {
                // service路径的获取
                String servicePath = Paths.get(file_path, serviceTemplatePath).toString();
                componentFilePaths.add(servicePath);
            }
        }
        return componentFilePaths;
    }
}
