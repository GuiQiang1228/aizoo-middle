package aizoo.viewObject.mapper;

import aizoo.common.NodeType;
import aizoo.domain.*;
import aizoo.repository.DatatypeDAO;
import aizoo.repository.VisualContainerDAO;
import aizoo.viewObject.object.VisualContainerVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import javax.persistence.EntityNotFoundException;
import java.util.*;

@Mapper(componentModel = "spring")
public interface ContainerVOEntityMapper {
    ContainerVOEntityMapper MAPPER = Mappers.getMapper(ContainerVOEntityMapper.class);

    @Mappings({
            @Mapping(source = "inputs",
                    target = "inputs",
                    qualifiedByName = "containerInput2ContainerVOMap"),
            @Mapping(target = "componentType",
                     ignore = true),
            @Mapping(
                    source = "properties",
                    target = "properties",
                    qualifiedByName = "containerProperties2ContainerVOProperties"
            ),
    })
    VisualContainerVO container2ContainerVO(VisualContainer container);

    @Mappings({
            @Mapping(source = "inputs",
                    target = "inputs",
                    ignore = true),
            @Mapping(
                    source = "properties",
                    target = "properties",
                    qualifiedByName = "containerVOProperties2ContainerProperties"
            )
    })
    VisualContainer containerVO2Container(VisualContainerVO containerVO);

    @Named("containerInput2ContainerVOMap")
    default List<Map<String, Object>> containerInput2ContainerVOMap(List<VisualContainerInputParameter> var) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (VisualContainerInputParameter vci : var) {
            Map<String, Object> map = new HashMap<>();
            map.put("id",vci.getId());
            map.put("name", vci.getParameter().getName());
            map.put("title", vci.getParameter().getTitle());
            map.put("originName", vci.getParameter().getOriginName());
            map.put("description", vci.getParameter().getDescription());
            map.put("datatype", vci.getParameter().getDatatype().getName());
            list.add(map);
        }
        return list;
    }

    @Mappings({
            @Mapping(target = "name", expression = "java(map.get(\"name\").toString())"),
            @Mapping(target = "title", expression = "java(map.get(\"title\").toString())"),
            @Mapping(target = "originName", expression = "java(map.get(\"originName\").toString())"),
            @Mapping(target = "description", expression = "java(map.get(\"description\").toString())")}
    )
    Parameter map2Parameter(Map<String, Object> map);

    @Named("containerVOInput2ContainerInput")
    default List<VisualContainerInputParameter> containerVOInput2ContainerInput(List<Map<String, Object>> var, @Context DatatypeDAO datatypeDAO) {
        List<VisualContainerInputParameter> list = new ArrayList<>();
        for (Map<String, Object> map : var) {
//            发布组件的input的id为null
            if(map.get("id")==null){
                VisualContainerInputParameter visualContainerInputParameter = new VisualContainerInputParameter();
                Parameter parameter = map2Parameter(map);
                parameter.setDatatype(datatypeDAO.findByName(map.get("datatype").toString()));
                visualContainerInputParameter.setParameter(parameter);
                list.add(visualContainerInputParameter);
            }
        }
        return list;
    }

    @Named("containerVOProperties2ContainerProperties")
    default String containerVOProperties2ContainerProperties(Map<String,Object> var){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(var);
        }
        catch (JsonProcessingException e){
            e.printStackTrace();
            return "";
        }
    }

    @Named("containerProperties2ContainerVOProperties")
    default Map<String,Object> containerProperties2ContainerVOProperties(String var){
        Map<String,Object> properties =new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            if(var != null){
                properties = objectMapper.readValue(var, new TypeReference<Map<String,Object>>() {});
            }
        }
        catch (JsonProcessingException e){
            e.printStackTrace();
        }
        return properties;
    }

    @AfterMapping
    default void completeContainer(@MappingTarget VisualContainer container, VisualContainerVO containerVO, @Context VisualContainerDAO containerDAO, @Context DatatypeDAO datatypeDAO){
        if(containerVO.getId() == null){
            List<VisualContainerInputParameter> visualContainerInputParameters = containerVOInput2ContainerInput(containerVO.getInputs(), datatypeDAO);
            container.setInputs(visualContainerInputParameters);
        }else{
            // container1为旧的可视化容器，新的container数据需要将其覆盖
            VisualContainer container1 = containerDAO.findById(containerVO.getId()).orElseThrow(() -> new EntityNotFoundException(String.valueOf(containerVO.getId())));
            if(container1.getInputs().isEmpty()){
                List<VisualContainerInputParameter> visualContainerInputParameters = containerVOInput2ContainerInput(containerVO.getInputs(), datatypeDAO);
                container.setInputs(visualContainerInputParameters);
            }
            else {
                container.setInputs(container1.getInputs());
            }
            if(container.getInputs() != null){
                for (VisualContainerInputParameter input: container.getInputs()){
                    input.setContainer(container);
                }
            }
            container.setTemplatePath(container1.getTemplatePath());
            container.setProperties(container1.getProperties());
        }
    }

    @AfterMapping
    default void completeContainerVO(@MappingTarget VisualContainerVO containerVO){
        containerVO.setComponentType(NodeType.VISUALCONTAINER);
    }
}
