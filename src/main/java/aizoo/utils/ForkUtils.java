package aizoo.utils;

import aizoo.common.Node;
import aizoo.common.NodeType;
import aizoo.domain.ComponentOutputParameter;
import aizoo.domain.Datasource;
import aizoo.domain.Service;
import aizoo.repository.*;
import aizoo.service.ComponentService;
import aizoo.viewObject.mapper.ComponentVOEntityMapper;
import aizoo.viewObject.mapper.DatasourceVOEntityMapper;
import aizoo.viewObject.mapper.ServiceVOEntityMapper;
import aizoo.viewObject.object.ComponentVO;
import aizoo.viewObject.object.DatasourceVO;
import aizoo.viewObject.object.ServiceVO;
import aizoo.viewObject.object.VisualContainerVO;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import javax.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ForkUtils {

    @Autowired
    DatatypeDAO datatypeDAO;

    @Autowired
    ComponentDAO componentDAO;

    @Autowired
    NamespaceDAO namespaceDAO;

    @Autowired
    DatasourceDAO datasourceDAO;

    @Autowired
    ComponentOutputParameterDAO componentOutputParameterDAO;

    @Autowired
    UserDAO userDAO;

    @Autowired
    ServiceDAO serviceDAO;

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(ForkUtils.class);

    /**
     * 在一个数据库表中，将一个暴露结点拷贝输出。
     * 从sourceNode里拿到暴露的输出列表，查sourceNode的output和component的output
     * 两个output的列表对比一下，如果两个名字一致
     * 则给这个component的暴露输出列表（exposed output）里加上拷贝之后的新输出的id
     * @param component
     * @param sourceNode
     * @return
     */
    public List<Long> updateExposedOutput(aizoo.domain.Component component, Node sourceNode) {
        // 存放所有源输出结点
        List<String> sourceList = new ArrayList<>();
        // 1、遍历所有源结点中暴露的结点
        for (Long sourceExposedId : sourceNode.getExposedOutput()) {
            // 2、通过id找到该暴露结点的output
            ComponentOutputParameter sourceExposedOutput = componentOutputParameterDAO.findById(sourceExposedId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(sourceExposedId)));
            // 3、找到后获取它的name放到源输出列表中
            String name = sourceExposedOutput.getParameter().getName();
            sourceList.add(name);
        }
        // 存放组件的output
        List<ComponentOutputParameter> componentOutputs = component.getOutputs();
        // 存放所有目的
        List<Long> targetExposedOutput = new ArrayList<>();
        for (ComponentOutputParameter componentOutput : componentOutputs) {
            String targetName = componentOutput.getParameter().getName();
            // 检查两个output是否一致
            for (String sourceName : sourceList) {
                if (targetName.equals(sourceName)) {
                    // 若一致 则加上拷贝后新输出的id
                    targetExposedOutput.add(componentOutput.getId());
                }
            }
        }
        return targetExposedOutput;
    }

    /**
     * 同上方法
     * @param childComponent
     * @param sourceExposedOutputIds
     * @return
     */
    private List<Long> updateExposedOutput(aizoo.domain.Component childComponent, List<Integer> sourceExposedOutputIds) {
        try {
            List<ComponentOutputParameter> sourceExposedOutputList = new ArrayList<>();
            for (Integer sourceExposedId : sourceExposedOutputIds) {
                ComponentOutputParameter sourceExposedOutput = componentOutputParameterDAO.findById(sourceExposedId.longValue()).orElseThrow(() -> new EntityNotFoundException(String.valueOf(sourceExposedId)));
                sourceExposedOutputList.add(sourceExposedOutput);
            }
            List<String> sourceNameList = new ArrayList<>();
            for (ComponentOutputParameter sourceOutput : sourceExposedOutputList) {
                String name = sourceOutput.getParameter().getName();
                sourceNameList.add(name);
            }
            List<ComponentOutputParameter> childComponentParameterList = childComponent.getOutputs();
            List<Long> targetExposedOutput = new ArrayList<>();
            for (ComponentOutputParameter childComponentParameter : childComponentParameterList) {
                String targetName = childComponentParameter.getParameter().getName();
                for (String sourceName : sourceNameList) {
                    if (targetName.equals(sourceName)) {
                        targetExposedOutput.add(childComponentParameter.getId());
                    }
                }
            }
            return targetExposedOutput;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private void copyImg() {//拷缩略图

    }

    private static void buildNode() {
        //if nodetype===servcie 要使用service的vo……
    }

    /**
     * 新建一份原始的json解析复制出去
     * 若原始为空 则直接返回
     * 从原始json中获取elements和nodes 判断node类型后解析
     * @param sourceOriginJson
     * @param forkedComponent
     * @param forkedService
     * @return
     * @throws JsonProcessingException
     */
    public String buildOriginJson(@Nullable String sourceOriginJson, HashMap<Long, Long> forkedComponent, HashMap<Long, Long> forkedService, HashMap<Long, Long> forkedDatasource) throws JsonProcessingException {

        if (sourceOriginJson == null)
            return null;
        Map<String, Object> sourceOJ = objectMapper.readValue(sourceOriginJson, new TypeReference<Map<String, Object>>() {});
        Map<String, Object> elements = (Map<String, Object>) sourceOJ.get("elements");
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) elements.get("nodes");
        for (Map<String, Object> node : nodes) {
            // 获取node的类型
            String classes = (String) node.get("classes");

            // 如果类型为compoundNode,则表示node是图中的节点
            if (classes.contains("compoundNode")) {
                Map<String, Object> sourceData = (Map<String, Object>) node.get("data");
                sourceData.put("checkPointId", null);
                sourceData.put("serviceJobId", null);
                // 更新原始json
                updateOriginJsonData(sourceData, forkedComponent, forkedService, forkedDatasource);
            }
        }
        return objectMapper.writeValueAsString(sourceOJ);
    }

    /**
     * 更新原始json数据
     * 查看原始数据中是否有组件：若有，解析（不是复制）一份并更新
     * 若没有，查看是否有服务：若有，解析（不是复制）一份并更新
     * @param sourceData
     * @param forkedComponent
     * @param forkedService
     * @throws JsonProcessingException
     */
    public void updateOriginJsonData(Map<String, Object> sourceData, HashMap<Long, Long> forkedComponent, HashMap<Long, Long> forkedService, HashMap<Long, Long> forkedDatasource) throws JsonProcessingException {
        try {
            if (sourceData.get("component") != null) {
                Map<String, Object> sourceComponentMap = (Map<String, Object>) sourceData.get("component");
                logger.info("sourceData.component={}", sourceComponentMap);

                Long sourceId = Long.parseLong(sourceComponentMap.get("id").toString());
                logger.info("sourceData.component.sourceId={}", sourceId);

                // 通过数据库接口查询相关的服务信息
                aizoo.domain.Component targetComponent = componentDAO.findById(forkedComponent.get(sourceId)).orElseThrow(() -> new EntityNotFoundException());
                ComponentVO targetComponentVO = ComponentVOEntityMapper.MAPPER.component2ComponentVO(targetComponent);

                // 解析一份组件信息
                sourceComponentMap.put("id", targetComponentVO.getId());
                sourceComponentMap.put("graphId", targetComponentVO.getGraphId());
                sourceComponentMap.put("forkFromUser", targetComponentVO.getForkFromUser());
                sourceComponentMap.put("namespace", targetComponentVO.getNamespace());
                sourceComponentMap.put("updateTime", targetComponentVO.getUpdateTime());
                sourceComponentMap.put("username", targetComponentVO.getUsername());
                sourceComponentMap.put("privacy", targetComponentVO.getPrivacy());

                // parameter不需要对输入输出进行替换
                if (sourceData.get("componentType").equals(NodeType.PARAMETER.toString())) {
                    return;
                }

                // 更新输入输出的端点信息
                updateEndPoint((List<Map<String, Object>>) sourceComponentMap.get("inputs"), targetComponentVO.getInputs());
                updateEndPoint((List<Map<String, Object>>) sourceComponentMap.get("outputs"), targetComponentVO.getOutputs());

                List<Long> targetExposedOutput = updateExposedOutput(targetComponent, (List<Integer>) sourceData.get("exposedOutput"));
                sourceData.put("exposedOutput", targetExposedOutput);
                sourceData.put("component", sourceComponentMap);
                return;

            } else if (sourceData.get("service") != null) {
                // 通过数据库接口查询相关的服务信息
                Map<String, Object> sourceServiceMap = (Map<String, Object>) sourceData.get("service");
                Long sourceId = Long.parseLong(sourceServiceMap.get("id").toString());
                Service targetService = serviceDAO.findById(forkedService.get(sourceId)).orElseThrow(() -> new EntityNotFoundException());
                ServiceVO targetServiceVO = ServiceVOEntityMapper.MAPPER.Service2ServiceVO(targetService);

                // 解析一份服务信息
                sourceServiceMap.put("id", targetServiceVO.getId());
                sourceServiceMap.put("namespace", targetServiceVO.getNamespace());
                sourceServiceMap.put("updateTime", targetServiceVO.getUpdateTime());
                sourceServiceMap.put("privacy", targetServiceVO.getPrivacy());
                sourceServiceMap.put("graphId", targetServiceVO.getGraphId());

                // 更新输入输出的端点信息
                updateEndPoint((List<Map<String, Object>>) sourceServiceMap.get("inputs"), targetServiceVO.getInputs());
                updateEndPoint((List<Map<String, Object>>) sourceServiceMap.get("outputs"), targetServiceVO.getOutputs());
            } else if (sourceData.get("datasource") != null) {
                // 通过数据库接口查询相关的数据集信息
                Map<String, Object> sourceDatasourceMap = (Map<String, Object>) sourceData.get("datasource");
                Long sourceId = Long.parseLong(sourceDatasourceMap.get("id").toString());
                Datasource targetDatasource = datasourceDAO.findById(forkedDatasource.get(sourceId)).orElseThrow(() -> new EntityNotFoundException());
                DatasourceVO targetDatasourceVO = DatasourceVOEntityMapper.MAPPER.datasource2DatasourceVO(targetDatasource);

                // 解析一份数据集信息
                sourceDatasourceMap.put("id", targetDatasourceVO.getId());
                sourceDatasourceMap.put("namespace", targetDatasourceVO.getNamespace());
                sourceDatasourceMap.put("updateTime", targetDatasourceVO.getUpdateTime());
                sourceDatasourceMap.put("privacy", targetDatasourceVO.getPrivacy());

                // 更新输出的端点信息
                updateEndPoint((List<Map<String, Object>>) sourceDatasourceMap.get("outputs"), targetDatasourceVO.getOutputs());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 端点信息更新到新的地方去
     * @param sourcePoints 源端点
     * @param targetPoints 目的端点
     */
    public void updateEndPoint(List<Map<String, Object>> sourcePoints, List<Map<String, Object>> targetPoints) {
        for (Map<String, Object> sourcePoint : sourcePoints) {
            String sourceName = (String) sourcePoint.get("originName");

            // 通过输入的originName字段，判断两个输入是否是相互对应的
            for (Map<String, Object> targetPoint : targetPoints) {
                String targetName = (String) targetPoint.get("originName");
                // 如果对应 则更新端点信息
                if (targetName.equals(sourceName)) {
                    sourcePoint.put("id", targetPoint.get("id"));
                    break;
                }
            }
        }
    }
}
