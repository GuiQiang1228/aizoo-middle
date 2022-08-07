package aizoo.grpcObject.mapper;


import aizoo.domain.CheckPoint;
import aizoo.domain.Component;
import aizoo.domain.Graph;
import aizoo.domain.ServiceJob;
import aizoo.grpcObject.object.GraphObject;
import aizoo.repository.CheckPointDAO;
import aizoo.repository.ServiceJobDAO;
import aizoo.utils.DAOUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import javax.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface GraphObjectEntityMapper {
    GraphObjectEntityMapper MAPPER = Mappers.getMapper(GraphObjectEntityMapper.class);
    ObjectMapper objectMapper = new ObjectMapper();

    @Mappings({
            @Mapping(
                    target = "nodeList",
                    ignore = true),
            @Mapping(
                    target = "linkList",
                    ignore = true),
            @Mapping(
                    target = "component",
                    ignore = true)
    })
    GraphObject graph2GraphObject(Graph graph, @Context DAOUtil daoUtil);

    /**
     * 将graph转换成graphObject
     * @param graph       source graph（图）
     * @param graphObject target graph（图实体）
     * @param daoUtil dao层的方法
     */
    @AfterMapping
    default void completeGraphObject(Graph graph, @MappingTarget GraphObject graphObject, @Context DAOUtil daoUtil) {
        try {
            if (graph.getLinkList() != null) {
                // 图所有连线信息的List，详细结构可查看package aizoo.common.Link
                // 将连线信息读取为List格式并保存进graphObject
                List<Map<String, Object>> linkList = objectMapper.readValue(graph.getLinkList(), new TypeReference<List<Map<String, Object>>>() {
                });
                graphObject.setLinkList(linkList);
            }
            if (graph.getNodeList() != null) {
                // 图所有节点的List,详细结构可查看package aizoo.common.Node
                // 将节点信息读取为List格式并保存进graphObject
                List<Map<String, Object>> nodeList = objectMapper.readValue(graph.getNodeList(), new TypeReference<List<Map<String, Object>>>() {
                });
                for (Map<String, Object> node : nodeList) {
                    if (node.get("component") != null) {
                        // 图所有组件的map，以及配置、文件列表信息
                        Map<String, Object> componentObject = (Map) node.get("component");
                        String properties = (String) componentObject.get("properties");
                        String fileList = (String) componentObject.get("fileList");
                        if (properties != null) {
                            Map<String, Object> propertiesObject = objectMapper.readValue(properties, new TypeReference<Map<String, Object>>() {
                            });
                            componentObject.replace("properties", propertiesObject);
                        }
                        if (fileList != null) {
                            Map<String, String> fileListObject = objectMapper.readValue(fileList, Map.class);
                            componentObject.replace("fileList", fileListObject);
                        }
                        if (node.get("componentType").equals("MODEL")) {
                            // 如果节点的组件类型是“MODEL”，通过checkPointId获得checkPoint的路径信息
                            if (node.get("checkPointId") != null) {
                                CheckPoint checkPoint = daoUtil.findCheckPointById(Long.valueOf(String.valueOf(node.get("checkPointId"))));
                                componentObject.put("checkPointPath", checkPoint.getPath());
                            }
                        }
                        componentObject.remove("user");
                        node.replace("component", componentObject);
                    } else if (node.get("service") != null) {
                        // 图所有服务的map，以及文件列表信息
                        Map<String, Object> serviceObject = (Map) node.get("service");
                        String fileList = (String) serviceObject.get("fileList");
                        if (fileList != null) {
                            Map<String, String> fileListObject = objectMapper.readValue(fileList, Map.class);
                            serviceObject.replace("fileList", fileListObject);
                        }
                        if (node.get("serviceJobId") != null) {
                            // 获取服务实体的ip,端口，url信息
                            ServiceJob serviceJob = daoUtil.findServiceJobById(Long.valueOf(String.valueOf(node.get("serviceJobId"))));
                            serviceObject.put("ip", serviceJob.getIp());
                            serviceObject.put("port", serviceJob.getPort());
                            serviceObject.put("url", serviceJob.getUrl());
                        }
                        serviceObject.remove("user");
                        node.replace("service", serviceObject);
                    } else if (node.get("visualContainer") != null) {
                        // 可视化容器信息
                        Map<String, Object> containerObject = (Map) node.get("visualContainer");
                        String properties = (String) containerObject.get("properties");
                        if (properties != null) {
                            Map<String, Object> propertiesObject = objectMapper.readValue(properties, new TypeReference<Map<String, Object>>() {
                            });
                            containerObject.replace("properties", propertiesObject);
                        }
                    }
                }
                graphObject.setNodeList(nodeList);
            }
            if (graph.getComponent() != null) {
                // 将图所有组件读取为Map格式并保存进graphObject
                Component component = graph.getComponent();
                Map<String, Object> componentObject = objectMapper.readValue(objectMapper.writeValueAsString(component), Map.class);
                // 将properties和fileList从String转化成对应的java类型
                if (component.getProperties() != null) {
                    Map<String, Object> propertiesObject = objectMapper.readValue(component.getProperties(), new TypeReference<Map<String, Object>>() {
                    });
                    componentObject.replace("properties", propertiesObject);
                }
                if (component.getFileList() != null) {
                    Map<String, String> fileListObject = objectMapper.readValue(component.getFileList(), Map.class);
                    componentObject.replace("fileList", fileListObject);
                }
                graphObject.setComponent(componentObject);
            }
            if (graph.getService() != null) {
                graphObject.setToken(graph.getService().getToken());
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
