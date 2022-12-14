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
     * ??????????????????
     * @param fileInfo ????????????
     * @param componentVO ????????????
     * @throws Exception
     */
    @Transactional
    public void uploadComponent(TFileInfoVO fileInfo, ComponentVO componentVO) throws Exception {
        logger.info("Start upload Component");
        Component component = ComponentVOEntityMapper.MAPPER.componentVO2Component(componentVO, datatypeDAO, componentDAO, namespaceDAO, userDAO);//????????????????????????
        logger.info("fileInfo: {}",fileInfo.toString());
        logger.info("componentVO: {}",componentVO.toString());


        String componentVersion = component.getComponentVersion();
        if (componentVersion != null) {
            componentVersion = component.getComponentVersion().replace(".", "_");
        }
        // ???????????????????????????output?????????????????????self
        ComponentOutputParameter selfOutput = new ComponentOutputParameter();
        selfOutput.setIsSelf(true);
        selfOutput.setParameter(new Parameter(AizooConstans.SELF_NAME, AizooConstans.SELF_TITLE, AizooConstans.SELF_DESCRIPTION, AizooConstans.SELF_ORIGIN_NAME, datatypeDAO.findByName(AizooConstans.AIZOO_UNKNOWN)));
        component.getOutputs().add(selfOutput);
//      set?????????path
        String filename = component.getName() + componentVersion + ".py";
        String file = Paths.get(file_path, componentVO.getUsername(), "temp", fileInfo.getUniqueIdentifier(), filename).toString();
        String tempFolder = Paths.get(file_path, componentVO.getUsername(), "temp", fileInfo.getUniqueIdentifier()).toString();
        String nsPath = componentVO.getNamespace().replaceAll("\\.", Matcher.quoteReplacement(File.separator));
        String targetPath = Paths.get(file_path, nsPath).toString();
        logger.info("upload Component filePath: {}", targetPath);
        component.setPath(Paths.get(targetPath, filename).toString().replace("\\", "/"));
//        set?????????inputs???outputs
        Map<String, String> filePathMap = new HashMap<>();
        for (ComponentInputParameter componentInputParameter : component.getInputs()) {
            componentInputParameter.setComponent(component);
        }
        for (ComponentOutputParameter componentOutputParameter : component.getOutputs()) {
            componentOutputParameter.setComponent(component);
        }
//        set?????????title composed graph version ??????
        component.setTitle(component.getName());
        component.setComposed(false);
        component.setGraph(null);
        component.setComponentVersion(componentVO.getComponentVersion());
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            FileUtil.merge(file, tempFolder, filename);
            if (fileInfo.getName().endsWith(".zip")) {
//                ???????????????????????????????????????????????????????????????????????????
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
//            ??????????????????????????????????????????????????????????????????
            for (String key : filePathMap.keySet()) {
                File f = new File(filePathMap.get(key));
                FileUtil.deleteFile(f);
            }
            throw e;
        } finally {
//            ?????????????????????????????????????????????
            String tempPath = Paths.get(file_path, componentVO.getUsername(), "temp", fileInfo.getUniqueIdentifier()).toString();
            FileUtils.deleteDirectory(new File(tempPath));
            logger.info("Delete Component temp folder and file");
        }
    }



    /**
     * ??????????????????????????????
     * @param privacy ??????????????????
     * @param username ???????????????
     * @param type ????????????
     * @param keyword ??????????????????
     * @return
     */
    public List<Component> getComponentByKeyword(String privacy, String username, String type, String keyword) {
        logger.info("Start find Component By Keyword, username: {}, keyword: {}", username, keyword);
//        ????????????????????????????????????????????????????????????????????????????????????fork????????????
        List<Component> componentList;
        if (privacy.equals("public")) {
            componentList = componentDAO.findByComponentTypeAndPrivacyAndUserUsernameNotAndTitleLikeAndPathIsNotNull(ComponentType.valueOf(type), privacy, username, "%" + keyword + "%");
//            ????????????fork????????????
            //          componentList.removeIf(component -> componentDAO.existsByForkFromAndUserUsername(component, username));
        }
//        ?????????????????????????????????
        else {
            componentList = componentDAO.findByComponentTypeAndUserUsernameAndTitleLikeAndPathIsNotNull(ComponentType.valueOf(type), username, "%" + keyword + "%");
        }
        logger.info("End find Component By Keyword");
        return componentList;
    }




    /**
     * ????????????????????????????????????
     * @param username ???????????????
     * @param keyword ??????????????????
     * @param type ????????????
     * @return
     */
    public HashMap<String, List<Object>> getAllComponentByKeyword(String username, String keyword, String type) {
//        type??????MODULE,MODEL,LOSS,JOB,SERVICE,APPLICATION
//        ????????????????????????????????????????????????????????????????????????component
        HashMap<String, List<Object>> resultList = new HashMap<>();
        if (EnumUtils.isValidEnum(GraphType.class, type)) {
//            JOB??????component??? ???MODULE PARAMETER????????????????????????datasource???
            if (GraphType.JOB == GraphType.valueOf(type)) {
//            ????????????????????????????????????????????????????????????????????????????????????fork????????????,?????????MODULE PARAMETER??????
                List<Component> publicComponentList = componentDAO.findByComponentTypeIsNotInAndPrivacyAndUserUsernameNotAndTitleLikeAndPathIsNotNull(Arrays.asList(ComponentType.MODULE, ComponentType.PARAMETER, ComponentType.FUNCTION), "public", username, "%" + keyword + "%");
//            ????????????fork????????????
                //             publicComponentList.removeIf(component -> componentDAO.existsByForkFromAndUserUsername(component, username));
//            ????????????????????????fork ???????????????????????????
                List<Component> privateComponentList = componentDAO.findByComponentTypeIsNotInAndPrivacyAndUserUsernameAndTitleLikeAndPathIsNotNull(Arrays.asList(ComponentType.MODULE, ComponentType.PARAMETER, ComponentType.FUNCTION), "private", username, "%" + keyword + "%");
                List<Datasource> privateDatasource = datasourceDAO.findByUserUsernameAndTitleLikeAndPrivacy(username, "%" + keyword + "%", "private");
                resultList.put("public", ListEntity2ListVO.component2ComponentVOObject(publicComponentList));
                resultList.put("private", ListEntity2ListVO.componentAndDatasource2ComponentVOAndDatasourceVOObject(privateComponentList, privateDatasource));
            }
//            SERVICE???component??? ??????PROCESS MODEL FUNCTION PARAMETER
            else if (GraphType.SERVICE == GraphType.valueOf(type)) {
//            ????????????????????????????????????????????????????????????????????????????????????fork????????????,??????PROCESS MODEL FUNCTION PARAMETER
                List<Component> publicComponentList = componentDAO.findByComponentTypeInAndPrivacyAndUserUsernameNotAndTitleLikeAndPathIsNotNull(Arrays.asList(ComponentType.PROCESS, ComponentType.MODEL, ComponentType.FUNCTION, ComponentType.PARAMETER), "public", username, "%" + keyword + "%");
//            ????????????fork????????????
//                publicComponentList.removeIf(component -> componentDAO.existsByForkFromAndUserUsername(component, username));
//            ?????????????????????????????????,??????PROCESS MODEL FUNCTION PARAMETER
                List<Component> privateComponentList = componentDAO.findByComponentTypeInAndPrivacyAndUserUsernameAndTitleLikeAndPathIsNotNull(Arrays.asList(ComponentType.PROCESS, ComponentType.MODEL, ComponentType.FUNCTION, ComponentType.PARAMETER), "private", username, "%" + keyword + "%");
                resultList.put("public", ListEntity2ListVO.component2ComponentVOObject(publicComponentList));
                resultList.put("private", ListEntity2ListVO.component2ComponentVOObject(privateComponentList));
            }
//        APPLICATION???container component??? ??????PROCESS FUNCTION DATALOADER service??? datasource???
            else if (GraphType.APPLICATION == GraphType.valueOf(type)) {
//            ????????????????????????????????????????????????????????????????????????????????????fork????????????,??????PROCESS FUNCTION DATALOADER
                List<Component> publicComponentList = componentDAO.findByComponentTypeInAndPrivacyAndUserUsernameNotAndTitleLikeAndPathIsNotNull(Arrays.asList(ComponentType.PROCESS, ComponentType.FUNCTION, ComponentType.DATALOADER), "public", username, "%" + keyword + "%");
//            ????????????fork????????????
//                publicComponentList.removeIf(component -> componentDAO.existsByForkFromAndUserUsername(component, username));
//            ?????????????????????????????????,??????PROCESS FUNCTION DATALOADER
                List<Component> privateComponentList = componentDAO.findByComponentTypeInAndPrivacyAndUserUsernameAndTitleLikeAndPathIsNotNull(Arrays.asList(ComponentType.PROCESS, ComponentType.FUNCTION, ComponentType.DATALOADER), "private", username, "%" + keyword + "%");
//            ?????????????????????????????????????????????????????????fork ?????????????????????fork???
                List<VisualContainer> visualContainerList = visualContainerDAO.findByTitleLike("%" + keyword + "%");
//            service???????????????
                List<aizoo.domain.Service> serviceList = serviceDAO.findByUserUsernameAndTitleLikeAndPrivacy(username, "%" + keyword + "%", "private");
//            datasource???????????????
                List<Datasource> datasourceList = datasourceDAO.findByUserUsernameAndTitleLikeAndPrivacy(username, "%" + keyword + "%", "private");
                resultList.put("public", ListEntity2ListVO.component2ComponentVOObject(publicComponentList));
                resultList.put("private", ListEntity2ListVO.componentAndVisualContainerAndServiceAndDatasource2ComponentVOAndVisualContainerAndServiceVOAndDatasourceVOObject(privateComponentList, visualContainerList, serviceList, datasourceList));
            }
        } else if (EnumUtils.isValidEnum(ComponentType.class, type)) {  // type??? MODULE/MODEL/LOSS???
            ComponentType componentType = ComponentType.valueOf(type);
            List<ComponentType> componentTypes = null;
            if (ComponentType.MODEL == componentType || ComponentType.MODULE == componentType)
                componentTypes = USED_FOR_MODULE_OR_MODEL_GRAPH;
            else
                componentTypes = USED_FOR_LOSS_GRAPH;
//            ????????????????????????????????????????????????????????????????????????????????????fork????????????
            List<Component> publicComponentList = componentDAO.findByPrivacyAndUserUsernameNotAndTitleLikeAndPathIsNotNullAndComponentTypeIn("public", username, "%" + keyword + "%", componentTypes);
//            ????????????fork????????????
//            publicComponentList.removeIf(component -> componentDAO.existsByForkFromAndUserUsername(component, username));
//            ?????????????????????????????????
            List<Component> privateComponentList = componentDAO.findByUserUsernameAndTitleLikeAndPathIsNotNullAndComponentTypeIn(username, "%" + keyword + "%", componentTypes);
            resultList.put("public", ListEntity2ListVO.component2ComponentVOObject(publicComponentList));
            resultList.put("private", ListEntity2ListVO.component2ComponentVOObject(privateComponentList));
        }
        return resultList;
    }

    /***
     * ????????????????????????????????????
     * @param username ????????????????????????
     * @param privacy     public/private
     *                 ??? public???????????????????????????private??????????????????
     * @param type     ????????????
     * @return ??????????????????????????????????????????
     */
    public List<Component> getComponentByType(String username, String privacy, String type) {
        logger.info("Start Find Component By Type,username: {}, type: {}", username, type);
//       ???????????????????????????public???????????????????????????????????????????????????????????????????????????fork??????????????????private?????????????????????????????????????????????
//       ??????????????????????????????
        List<Component> componentList = null;
        if (type.equals("OPERATOR_TEMPLATE")) {// ????????????????????????????????????????????????????????????list????????????list??????????????????input???????????????????????????
            componentList = componentDAO.findByComponentType(ComponentType.OPERATOR_TEMPLATE);
            return componentList;
        }
        if (privacy.equals("public")) {
            componentList = componentDAO.findByComponentTypeAndUserUsernameNotAndPrivacyAndPathIsNotNull(ComponentType.valueOf(type), username, privacy);
//                ????????????fork????????????
            componentList.removeIf(component -> componentDAO.existsByForkFromAndUserUsername(component, username));
        }
//            ??????????????????????????????
        else if (privacy.equals("private")) {
            componentList = componentDAO.findByComponentTypeAndUserUsernameAndPathIsNotNull(ComponentType.valueOf(type), username);
        } else if (StringUtil.isNullOrEmpty(privacy)) {
            componentList = componentDAO.findByComponentType(ComponentType.PARAMETER);
        }
        return componentList;
    }


    /**
     * ??????????????????????????????????????????fork??????
     *
     * @param sourceId ??????id????????????id????????????????????????id
     * @param type ????????????
     * @return true/false ????????????fork
     */
    public boolean allowedFork(Long sourceId, String type) {
        if (type.equals("SERVICE")) {
            if (!serviceDAO.existsById(sourceId)) {//?????????service????????????
                return false;
            }
            aizoo.domain.Service service = serviceDAO.findById(sourceId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(sourceId)));
            Long sourceGraphId = service.getGraph().getId();
            if (!graphDAO.existsById(sourceGraphId)) {//????????????????????????
                return false;
            }
            Graph sourceGraph = graphDAO.findById(sourceGraphId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(sourceGraphId)));
            GraphVO sourceGraphVO = graphVOEntityMapper.graph2GraphVO(sourceGraph);
            List<Node> nodeList = sourceGraphVO.getNodeList();
            if (nodeList != null) {
                for (Node node : nodeList) {//???????????????nodelist??????COMPONENT????????????????????????
                    if ((node.getComponentType().toString() != ("DATASOURCE")) && (node.getComponentType().toString() != ("PARAMETER"))) {
                        if (!allowedFork(node.getComponentVO().getId(), "COMPONENT")) {
                            return false;//??????????????????????????????????????????false?????????????????????
                        }
                    }
                }
            }
            return true;
        } else if (type.equals("COMPONENT")) {
            if (!componentDAO.existsById(sourceId)) {//???????????????????????????
                return false;
            }
            Component sourceComponent = componentDAO.findById(sourceId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(sourceId)));
            if (sourceComponent.isComposed()) {//???????????????????????????????????????????????????
                Long sourceGraphId = sourceComponent.getGraph().getId();
                if (!graphDAO.existsById(sourceGraphId)) {//?????????????????????
                    return false;
                }
                Graph sourceGraph = graphDAO.findById(sourceGraphId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(sourceGraphId)));
                GraphVO sourceGraphVO = graphVOEntityMapper.graph2GraphVO(sourceGraph);
                List<Node> nodeList = sourceGraphVO.getNodeList();
                if (nodeList != null) {
                    for (Node node : nodeList) {//???????????????nodelist??????COMPONENT????????????????????????
                        if (node.getComponentType().toString() != ("DATASOURCE")) {
                            if (!allowedFork(node.getComponentVO().getId(), "COMPONENT")) {
                                return false;
                            }
                        }
                    }
                }
            }
            return true;
        } else if (type.equals("JOB")) {     //???????????????
            if (!graphDAO.existsById(sourceId)) {//?????????????????????
                return false;
            }
            Graph sourceGraph = graphDAO.findById(sourceId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(sourceId)));
            GraphVO sourceGraphVO = graphVOEntityMapper.graph2GraphVO(sourceGraph);
            List<Node> nodeList = sourceGraphVO.getNodeList();
            if (nodeList != null) {
                for (Node node : nodeList) {//???????????????nodelist??????COMPONENT????????????????????????
                    if (node.getComponentType().toString() != ("DATASOURCE")) {
                        if (!allowedFork(node.getComponentVO().getId(), "COMPONENT")) {
                            return false;
                        }
                    }
                }
            }
            return true;
        } else if (type.equals("APPLICATION")) {
            if (!graphDAO.existsById(sourceId)) {//?????????????????????
                return false;
            }
            Graph sourceGraph = graphDAO.findById(sourceId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(sourceId)));
            GraphVO sourceGraphVO = graphVOEntityMapper.graph2GraphVO(sourceGraph);
            List<Node> nodeList = sourceGraphVO.getNodeList();
            if (nodeList != null) {
                for (Node node : nodeList) {//???????????????nodelist??????COMPONENT????????????????????????
                    if ((node.getComponentType().toString() != ("DATASOURCE")) && (node.getComponentType().toString() != ("VISUALCONTAINER"))) {
                        if (node.getComponentType().toString().equals("SERVICE")) {//??????node???service??????
                            if (!allowedFork(node.getServiceVO().getId(), "SERVICE")) {
                                return false;
                            }
                        } else {//????????????COMPONENT??????
                            if (!allowedFork(node.getComponentVO().getId(), "COMPONENT")) {
                                return false;
                            }
                        }
                    }
                }
            }
            //???????????????nodelist??????????????????true
            return true;
        } else {//??????????????????????????????????????????fork
            return false;
        }
    }

    /**
     * ???????????????????????????fork?????????????????????component???nodelist???
     *
     * @param childComponent1 ??????????????????
     * @param sourceVO ??????????????????componentVO
     * @return ?????????????????????????????????
     * @throws JsonProcessingException
     */
    public Component inputOutputRenameFromGraph(Component childComponent1, ComponentVO sourceVO) throws JsonProcessingException {
        logger.info("Start Rename input output,sourceComponentId: {}",sourceVO.getId());
        Component sourceNodeComponent = componentDAO.findById(sourceVO.getId()).orElseThrow(() -> new EntityNotFoundException(String.valueOf(sourceVO.getId())));
        Component sourceComponent1 = new Component();
        BeanUtils.copyProperties(sourceNodeComponent, sourceComponent1);
//        set title properties discription input output ??????
        childComponent1.setTitle(sourceVO.getTitle());
        ObjectMapper objectMapper = new ObjectMapper();
        String properties = objectMapper.writeValueAsString(sourceVO.getProperties());
        childComponent1.setProperties(properties);
        childComponent1.setDescription(sourceVO.getDescription());
        List<Map<String, Object>> sourceInputList = sourceVO.getInputs();
        List<Map<String, Object>> sourceOutputList = sourceVO.getOutputs();
        List<ComponentInputParameter> newInputList = childComponent1.getInputs();
        List<ComponentOutputParameter> newOutputList = childComponent1.getOutputs();
//        ???newList?????????sourceList???????????????????????????title?????????????????????????????????????????????????????????node???input output ????????????????????????

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
     * fork?????????????????????????????????
     * @param targetUser ????????????
     * @param finalTargetComponent ??????????????????
     * @param sourceComponent ???fork?????????
     * @param isFirstAtom ??????????????????fork?????????????????????????????????
     * @return
     */
    public Component buildNewComponent(User targetUser, Component finalTargetComponent, Component sourceComponent,
                                       boolean isFirstAtom) {

        logger.info("buildNewComponent start,sourceComponentid={},  finalTargetComponentid={}", sourceComponent.getId(), finalTargetComponent.getId());
        Long sourceComponentId = sourceComponent.getId();
        String targetNamespace = "";
//          ?????????????????? username.type.version(fork+forkfromuser+number).forkfrom???????????????
        targetNamespace = namespaceService.forkRegisterNamespace(sourceComponentId, isFirstAtom, targetUser, "COMPONENT");
        Namespace namespace = namespaceDAO.findByNamespace(targetNamespace);

        logger.info("buildNewComponent forkRegisterNamespace complete,sourceComponentid={},  finalTargetComponentid={}", sourceComponent.getId(), finalTargetComponent.getId());
//        set ??????????????????????????????
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

//      set??????input???output
        finalTargetComponent.setInputs(buildNewInputList(sourceComponent, finalTargetComponent));
        logger.info("buildNewComponent setinputs complete,sourceComponentid={},  finalTargetComponentid={}", sourceComponent.getId(), finalTargetComponent.getId());

        finalTargetComponent.setOutputs(buildNewOutputList(sourceComponent, finalTargetComponent));
        logger.info("buildNewComponent setoutputs complete,sourceComponentid={},  finalTargetComponentid={}", sourceComponent.getId(), finalTargetComponent.getId());
//        finalTargetComponent.setGraph(targetGraph);
        componentDAO.save(finalTargetComponent);
        return finalTargetComponent;
    }

    /**
     * ??????fork????????????nodelist
     *
     * @param sourceNode ???fork?????????node
     * @param targetUser ????????????
     * @param forkedComponent ????????????fork?????????fork????????????
     * @return ??????????????????node
     * @throws IOException
     */
    public HashMap<String, Object> buildNewNode(Node sourceNode, User targetUser, HashMap<Long, Long> forkedComponent, HashMap<Long, Long> forkedService, HashMap<Long, Long> forkedDatasource) throws Exception {
        Component childComponent = null;
        Datasource childDatasource = null;
        aizoo.domain.Service childService = null;
        //  fork???????????????????????????
        String componentPrivacy = "private";
        boolean isFirstAtom = false;
        String username = targetUser.getUsername();
        logger.info("buildNewNode start... id={}", sourceNode.getId());
        if (sourceNode.getComponentType().toString().equals("DATASOURCE")) {
            //  1. ?????????datasource?????????????????????fork
            //????????????fork??????
            if (forkedDatasource.containsKey(sourceNode.getDatasourceVO().getId()) && forkedDatasource.get(sourceNode.getDatasourceVO().getId()) != null) {
                Long id = forkedDatasource.get(sourceNode.getDatasourceVO().getId());
                childDatasource = datasourceDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
            } else {
                Datasource sourceDatasource = datasourceVOEntityMapper.datasourceVO2Datasource(sourceNode.getDatasourceVO(), datasourceDAO, namespaceDAO, userDAO, datatypeDAO);
                childDatasource = datasourceService.forkDatasource(targetUser, sourceDatasource);
                forkedDatasource.put(sourceDatasource.getId(), childDatasource.getId());
            }
        } else if (sourceNode.getComponentType().toString().equals("PARAMETER") || sourceNode.getComponentType().toString().equals("OPERATOR")) {
            //  2. ?????????parameter???operator?????????????????????nodelist????????????????????????fork????????????????????????????????????????????????fork???
            Long sourceId = sourceNode.getComponentVO().getId();
            childComponent = componentDAO.findById(sourceId).orElseThrow(() ->
                    new EntityNotFoundException(String.valueOf(sourceId)));
            forkedComponent.put(sourceId, sourceId);
        } else if (sourceNode.getComponentType().toString().equals("SERVICE")) {
            //  3. ?????????service
            try {
                childService = forkService(sourceNode.getServiceVO().getId(), username, forkedComponent, forkedService, forkedDatasource);
            } catch (Exception e) {
                e.printStackTrace();
            }
            childComponent = null;
        } else if (sourceNode.getComponentType().toString().equals("VISUALCONTAINER")) {
            //  4. ???????????????????????? ????????????
            childComponent = null;
        } else {
            //  5. ????????????????????????????????????fork
            boolean flag = true;
            if (forkedComponent.containsKey(sourceNode.getComponentVO().getId()) && forkedComponent.get(sourceNode.getComponentVO().getId()) != null) {
                //  fork??????????????????target??????null,????????????fork
                flag = false;
            }
            if (flag) {
                //  ????????????description??????
                Component c = newFork(targetUser, sourceNode.getComponentVO().getId(), componentPrivacy, null, isFirstAtom, forkedComponent, forkedService, forkedDatasource);
                childComponent = componentDAO.findById(c.getId()).orElseThrow(() -> new EntityNotFoundException(String.valueOf(c.getId())));
            } else {
                //  ????????????fork??????????????????????????????value????????????childComponent
                Long id = forkedComponent.get(sourceNode.getComponentVO().getId());
                childComponent = componentDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
            }
        }
        HashMap<String, Object> newNode = new HashMap<>();
        //  set node?????????
        newNode.put("componentType", sourceNode.getComponentType());
        newNode.put("componentVersion", sourceNode.getComponentVersion());
        newNode.put("checkPointId", sourceNode.getCheckPointId());
        newNode.put("dataloaderType", sourceNode.getDataloaderType());
        newNode.put("serviceJobId", sourceNode.getServiceJobId());
        newNode.put("parameterName", sourceNode.getParameterName());
        //  ????????????nodelist??????component????????????????????????vo
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
        if (sourceNode.getDatasourceVO() != null) { //?????????fork???node???datasource????????????
            newNode.put("datasource", childDatasource);
        } else {
            newNode.put("datasource", null);
        }

        newNode.put("service", childService);
        newNode.put("variable", sourceNode.getVariable());
        newNode.put("id", sourceNode.getId());
        VisualContainerVO containerVO = sourceNode.getVisualContainerVO();
        VisualContainer nodeContainer = null;
        if (containerVO != null) // ?????????fork???node???containerVO????????????
            nodeContainer = daoUtil.findVisualContainerById(containerVO.getId());
        newNode.put("visualContainer", nodeContainer);
//        saveOutput???????????????
        newNode.put("saveOutput", null);
        return newNode;
    }


    /**
     * ??????fork??????Graph
     *
     * @param targetUser ????????????
     * @param newNodeList ?????????????????????nodelist
     * @param targetService ?????????service??????
     * @param sourceGraph ???fork???graph
     * @param forkedComponent  ????????????fork?????????fork????????????
     * @return ???????????????graph
     * @throws JsonProcessingException
     */
    @Transactional
    public Graph buildNewGraph(User targetUser, List<HashMap<String, Object>> newNodeList, aizoo.domain.Service targetService,
                               Component targetComponent, Graph sourceGraph, HashMap<Long, Long> forkedComponent, HashMap<Long, Long> forkedService, HashMap<Long, Long> forkedDatasource) throws JsonProcessingException {
        Graph finalTargetGraph = new Graph();
//        set fork??????graph??????????????????
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
//        set graph?????????ApplitcationjobId
        finalTargetGraph.setApplications(null);
//        set graph?????????serviceId
        finalTargetGraph.setService(targetService);
        graphDAO.save(finalTargetGraph);
        return finalTargetGraph;
    }

    /**
     * ??????fork?????????service
     *
     * @param targetService ???????????????service
     * @param sourceService ???fork???service
     * @param targetUser ????????????
     * @return ???????????????service
     */
    public aizoo.domain.Service buildNewService(aizoo.domain.Service targetService, aizoo.domain.Service sourceService,
                                                User targetUser) {
//      ????????????????????????namespace
        String targetNamespace = "";
        targetNamespace = namespaceService.forkRegisterNamespace(sourceService.getId(), true, targetUser, "SERVICE");
        Namespace namespace = namespaceDAO.findByNamespace(targetNamespace);
        targetService.setNamespace(namespace);
//        set fork??????service??????????????????
        targetService.setDescription(sourceService.getDescription());
        targetService.setPrivacy("private");
        targetService.setName(sourceService.getName());
        targetService.setTitle(sourceService.getTitle());
        //  set path ?????????????????????????????????namespace??????????????????
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

        // ??????sevice??? input output
        List<ServiceInputParameter> myServiceInputParameters = buildNewInputList(sourceService, targetService);

        targetService.setInputs(myServiceInputParameters);
        List<ServiceOutputParameter> myServiceOutputParameters = buildNewOutputList(sourceService, targetService);
        targetService.setOutputs(myServiceOutputParameters);

        serviceDAO.save(targetService);
        return targetService;
    }

    /**
     * ??????forkService??????input
     *
     * @param sourceService ???fork???service
     * @param targetService fork????????????service
     * @return ???????????????inputList
     */
    public List<ServiceInputParameter> buildNewInputList(aizoo.domain.Service sourceService, aizoo.domain.Service targetService) {
        // ??????inputs
        List<ServiceInputParameter> serviceInputParameters = sourceService.getInputs();
        // ??????inputs
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
     * ??????forkService?????????output
     *
     * @param sourceService ???fork???service
     * @param targetService fork????????????service
     * @return ???????????????outputList
     */
    public List<ServiceOutputParameter> buildNewOutputList(aizoo.domain.Service sourceService, aizoo.domain.Service targetService) {
        // ??????outputs
        List<ServiceOutputParameter> myServiceOutputParameters = new ArrayList<>();
        // ??????outputs
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

        // ???????????????????????????output?????????????????????self
        ServiceOutputParameter selfOutput = new ServiceOutputParameter();
        selfOutput.setIsSelf(true);
        selfOutput.setParameter(new Parameter(AizooConstans.SELF_NAME, AizooConstans.SELF_TITLE, AizooConstans.SELF_DESCRIPTION, AizooConstans.SELF_ORIGIN_NAME, datatypeDAO.findByName(AizooConstans.AIZOO_UNKNOWN)));
        myServiceOutputParameters.add(selfOutput);
        return myServiceOutputParameters;
    }

    /**
     * ????????????forkComponent???input
     *
     * @param sourceComponent ???fork???component
     * @param finalTargetComponent fork????????????component
     * @return ???????????????inputList
     */
    public List<ComponentInputParameter> buildNewInputList(Component sourceComponent, Component finalTargetComponent) {
        // ??????inputs
        List<ComponentInputParameter> componentInputParameters = sourceComponent.getInputs();
        // ??????inputs
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
     * ????????????forkComponent???output
     *
     * @param sourceComponent ???fork???component
     * @param finalTargetComponent fork????????????component
     * @return ???????????????outputList
     */
    public List<ComponentOutputParameter> buildNewOutputList(Component sourceComponent, Component finalTargetComponent) {
        // ??????outputs
        List<ComponentOutputParameter> myComponentOutputParameters = new ArrayList<>();
        // ??????outputs
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
     * ???????????????service????????????
     *
     * @param sourceId ???fork???serviceId
     * @param sourceGraph ???fork???graph
     * @param username ????????????
     * @param targetGraph fork?????????graph
     * @param type ????????????
     * @throws IOException
     */
    public void copyPicturePNG(Long sourceId, Graph sourceGraph, String username, Graph targetGraph, String type) throws IOException {
        // ???fork??????????????????
        String sourcePictureFullPath = null;
        // fork?????????????????????
        String targetPicturePath = null;
        // ??????????????????????????????????????????
        if (type.equals("COMPONENT")) {
            Component sourceComponent = componentDAO.findById(sourceId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(sourceId)));
            String sourceUserName = sourceComponent.getUser().getUsername();
            sourcePictureFullPath = Paths.get(file_path, sourceUserName, "picture",
                    sourceComponent.getComponentType().getValue(), sourceGraph.getId() + "_" + sourceGraph.getName() + ".png").toString();//???????????????????????????
            targetPicturePath = Paths.get(file_path, username, "picture",
                    sourceComponent.getComponentType().getValue()).toString();//???????????????
        } else if (type.equals("SERVICE")) {
            aizoo.domain.Service sourceService = serviceDAO.findById(sourceId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(sourceId)));
            String sourceUserName = sourceService.getUser().getUsername();
            sourcePictureFullPath = Paths.get(file_path, sourceUserName, "picture",
                    "service", sourceGraph.getId() + "_" + sourceGraph.getName() + ".png").toString();//???????????????????????????
            targetPicturePath = Paths.get(file_path, username, "picture",
                    "service").toString();//???????????????
        }
        File sourcePictureFile = new File(sourcePictureFullPath);
        // ?????????fork?????????????????????
        if (!sourcePictureFile.exists()) {
            logger.info("?????????????????????{}", sourcePictureFullPath);
            return;
        }
        try {
            // ?????????????????????
            String myPictureSavePath = FileUtil.copyFile(sourcePictureFullPath, targetPicturePath);//????????????????????????????????????
            File myPictureFile = new File(myPictureSavePath);
            String newname = (targetGraph.getId() + "_" + targetGraph.getName() + ".png");
            File newfile = new File(myPictureFile.getParent() + File.separator + newname);
            //  ???????????????????????????
            myPictureFile.renameTo(newfile);
        } catch (FileNotFoundException e) {
            logger.error("?????????????????????,{}", e);
        }
    }

    /**
     * ??????service???filelist??????
     *
     * @param finalTargetGraph fork?????????graph
     * @param targetService ???fork???service
     * @return ????????????????????????service???filelist
     * @throws JsonProcessingException
     */
    public HashMap<String, String> buildNewServiceFileList(Graph finalTargetGraph, aizoo.domain.Service targetService) throws JsonProcessingException {
        HashMap<String, String> fileList = new HashMap<>();
        //  ?????????????????????????????????????????????????????????????????????????????????fileList???
        ObjectMapper objectMapper1 = new ObjectMapper();
        GraphVO targetGraphVO = graphVOEntityMapper.graph2GraphVO(finalTargetGraph);
        for (Node node : targetGraphVO.getNodeList()) {
            if (node.getComponentType() != NodeType.PARAMETER && node.getComponentType() != NodeType.OPERATOR && node.getComponentType() != NodeType.OPERATOR_TEMPLATE) {
                Component nodeComponent = componentDAO.findById(node.getComponentVO().getId()).orElseThrow(() -> new EntityNotFoundException(String.valueOf(node.getComponentVO().getId())));
                String nodeComponentVersion = "";
                if (nodeComponent.getComponentVersion() != null) {
                    nodeComponentVersion = nodeComponent.getComponentVersion().replace(".", "_");
                }
                //  ??????fileList
                if (nodeComponent.getFileList() == null) {
                    fileList.put(nodeComponent.getName() + nodeComponentVersion + ".py", nodeComponent.getPath());
                } else {   //   ???????????????fileList??????????????????
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
     * @param targetUser        ????????????
     * @param sourceComponentId ???fork?????????
     * @param componentPrivacy ???????????? public/private
     * @param description ????????????
     * @param isFirstAtom     ??????????????????fork?????????????????????????????????
     * @return fork??????????????????
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
                ???. ????????????
                 */
            //  1. ????????????component
            Component targetComponent = new Component();
            targetComponent.setGraph(null);
            logger.info("setGraph complete");

            targetComponent = buildNewComponent(targetUser, targetComponent, sourceComponent, isFirstAtom);
            logger.info("buildNewComponent complete");

            //  1) ??????filelist???path??????
            String targetNamespace = targetComponent.getNamespace().getNamespace();
            //  fork?????????????????????  ?????????windows?????????linux????????????????????????
            Path myForkFileDir = Paths.get(file_path, targetNamespace.split("\\."));
            logger.info("newFork atomic  myForkFileDir={}", myForkFileDir.toString());

            //  ???????????????????????????
            Path myForkPath = Paths.get(String.valueOf(myForkFileDir), sourceComponent.getPath().substring(sourceComponent.getPath().lastIndexOf("/") + 1));
            //  ????????????,forkFromPaths??????????????????myForkFilePaths???fork???????????????
            String sourceComponentFilePaths = sourceComponent.getFileList();
//            logger.info("sourceComponentFilePaths ={}", sourceComponentFilePaths);

            String myForkFilePaths = FileUtil.forkFileCopy(sourceComponentFilePaths, myForkFileDir.toString());//????????????????????????????????????
//            logger.info("forkFileCopy complete source ={},target={},myForkFilePaths=", sourceComponentFilePaths, myForkFileDir.toString(), myForkFilePaths);

            String path = myForkPath.toString().replace("\\", "/");
            targetComponent.setFileList(myForkFilePaths);
            targetComponent.setPath(path);

            componentDAO.save(targetComponent);

            //  2. ?????????????????????component
            forkedComponent.put(sourceComponentId, targetComponent.getId());
            return targetComponent;
        } else {
                /*
                ???. ????????????
                 */

            Graph sourceGraph = sourceComponent.getGraph();
            GraphVO sourceGraphVO = graphVOEntityMapper.graph2GraphVO(sourceGraph);
            //  1. ????????????nodelist
            List<Node> nodeList = sourceGraphVO.getNodeList();
            List<HashMap<String, Object>> newNodeList = new ArrayList<>();
            for (Node sourceNode : nodeList) {
                HashMap<String, Object> newNode = buildNewNode(sourceNode, targetUser, forkedComponent, forkedService, forkedDatasource);
                ;
                newNodeList.add(newNode);
            }
            logger.info("??????nodelist??????");

            componentDAO.save(finalTargetComponent);

            //  2. ????????????graph
            Graph targetGraph = buildNewGraph(targetUser, newNodeList, null, finalTargetComponent, sourceGraph, forkedComponent, forkedService, forkedDatasource);
            logger.info("??????graph??????!graphid={}", targetGraph.getId());

            //  3. ????????????component
            finalTargetComponent = buildNewComponent(targetUser, finalTargetComponent, sourceComponent, false);
            finalTargetComponent.setGraph(targetGraph);
            logger.info("??????component??????");

            //  4. ??????????????????????????????????????????????????????filelist path
            ComponentVO componentVO = componentVOEntityMapper.component2ComponentVO(finalTargetComponent);
            GraphVO graphVO = graphVOEntityMapper.graph2GraphVO(targetGraph);
            if (sourceComponent.isReleased()) {
                graphService.release2ComponentAndGraph(graphVO, componentVO);
            }
            //  1) ?????????release?????????????????????VO???????????????forkFrom?????????????????????????????????
            finalTargetComponent.setForkFrom(sourceComponent);
            componentDAO.save(finalTargetComponent);
            logger.info("??????????????????????????????");

            //  5. ?????????????????????????????????
            copyPicturePNG(sourceComponentId, sourceGraph, username, targetGraph, "COMPONENT");
            logger.info("?????????????????????");
            //  6. ??????component???id?????????????????????????????????fork
            forkedComponent.put(sourceComponentId, finalTargetComponent.getId());
            return finalTargetComponent;
        }
    }

    /**
     * fork?????????
     *
     * @param sourceGraphId ???fork???graphId
     * @param username ????????????
     * @param forkedComponent ?????????fork????????????fork????????????
     * @return fork????????????graph
     * @throws IOException
     */

    public Graph forkExperimentGraph(Long sourceGraphId, String username, HashMap<Long, Long> forkedComponent, HashMap<Long, Long> forkedService, HashMap<Long, Long> forkedDatasource) throws Exception {
        logger.info("Start fork ExperimentGraph,sourceGraphId: {}",sourceGraphId);
        Graph sourceGraph = graphDAO.findById(sourceGraphId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(sourceGraphId)));
        GraphVO sourceGraphVO = graphVOEntityMapper.graph2GraphVO(sourceGraph);
        User targetUser = userDAO.findByUsername(username);

        //  1. ????????????nodelist
        List<Node> nodeList = sourceGraphVO.getNodeList();
        List<HashMap<String, Object>> newNodeList = new ArrayList<>();
        for (Node sourceNode : nodeList) {
            HashMap<String, Object> newNode = buildNewNode(sourceNode, targetUser, forkedComponent, forkedService, forkedDatasource);
            ;
            newNodeList.add(newNode);
        }

        //  2. ????????????experimentGraph
        Graph finalTargetGraph = buildNewGraph(targetUser, newNodeList, null, null, sourceGraph, forkedComponent, forkedService, forkedDatasource);
        return finalTargetGraph;
    }

    /**
     * fork service
     *
     * @param sourceServiceId ???fork???serviceId
     * @param username ????????????
     * @param forkedComponent ?????????fork????????????fork????????????
     * @return fork?????????service
     * @throws Exception
     */

    @Transactional
    public aizoo.domain.Service forkService(Long sourceServiceId, String username, HashMap<Long, Long> forkedComponent, HashMap<Long, Long> forkedService, HashMap<Long, Long> forkedDatasource) throws Exception {
        logger.info("forkService start...");
        aizoo.domain.Service targetService = new aizoo.domain.Service();
        User targetUser = userDAO.findByUsername(username);

        //  1. sourceservice???forkBy??????
        aizoo.domain.Service sourceService = serviceDAO.findById(sourceServiceId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(sourceServiceId)));
        List<aizoo.domain.Service> list = sourceService.getForkBy();
        list.add(targetService);
        sourceService.setForkBy(list);
        serviceDAO.save(sourceService);
        logger.info("setForkBy....");

        Graph sourceGraph = sourceService.getGraph();
        GraphVO sourceGraphVO = graphVOEntityMapper.graph2GraphVO(sourceGraph);

        //  2. ????????????nodelist
        List<Node> nodeList = sourceGraphVO.getNodeList();
        List<HashMap<String, Object>> newNodeList = new ArrayList<>();
        for (Node sourceNode : nodeList) {
            HashMap<String, Object> newNode = buildNewNode(sourceNode, targetUser, forkedComponent, forkedService, forkedDatasource);
            newNodeList.add(newNode);
        }
        logger.info("buildNewNode....");

        //  3. ????????????graph
        Graph finalTargetGraph = buildNewGraph(targetUser, newNodeList, targetService, null, sourceGraph, forkedComponent, forkedService, forkedDatasource);
        graphDAO.save(finalTargetGraph);
        logger.info("buildNewGraph...");


        //  4. ????????????service
        targetService = buildNewService(targetService, sourceService, targetUser);
        targetService.setGraph(finalTargetGraph);
        //  1) ??????service??????filelist
        logger.info("service input len={}", finalTargetGraph.getService().getInputs().size());
        logger.info("service output len={}", finalTargetGraph.getService().getOutputs().size());
        HashMap<String, String> fileList = buildNewServiceFileList(finalTargetGraph, targetService);
        ObjectMapper objectMapper = new ObjectMapper();
        targetService.setFileList(objectMapper.writeValueAsString(fileList));
        serviceDAO.save(targetService);
        logger.info("????????????service??????");


        //  5. ??????service???????????????
        logger.info("targetService namespace={}", targetService.getNamespace().getNamespace());
        logger.info(targetService.getNamespace().getNamespace().replaceAll("\\.", Matcher.quoteReplacement(File.separator)));
        String nsPath = targetService.getNamespace().getNamespace().replaceAll("\\.", Matcher.quoteReplacement(File.separator));
        logger.info("nsPath={}", nsPath);
        String targetPath = Paths.get(file_path, nsPath).toString();
        logger.info("nsPath={}", nsPath);
        translationService.translateService(finalTargetGraph, targetPath, null);

        //  6. ?????????????????????
        copyPicturePNG(sourceServiceId, sourceGraph, username, finalTargetGraph, "SERVICE");

        //  6. ??????service???id?????????????????????????????????fork
        forkedService.put(sourceServiceId, targetService.getId());
        return targetService;
    }

    /**
     * fork ?????????
     *
     * @param applicationId ???fork???applicationId
     * @param username ????????????
     * @param forkedComponent ?????????fork???????????????fork????????????
     * @return fork????????????application
     * @throws Exception
     */
    @Transactional
    public Graph forkApplication(Long applicationId, String username, HashMap<Long, Long> forkedComponent, HashMap<Long, Long> forkedService, HashMap<Long, Long> forkedDatasource) throws Exception {
        logger.info("forkApplication--id={}", applicationId);
        Graph sourceGraph = graphDAO.findById(applicationId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(applicationId)));
        GraphVO sourceGraphVO = graphVOEntityMapper.graph2GraphVO(sourceGraph);
        User targetUser = userDAO.findByUsername(username);

        //  ????????????nodelist
        List<Node> nodeList = sourceGraphVO.getNodeList();
        List<HashMap<String, Object>> newNodeList = new ArrayList<>();
        for (Node sourceNode : nodeList) {
            logger.info("forkApplication--sourceNode={}", sourceNode.getId());
            HashMap<String, Object> newNode = buildNewNode(sourceNode, targetUser, forkedComponent, forkedService, forkedDatasource);
            ;
            newNodeList.add(newNode);
        }

        //  ????????????graph
        Graph finalTargetGraph = buildNewGraph(targetUser, newNodeList, null, null, sourceGraph, forkedComponent, forkedService, forkedDatasource);
        graphDAO.save(finalTargetGraph);

        return finalTargetGraph;
    }

    /**
     * ???????????????????????????
     * @param id
     * @throws Exception
     */
    @Transactional
    public void deleteUploadComponent(Long id) throws Exception {
        logger.info("Start Delete upload Component, ComponentId: {}", id);
        Component component = componentDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
//        ??????????????????????????????
        ComponentUtil.removeComponentFiles(component);
//        ??????????????????????????????????????????graph??????????????????namespace
        component.setNamespace(null);
//        ??????inputs
        List<ComponentInputParameter> componentInputParameters = component.getInputs();
        for (ComponentInputParameter componentInputParameter : componentInputParameters) {
            componentInputParameter.setComponent(null);
            componentInputParameterDAO.delete(componentInputParameter);
        }
//        ??????outputs
        List<ComponentOutputParameter> componentOutputParameters = component.getOutputs();
        for (ComponentOutputParameter componentOutputParameter : componentOutputParameters) {
            componentOutputParameter.setComponent(null);
            componentOutputParameterDAO.delete(componentOutputParameter);
        }
//        ???fork?????????????????????????????????forkFrom????????????null
        List<Component> componentList = component.getForkBy();
        if (componentList != null) {
            for (Component component1 : componentList) {
                component1.setForkFrom(null);
                componentDAO.save(component1);
            }
        }
//        ????????????project??????
        List<Project> projects = component.getProjects();
        List<Long[]> removeList = new ArrayList<>();
        for (Project project : projects) {
            removeList.add(new Long[]{project.getId(), component.getId()});
        }
        projectService.removeProjectComponentRelation(removeList);
        componentDAO.delete(component);
        //??????es????????????
        Optional<ElasticComponent> optional = componentRepository.findById(component.getId().toString());
        if(optional.isPresent())
            componentRepository.delete(optional.get());
    }

    /**
     * ??????????????????
     * @param componentVO ???????????????componentVO
     * @return ???????????????component
     * @throws Exception
     */
    @Transactional(rollbackFor = Exception.class)
    public String buildComponent(ComponentVO componentVO) throws Exception {
        Component component = ComponentVOEntityMapper.MAPPER.componentVO2Component(componentVO, datatypeDAO, componentDAO, namespaceDAO, userDAO);//????????????????????????
        String componentVersion = component.getComponentVersion();
        if (componentVersion != null) {
            componentVersion = component.getComponentVersion().replace(".", "_");
        }
        // ???????????????????????????output?????????????????????self
        ComponentOutputParameter selfOutput = new ComponentOutputParameter();
        selfOutput.setIsSelf(true);
        selfOutput.setParameter(new Parameter(AizooConstans.SELF_NAME, AizooConstans.SELF_TITLE, AizooConstans.SELF_DESCRIPTION, AizooConstans.SELF_ORIGIN_NAME, datatypeDAO.findByName(AizooConstans.AIZOO_UNKNOWN)));
        component.getOutputs().add(selfOutput);

//        set component???path??????
        String filename = component.getName() + componentVersion + ".py";
        String nsPath = componentVO.getNamespace().replaceAll("\\.", Matcher.quoteReplacement(File.separator));
        String targetPath = Paths.get(file_path, nsPath).toString();
        component.setPath(Paths.get(targetPath, filename).toString().replace("\\", "/"));

        Map<String, String> filePathMap = new HashMap<>();
//        set input output???????????????
        for (ComponentInputParameter componentInputParameter : component.getInputs()) {
            componentInputParameter.setComponent(component);
        }
        for (ComponentOutputParameter componentOutputParameter : component.getOutputs()) {
            componentOutputParameter.setComponent(component);
        }
//        set title composed graph version ??????
        component.setTitle(component.getName());
        component.setComposed(false);
        component.setGraph(null);
        component.setComponentVersion(componentVO.getComponentVersion());
        ObjectMapper objectMapper = new ObjectMapper();
//        set filelist??????
        try {
            FileUtil.buildFile(targetPath, filename);
            filePathMap.put(filename, component.getPath());
            String fileList = objectMapper.writeValueAsString(filePathMap);
            component.setFileList(fileList);
            componentDAO.save(component);
        } catch (Exception e) {
            logger.error("Build Component Failed!,componentVO: {}, Exception: {}", componentVO.toString(), e);
            e.printStackTrace();
            //??????????????????????????????????????????????????????????????????
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
     * ????????????????????????
     * @param id ??????id
     * @return ????????????
     */


    public String modifyComponent(long id) throws Exception{
        Component component = componentDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
        String componentVersion = component.getComponentVersion();
        if (componentVersion != null)
            componentVersion = componentVersion.replace(".", "_");
        else
            componentVersion = "";  // ????????????????????????????????????????????????????????????????????????
        String filename = component.getName() + componentVersion + ".py";
        Namespace namespace = component.getNamespace();
        String path = NotebookUtils.getComponentNotebookPath(namespace, filename);
        return path;
    }

    /**
     * ????????????????????????????????????????????????????????????
     * @param componentVO ???????????????componentVO
     * @throws JsonProcessingException
     */
    @Transactional
    public void updateDesc(ComponentVO componentVO) throws JsonProcessingException {
//        ??????description
        componentDAO.updateDesc(componentVO.getId(), componentVO.getDescription());
//        ??????inputs outputs
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
//        ??????properties example
        Map<String, Object> properties = componentVO.getProperties();
        ObjectMapper objectMapper = new ObjectMapper();
        Component component = componentDAO.findById(componentVO.getId()).orElseThrow(() -> new EntityNotFoundException(String.valueOf(componentVO.getId())));
        component.setProperties(objectMapper.writeValueAsString(properties));
        component.setExample(componentVO.getExample());
        componentDAO.save(component);
    }

    /**
     * ???????????????????????????????????????
     * @param graph ?????????
     * @return ?????????????????????Path
     * @throws JsonProcessingException
     */
    public Set getComponentFilePaths(Graph graph) throws JsonProcessingException {
        GraphVO graphVO = GraphVOEntityMapper.MAPPER.graph2GraphVO(graph);
        ObjectMapper objectMapper = new ObjectMapper();
        Set<String> componentFilePaths = new HashSet<>();
        for (Node node : graphVO.getNodeList()) { //???????????????????????????????????????
            if (NodeType.isComponentType(node.getComponentType())
                    && node.getComponentType() != NodeType.DATASOURCE
                    && node.getComponentType() != NodeType.PARAMETER
                    && node.getComponentType() != NodeType.OPERATOR_TEMPLATE
                    && node.getComponentType() != NodeType.OPERATOR

            ) {
                Component nodeComponent = componentDAO.findById(node.getComponentVO().getId()).orElseThrow(() -> new EntityNotFoundException("no component entity found"));
                if (nodeComponent.getFileList() == null) {
                    // ????????????path??????
                    componentFilePaths.add(nodeComponent.getPath());
                } else {
                    // ???????????????fileList??????????????????
                    Map<String, String> fileList = objectMapper.readValue(nodeComponent.getFileList(), new TypeReference<Map<String, String>>() {
                    });
                    componentFilePaths.addAll(fileList.values());
                }
            } else if (node.getComponentType() == NodeType.VISUALCONTAINER) {
                // ??????????????????????????????
                VisualContainer container = visualContainerDAO.findById(node.getVisualContainerVO().getId()).orElseThrow(() -> new EntityNotFoundException("no visualContainer entity found"));
                if (container.getPath() != null)
                    componentFilePaths.add(container.getPath());
                if (container.getTemplatePath() != null)
                    componentFilePaths.add(container.getTemplatePath());
            } else if (node.getComponentType() == NodeType.SERVICE) {
                // service???????????????
                String servicePath = Paths.get(file_path, serviceTemplatePath).toString();
                componentFilePaths.add(servicePath);
            }
        }
        return componentFilePaths;
    }
}
