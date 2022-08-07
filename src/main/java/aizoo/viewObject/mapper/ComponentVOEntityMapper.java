package aizoo.viewObject.mapper;

import aizoo.domain.*;
import aizoo.repository.*;
import aizoo.viewObject.object.ComponentVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;


import javax.persistence.EntityNotFoundException;
import java.util.*;

@Mapper(componentModel = "spring")
public interface ComponentVOEntityMapper {
    ComponentVOEntityMapper MAPPER = Mappers.getMapper(ComponentVOEntityMapper.class);
    ObjectMapper objectMapper = new ObjectMapper();

    @Mappings({
            @Mapping(
                    source = "inputs",
                    target = "inputs",
                    qualifiedByName = "componentInput2ComponentVOMap"),

            @Mapping(
                    source = "outputs",
                    target = "outputs",
                    qualifiedByName = "componentOutput2ComponentVOMap"),
            @Mapping(
                    source = "properties",
                    target = "properties",
                    qualifiedByName = "componentProperties2ComponentVOProperties"
            ),
            @Mapping(
                    source = "forkFrom",
                    target = "forkFromUser",
                    qualifiedByName = "componentForkFrom2ComponentVOForkFromUser"
            ),
            @Mapping(
                    source = "namespace",
                    target = "namespace",
                    ignore = true)

    })
    ComponentVO component2ComponentVO(Component component);

    @Mappings({
            @Mapping(
                    source = "inputs",
                    target = "inputs",
                    ignore = true),

            @Mapping(
                    source = "outputs",
                    target = "outputs",
                    ignore = true),
            @Mapping(
                    source = "properties",
                    target = "properties",
                    qualifiedByName = "ComponentVOProperties2ComponentProperties"
            ),
            /*@Mapping(
                    target = "user",
                    expression = "java(userDAO.findByUsername(componentVO.getUsername()))")*/
            /*@Mapping(
                    target = "namespace",
                    expression = "java(namespaceDAO.findByNamespace(componentVO.getNamespace()))")*/
            @Mapping(
                    target = "namespace",
                    source = "namespace",
                    ignore = true
            )

    })
    Component componentVO2Component(ComponentVO componentVO, @Context DatatypeDAO datatypeDAO, @Context ComponentDAO componentDAO, @Context NamespaceDAO namespaceDAO, @Context UserDAO userDAO);//@Context 可以给qualifiedByName中的方法提供参数

    @Mappings({
            @Mapping(target = "name", expression = "java(map.get(\"name\").toString())"),
            @Mapping(target = "title", expression = "java(map.get(\"title\").toString())"),
            @Mapping(target = "originName", expression = "java(map.get(\"originName\").toString())"),
            @Mapping(target = "description", expression = "java(map.get(\"description\").toString())")}
    )
    Parameter map2Parameter(Map<String, Object> map);

    /**
     * componentVOInput->ComponentInput
     *
     * @param var
     * @param datatypeDAO
     * @return
     */
    @Named("componentVOInput2ComponentInput")
    //这里的DAO可以直接接到没有实体方法的componentVO2Component的参数，所以不用再使用@Context
    default List<ComponentInputParameter> componentVOInput2ComponentParameters(List<Map<String, Object>> var, @Context DatatypeDAO datatypeDAO) {
        List<ComponentInputParameter> list = new ArrayList<>();
        for (Map<String, Object> map : var) {
//            新发布组件的input的id为null，需要手动新建ComponentInputParameter
            if (map.get("id") == null) {
                ComponentInputParameter componentInputParameter = new ComponentInputParameter();
                Parameter parameter = map2Parameter(map);
                parameter.setDatatype(datatypeDAO.findByName(map.get("datatype").toString()));
                componentInputParameter.setParameter(parameter);
                list.add(componentInputParameter);
            }
        }
        return list;
    }

    /**
     * componentVOOutput->ComponentOutput
     *
     * @param var
     * @param datatypeDAO
     * @return
     */
    @Named("componentVOOutput2ComponentOutput")
    default List<ComponentOutputParameter> componentVOOutput2ComponentParameters(List<Map<String, Object>> var, @Context DatatypeDAO datatypeDAO) {
        List<ComponentOutputParameter> list = new ArrayList<>();
        for (Map<String, Object> map : var) {
//           新发布组件的output的id为null，需要手动新建ComponentOutputParameter
            if (map.get("id") == null) {
                ComponentOutputParameter componentOutputParameter = new ComponentOutputParameter();
                Parameter parameter = map2Parameter(map);
                parameter.setDatatype(datatypeDAO.findByName(map.get("datatype").toString()));
                componentOutputParameter.setParameter(parameter);
                list.add(componentOutputParameter);
            }
        }
        return list;
    }

    /**
     * componentInput->componentVOInputs
     *
     * @param componentInputParameterList
     * @return
     */
    @Named("componentInput2ComponentVOMap")
    default List<Map<String, Object>> componentInput2ComponentVOMap(List<ComponentInputParameter> componentInputParameterList) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (ComponentInputParameter componentInputParameter : componentInputParameterList) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", componentInputParameter.getId());
            map.put("name", componentInputParameter.getParameter().getName());
            map.put("title", componentInputParameter.getParameter().getTitle());
            map.put("originName", componentInputParameter.getParameter().getOriginName());
            map.put("description", componentInputParameter.getParameter().getDescription());
            if (componentInputParameter.getParameter().getDatatype() != null) {
                map.put("datatype", componentInputParameter.getParameter().getDatatype().getName());
            } else {
                map.put("datatype", null);
            }
            list.add(map);
        }
        return list;
    }

    /**
     * componentOutput->componentVOOutputs
     *
     * @param componentOutputParameterList
     * @return
     */
    @Named("componentOutput2ComponentVOMap")
    default List<Map<String, Object>> componentOutput2ComponentVOMap(List<ComponentOutputParameter> componentOutputParameterList) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (ComponentOutputParameter componentOutputParameter : componentOutputParameterList) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", componentOutputParameter.getId());
            map.put("name", componentOutputParameter.getParameter().getName());
            map.put("title", componentOutputParameter.getParameter().getTitle());
            map.put("originName", componentOutputParameter.getParameter().getOriginName());
            map.put("description", componentOutputParameter.getParameter().getDescription());
            map.put("isSelf", componentOutputParameter.getIsSelf());
            if (componentOutputParameter.getParameter().getDatatype() != null) {
                map.put("datatype", componentOutputParameter.getParameter().getDatatype().getName());
            } else {
                map.put("datatype", null);
            }
            list.add(map);
        }
        return list;
    }

    /**
     * componentVOProperties->componentProperties
     *
     * @param var
     * @return
     */
    @Named("componentVOProperties2componentProperties")
    default String componentVOProperties2componentProperties(Map<String, Object> var) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(var);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * componentProperties->componentVOProperties
     *
     * @param var
     * @return
     */
    @Named("componentProperties2componentVOProperties")
    default Map<String, Object> componentProperties2componentVOProperties(String var) {
        Map<String, Object> properties = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            if (var != null) {
                properties = objectMapper.readValue(var, new TypeReference<Map<String, Object>>() {
                });
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return properties;
    }

    /**
     * componentForkFrom(user对象)->ComponentVOForkFromUser(username)
     *
     * @param forkFrom
     * @return
     */
    @Named("componentForkFrom2ComponentVOForkFromUser")
    default String componentForkFrom2ComponentVOForkFromUser(Component forkFrom) {
        if (forkFrom != null) {
            User user = forkFrom.getUser();
            if (user != null) {
                return user.getUsername();
            }
        }
        return null;
    }

    /**
     * componentVO->component 转换完成时，补全信息
     *
     * @param component    传入的component是new出来的，未进入hibernate的session管理，返回时也请不要返回直接从数据库中查出的对象
     * @param componentDAO
     * @param componentVO
     * @param datatypeDAO
     * @param userDAO
     * @param namespaceDAO
     */
    @AfterMapping
    default void completeComponent(@MappingTarget Component component, @Context ComponentDAO componentDAO, ComponentVO componentVO, @Context DatatypeDAO datatypeDAO, @Context UserDAO userDAO, @Context NamespaceDAO namespaceDAO) {
        Component component1;
//      1. 新组件没有id，input和output也没插入到数据库，需要新建input和output，建立外键关联（input、output、user、namespace）
        if (componentVO.getId() == null) {
            List<ComponentInputParameter> componentInputParameters = componentVOInput2ComponentParameters(componentVO.getInputs(), datatypeDAO);
            List<ComponentOutputParameter> componentOutputParameters = componentVOOutput2ComponentParameters(componentVO.getOutputs(), datatypeDAO);
            component.setInputs(componentInputParameters);
            component.setOutputs(componentOutputParameters);
            component.setUser(userDAO.findByUsername(componentVO.getUsername()));
            component.setNamespace(namespaceDAO.findByNamespace(componentVO.getNamespace()));
        } else {
            // 2. 已有组件，则根据vo里填的值，完成修改
            component1 = componentDAO.findById(componentVO.getId()).orElseThrow(() -> new EntityNotFoundException(String.valueOf(componentVO.getId())));
//            发布组件类型的vo->entity转换，使用的是vo内的输入输出
//            发布组件的input output每次都根据vo中的生成并存入数据库
//            未发布的组件input和output都未存入数据库，只会在首次发布时存入
//            后期重复发布的时候会事前置空，防止累积旧的输入输出
            if (component1.getOutputs().isEmpty() && component1.getInputs().isEmpty()) {
                List<ComponentInputParameter> componentInputParameters = componentVOInput2ComponentParameters(componentVO.getInputs(), datatypeDAO);
                List<ComponentOutputParameter> componentOutputParameters = componentVOOutput2ComponentParameters(componentVO.getOutputs(), datatypeDAO);
                component.setInputs(componentInputParameters);
                component.setOutputs(componentOutputParameters);
            } else {
//                查询类型的vo->entity转换，component未发布时不会执行这个代码块
                component.setInputs(component1.getInputs());
                component.setOutputs(component1.getOutputs());
            }
            component.setChildComponentIdList(component1.getChildComponentIdList());
            component.setPath(component1.getPath());
            component.setForkBy(component1.getForkBy());
            component.setGraph(component1.getGraph());
            component.setNamespace(component1.getNamespace());
            component.setUser(component1.getUser());
            component.setFileList(component1.getFileList());
            if (component.getInputs() != null) {
                for (ComponentInputParameter componentInputParameter : component.getInputs()) {
                    componentInputParameter.setComponent(component);
                }
            }
            if (component.getOutputs() != null) {
                for (ComponentOutputParameter componentOutputParameter : component.getOutputs()) {
                    componentOutputParameter.setComponent(component);
                }
            }
        }
    }

    @AfterMapping
    default void completeComponentVO(@MappingTarget ComponentVO componentVO, Component component) {
        if (component.getForkFrom() != null) {
            if (component.getForkFrom().getUser() != null) {
                componentVO.setForkFromUser(component.getForkFrom().getUser().getUsername());
            }
        }
        if (component.getNamespace() != null) {
            componentVO.setNamespace(component.getNamespace().getNamespace());
        }
        if (component.getUser() != null) {
            componentVO.setUsername(component.getUser().getUsername());
        }

        if (component.getGraph() != null) {
            componentVO.setGraphId(component.getGraph().getId());
        }
    }
}