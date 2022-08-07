package aizoo.viewObject.mapper;

import aizoo.common.*;
import aizoo.domain.*;
import aizoo.repository.*;
import aizoo.utils.DAOUtil;
import aizoo.viewObject.object.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import javax.persistence.EntityNotFoundException;
import java.util.*;


@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface GraphVOEntityMapper {
    GraphVOEntityMapper MAPPER = Mappers.getMapper(GraphVOEntityMapper.class);
    ObjectMapper objectMapper = new ObjectMapper();

    //    nodeList需要还原vo-->entity
    @Mappings({
            @Mapping(target = "nodeList", ignore = true),
            @Mapping(target = "linkList", ignore = true),
            @Mapping(
                    source = "originJson",
                    target = "originJson",
                    qualifiedByName = "graphVOOriginJson2GraphOriginJson"
            ),
            @Mapping(source = "graphReleased", target = "released")
    })
    Graph graphVO2Graph(GraphVO graphVO, @Context GraphDAO graphDAO, @Context ComponentDAO componentDAO, @Context DAOUtil daoUtil, @Context DatatypeDAO datatypeDAO);

    @Mappings({
            @Mapping(target = "nodeList", ignore = true),
            @Mapping(target = "linkList", ignore = true),
            @Mapping(
                    source = "originJson",
                    target = "originJson",
                    qualifiedByName = "graphOriginJson2GraphVOOriginJson"
            ),
            @Mapping(
                    source = "released",
                    target = "graphReleased"
            )
    })
    GraphVO graph2GraphVO(Graph graph);

    @Named("graphVOOriginJson2GraphOriginJson")
    default String graphVOOriginJson2GraphOriginJson(Map<String, Object> var) throws JsonProcessingException {
        return objectMapper.writeValueAsString(var);
    }

    @Named("graphOriginJson2GraphVOOriginJson")
    default Map<String, Object> graphOriginJson2GraphVOOriginJson(String var) throws JsonProcessingException {
        if (var != null) {
            return objectMapper.readValue(var, new TypeReference<Map<String, Object>>() {
            });
        }
        return new HashMap<>();
    }

    /**
     * graphVO->graph
     * 主要负责组织nodelist和linklist
     *
     * @param graphVO
     * @param graph
     * @param graphDAO
     * @param componentDAO
     * @param daoUtil
     * @param datatypeDAO
     * @throws EntityNotFoundException
     */
    @AfterMapping
    default void completeGraph(GraphVO graphVO, @MappingTarget Graph graph, @Context GraphDAO graphDAO, @Context ComponentDAO componentDAO, @Context DAOUtil daoUtil, @Context DatatypeDAO datatypeDAO) throws EntityNotFoundException {
        Graph graph1 = graphDAO.findById(graphVO.getId()).orElseThrow(() -> new EntityNotFoundException(String.valueOf(graphVO.getId())));
        // 从数据库里查到图，复制给graph（MappingTarget）
        if (graph1 != null) {
            graph.setCreateTime(graph1.getCreateTime());
            graph.setComponent(graph1.getComponent());
            graph.setService(graph1.getService());
            graph.setUser(graph1.getUser());
            graph.setReleased(graph1.isReleased());
        }
        // nodeList中NodeVO中的componentVO、datasourceVO、serviceVO也需要转成对应entity
        try {
            // 1. linklist 直接全复制即可
            graph.setLinkList(objectMapper.writeValueAsString(graphVO.getLinkList()));
            // 2. 根据vo里的nodelist，组织component的nodelist
            List<Node> nodeList = graphVO.getNodeList();
            // 放到graph里的nodelist（需根据vo和数据库，补全全部信息）
            List<HashMap<String, Object>> newNodeList = new ArrayList<>();
            for (Node node : nodeList) {
                HashMap<String, Object> newNode = new HashMap<>();
                // 2.1 复制component，从数据库里查component，复制到nodeComponent里，部分字段如输入输出的title等，根据vo数据进行修改
                // 注意，所有对象，都要是new的与hibernate无关的，否则hibernate可能监听到有些对象被修改了，但未被保存
                if (node.getComponentType() != NodeType.DATASOURCE && NodeType.isComponentType(node.getComponentType())) {
                    // 数据库里先把component查出来，把input和output都查出来，避免现场查，有些对象会进入hibernate的session托管
                    Component component = componentDAO.findById(node.getComponentVO().getId()).orElseThrow(() -> new EntityNotFoundException(String.valueOf(node.getComponentVO().getId())));
                    component.getInputs();
                    component.getOutputs();

                    // 准备放在nodelist某元素里的component
                    Component nodeComponent = new Component();

                    // 从数据库里查出来的component先转JSON，再赋值给nodeComponent，保证它是新对象，否则相同id的component存到nodeList里信息相同，datasource同理
                    try {
                        nodeComponent = objectMapper.readValue(objectMapper.writeValueAsString(component), Component.class);
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                    // vo中的nodeList中component的输入输出前端改过名字，需要根据vo重新给nodeComponent赋值
                    ComponentVO componentVO = node.getComponentVO();
                    // 原始数据库里查到的output parameter，先转json，再转回，保证是新对象而不是数据库查出来的
                    List<ComponentOutputParameter> componentOutputParameterList = objectMapper.readValue(objectMapper.writeValueAsString(nodeComponent.getOutputs()), new TypeReference<List<ComponentOutputParameter>>() {
                    });
                    // 根据vo值给output parameter赋值
                    for (Map<String, Object> nodeComponentOutput : componentVO.getOutputs()) {
                        for (ComponentOutputParameter componentOutputParameter : componentOutputParameterList) {
                            if (nodeComponentOutput.get("id").toString().equals(componentOutputParameter.getId().toString())) {
                                componentOutputParameter.getParameter().setName(nodeComponentOutput.get("name").toString());
                                componentOutputParameter.getParameter().setTitle(nodeComponentOutput.get("title").toString());
                                componentOutputParameter.getParameter().setOriginName(nodeComponentOutput.get("originName").toString());
                                componentOutputParameter.getParameter().setDescription(
                                        nodeComponentOutput.get("description") == null ? "" : nodeComponentOutput.get("description").toString());

                                if (nodeComponentOutput.containsKey("datatype")) {
                                    // service的parameter，datatype默认为null，会在拖入时，由用户赋值
                                    // 其他情况，datatype均已经赋值好，vo里的值与原始值一致
                                    String dataTypeStr = nodeComponentOutput.get("datatype").toString();
                                    if (dataTypeStr != null) {
                                        Datatype datatype = datatypeDAO.findByName(dataTypeStr);
                                        if (datatype != null) {
                                            // 为了避免修改数据库，新建datatype对象，往graph的node list里保存，而不用原来从数据库里查出的
                                            Datatype datatypeInNode = new Datatype();
                                            datatypeInNode.setId(datatype.getId());
                                            datatypeInNode.setName(datatype.getName());
                                            datatypeInNode.setTitle(datatype.getTitle());
                                            datatypeInNode.setPrivacy(datatype.getPrivacy());
                                            componentOutputParameter.getParameter().setDatatype(datatypeInNode);
                                        }
                                    }
                                }

                            }
                        }
                    }
                    // 原始数据库里查到的input parameter，先转json，再转回，保证是新对象而不是数据库查出来的
                    List<ComponentInputParameter> componentInputParameterList = objectMapper.readValue(objectMapper.writeValueAsString(nodeComponent.getInputs()), new TypeReference<List<ComponentInputParameter>>() {
                    });
                    // 根据vo值给input parameter赋值
                    for (Map<String, Object> nodeComponentInput : componentVO.getInputs()) {
                        for (ComponentInputParameter componentInputParameter : componentInputParameterList) {
                            if (nodeComponentInput.get("id").toString().equals(componentInputParameter.getId().toString())) {
                                componentInputParameter.getParameter().setName(nodeComponentInput.get("name").toString());
                                componentInputParameter.getParameter().setTitle(nodeComponentInput.get("title").toString());
                                componentInputParameter.getParameter().setOriginName(nodeComponentInput.get("originName").toString());
                                componentInputParameter.getParameter().setDescription(
                                        nodeComponentInput.get("description") == null ? "" : nodeComponentInput.get("description").toString());

                                if (nodeComponentInput.containsKey("datatype")) {
                                    // service的parameter，datatype默认为null，会在拖入时，由用户赋值
                                    // 其他情况，datatype均已经赋值好，vo值与数据库值一致
                                    String dataTypeStr = nodeComponentInput.get("datatype").toString();
                                    if (dataTypeStr != null) {
                                        Datatype datatype = datatypeDAO.findByName(dataTypeStr);
                                        if (datatype != null) {
                                            // 为了避免修改数据库，新建datatype对象，往graph的node list里保存，而不用原来从数据库里查出的
                                            Datatype datatypeInNode = new Datatype();
                                            datatypeInNode.setId(datatype.getId());
                                            datatypeInNode.setName(datatype.getName());
                                            datatypeInNode.setTitle(datatype.getTitle());
                                            datatypeInNode.setPrivacy(datatype.getPrivacy());
                                            componentInputParameter.getParameter().setDatatype(datatypeInNode);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    assert nodeComponent != null;
                    nodeComponent.setDescription(componentVO.getDescription());
                    nodeComponent.setTitle(componentVO.getTitle());
                    nodeComponent.setOutputs(componentOutputParameterList);
                    nodeComponent.setInputs(componentInputParameterList);
                    nodeComponent.setProperties(objectMapper.writeValueAsString(componentVO.getProperties()));
                    newNode.put("component", nodeComponent);
                } else if (node.getComponentType() == NodeType.DATASOURCE) {
                    // 注意，由于jpa要求比较严格，直接修改从数据库查询的entity，但未进行save，会报错！！
                    // 所以，如果有要set值的，放在类似nodelist里而不是parameter本身的，一定要new 出来新的对象，再进行set
                    // 即，先查出来，转成json str，再用mapper转回去，再执行set操作
                    Datasource datasource = daoUtil.findDatasourceById(node.getDatasourceVO().getId());
                    datasource.getDatasourceOutputParameters();
                    Datasource nodeDatasource = new Datasource();
                    try {
                        nodeDatasource = objectMapper.readValue(objectMapper.writeValueAsString(datasource), Datasource.class);
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                    DatasourceVO datasourceVO = node.getDatasourceVO();
                    List<DatasourceOutputParameter> datasourceOutputParameterList = objectMapper.readValue(objectMapper.writeValueAsString(nodeDatasource.getDatasourceOutputParameters()), new TypeReference<List<DatasourceOutputParameter>>() {
                    });
                    for (Map<String, Object> nodeDatasourceOutput : datasourceVO.getOutputs()) {
                        for (DatasourceOutputParameter datasourceOutputParameter : datasourceOutputParameterList) {
                            if (nodeDatasourceOutput.get("id").toString().equals(datasourceOutputParameter.getId().toString())) {
                                Parameter p = datasourceOutputParameter.getParameter();
                                p.setName(nodeDatasourceOutput.get("name").toString());
                                p.setDescription(nodeDatasourceOutput.get("description").toString());
                                p.setOriginName(nodeDatasourceOutput.get("originName").toString());
                                p.setTitle(nodeDatasourceOutput.get("title").toString());
                                p.setDatatype(nodeDatasourceOutput.get("datatype") != null ? datatypeDAO.findByName(nodeDatasourceOutput.get("datatype").toString()) : null);
                                datasourceOutputParameter.setParameterIoType(ParameterIOType.OUTPUT);
                            }
                        }
                    }
                    nodeDatasource.setDescription(datasourceVO.getDescription());
                    nodeDatasource.setTitle(datasourceVO.getTitle());
                    nodeDatasource.setDatasourceOutputParameters(datasourceOutputParameterList);
                    newNode.put("datasource", nodeDatasource);
                } else if (node.getComponentType() == NodeType.SERVICE) {
                    // 2.2 复制service节点
                    Service service = daoUtil.findServiceById(node.getServiceVO().getId());
                    service.getInputs();
                    service.getOutputs();
                    // 提前把所有要用到的对象，都查出来，再进行对象转jsonstr再转对象的工作
                    Service nodeService = new Service();
                    try {

                        String tmp = objectMapper.writeValueAsString(service);
                        // 这里强制new了个新的string，避免编译优化时这两个json entity互转操作被优化掉，从而直接使用hibernate session里的持久态的对象，而引起报错
                        nodeService = objectMapper.readValue(new String(tmp + ""), Service.class);
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }


                    ServiceVO serviceVO = node.getServiceVO();
                    List<ServiceInputParameter> serviceInputParameterList = objectMapper.readValue(objectMapper.writeValueAsString(nodeService.getInputs()), new TypeReference<List<ServiceInputParameter>>() {
                    });


                    //在应用图中，service的输入输出等信息均可被修改，需要重新给nodelist赋值
                    for (Map<String, Object> nodeServiceInput : serviceVO.getInputs()) {
                        for (ServiceInputParameter serviceInputParameter : serviceInputParameterList) {
                            if (nodeServiceInput.get("id").toString().equals(serviceInputParameter.getId().toString())) {
                                Parameter p = serviceInputParameter.getParameter();
                                p.setName(nodeServiceInput.get("name").toString());
                                p.setDescription(nodeServiceInput.get("description").toString());
                                p.setOriginName(nodeServiceInput.get("originName").toString());
                                p.setTitle(nodeServiceInput.get("title").toString());
                                p.setDatatype(nodeServiceInput.get("datatype") != null ? datatypeDAO.findByName(nodeServiceInput.get("datatype").toString()) : null);
                                serviceInputParameter.setParameterIoType(ParameterIOType.INPUT);
                            }
                        }
                    }

                    nodeService.setInputs(serviceInputParameterList);


                    List<ServiceOutputParameter> serviceOutputParameters = objectMapper.readValue(objectMapper.writeValueAsString(nodeService.getOutputs()), new TypeReference<List<ServiceOutputParameter>>() {
                    });

                    //在应用图中，service的输入输出等信息均可被修改，需要重新给nodelist赋值
                    for (Map<String, Object> nodeServiceOutput : serviceVO.getOutputs()) {
                        for (ServiceOutputParameter serviceOutputParameter : serviceOutputParameters) {
                            if (nodeServiceOutput.get("id").toString().equals(serviceOutputParameter.getId().toString())) {
                                Parameter p = serviceOutputParameter.getParameter();
                                p.setName(nodeServiceOutput.get("name").toString());
                                p.setDescription(nodeServiceOutput.get("description").toString());
                                p.setOriginName(nodeServiceOutput.get("originName").toString());
                                p.setTitle(nodeServiceOutput.get("title").toString());
                                p.setDatatype(nodeServiceOutput.get("datatype") != null ? datatypeDAO.findByName(nodeServiceOutput.get("datatype").toString()) : null);
                                serviceOutputParameter.setParameterIoType(ParameterIOType.OUTPUT);
                            }
                        }
                    }
                    nodeService.setOutputs(serviceOutputParameters);
                    nodeService.setDescription(serviceVO.getDescription());
                    nodeService.setTitle(serviceVO.getTitle());
                    newNode.put("service", nodeService);
                } else if (node.getComponentType() == NodeType.VISUALCONTAINER) {
                    // 2.3 复制可视化容器节点（已弃用）
                    VisualContainerVO containerVO = node.getVisualContainerVO();
                    VisualContainer nodeContainer = daoUtil.findVisualContainerById(containerVO.getId());
                    assert nodeContainer != null;
                    nodeContainer.setDescription(containerVO.getDescription());
                    newNode.put("visualContainer", nodeContainer);
                }
                completeNewNode(newNode, node);
                newNodeList.add(newNode);
            }
            graph.setNodeList(objectMapper.writeValueAsString(newNodeList));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    /**
     * 将newNode中没有的字段补上并赋null值
     *
     * @param newNode 最终存到graph的nodelist中
     * @param node    vo node
     */
    default void completeNewNode(Map<String, Object> newNode, Node node) {
        for (String nodeType : Arrays.asList("component", "datasource", "service", "visualContainer"))
            if (!newNode.keySet().contains(nodeType))
                newNode.put(nodeType, null);

        newNode.put("id", node.getId());
        newNode.put("componentType", node.getComponentType());
        newNode.put("variable", node.getVariable());
        newNode.put("parameterName", node.getParameterName());
        newNode.put("saveOutput", node.getSaveOutput());
        newNode.put("exposedOutput", node.getExposedOutput());
        newNode.put("dataloaderType", node.getDataloaderType());
        newNode.put("checkPointId", node.getCheckPointId());
        newNode.put("serviceJobId", node.getServiceJobId());
    }

    default void completeServiceNode(Map<String, Object> newNode, Node node) {

    }

    /**
     * graph->graphVO
     * 主要负责补全graphvo的基本信息，graph nodelist中各种node，都给替换成vo，nodelist存时，存的是数据库里的对象，替换了部分值（不是vo）
     * @param graph
     * @param graphVO
     */
    @AfterMapping
    default void completeGraphVO(Graph graph, @MappingTarget GraphVO graphVO) {
        if (graph.getComponent() != null) {
            graphVO.setComponentType(graph.getComponent().getComponentType());
            graphVO.setNamespace(graph.getComponent().getNamespace().getNamespace());
            graphVO.setGraphPrivacy(graph.getComponent().getPrivacy());
        }
        if (graph.getService() != null) {
            graphVO.setNamespace(graph.getService().getNamespace().getNamespace());
            graphVO.setGraphPrivacy(graph.getService().getPrivacy());
        }
        graphVO.setDescription(graph.getDescription());
        JavaType nodeVOType = objectMapper.getTypeFactory().constructParametricType(List.class, Node.class);
        JavaType linkVOType = objectMapper.getTypeFactory().constructParametricType(List.class, Link.class);
        try {
            if (graph.getNodeList() != null) {
//                把数据库中nodeList中Node中的component-->componentVO,datasource-->datasourceVO,service-->serviceVO
                List<Map<String, Object>> entityNodeList = objectMapper.readValue(graph.getNodeList(), new TypeReference<List<Map<String, Object>>>() {
                });
                for (Map<String, Object> entityNode : entityNodeList) {
                    // component类型
                    Component nodeComponent = objectMapper.readValue(objectMapper.writeValueAsString(entityNode.get("component")), Component.class);
                    if (graph.getGraphType() == GraphType.SERVICE && nodeComponent.getComponentType() == ComponentType.PARAMETER && graph.getService() != null) {
                        nodeComponent.setInputs(nodeComponent.getInputs());
                        nodeComponent.setOutputs(nodeComponent.getOutputs());
                    }
                    ComponentVO componentVO = ComponentVOEntityMapper.MAPPER.component2ComponentVO(nodeComponent);
                    Datasource datasource = objectMapper.readValue(objectMapper.writeValueAsString(entityNode.get("datasource")), Datasource.class);
                    DatasourceVO datasourceVO = DatasourceVOEntityMapper.MAPPER.datasource2DatasourceVO(datasource);
                    Service service = objectMapper.readValue(objectMapper.writeValueAsString(entityNode.get("service")), Service.class);
                    ServiceVO serviceVO = ServiceVOEntityMapper.MAPPER.Service2ServiceVO(service);
                    VisualContainer container = objectMapper.readValue(objectMapper.writeValueAsString(entityNode.get("visualContainer")), VisualContainer.class);
                    VisualContainerVO containerVO = ContainerVOEntityMapper.MAPPER.container2ContainerVO(container);
                    entityNode.put("component", componentVO);
                    entityNode.put("datasource", datasourceVO);
                    entityNode.put("service", serviceVO);
                    entityNode.put("visualContainer", containerVO);
                }
                List<Node> nodeList = objectMapper.readValue(objectMapper.writeValueAsString(entityNodeList), nodeVOType);
                graphVO.setNodeList(nodeList);
            }
            if (graph.getLinkList() != null) {
                List<Link> linkList = objectMapper.readValue(graph.getLinkList(), linkVOType);
                graphVO.setLinkList(linkList);
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
