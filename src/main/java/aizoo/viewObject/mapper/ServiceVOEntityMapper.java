package aizoo.viewObject.mapper;

import aizoo.common.NodeType;
import aizoo.domain.*;
import aizoo.repository.DatatypeDAO;
import aizoo.repository.ServiceDAO;
import aizoo.viewObject.object.ServiceVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import javax.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface ServiceVOEntityMapper {
    ServiceVOEntityMapper MAPPER = Mappers.getMapper(ServiceVOEntityMapper.class);

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
                    source = "namespace",
                    target = "namespace",
                    ignore = true),
            @Mapping(
                    source = "properties",
                    target = "properties",
                    qualifiedByName = "serviceVOProperties2ServiceProperties"
            )
    })
    Service ServiceVO2Service(ServiceVO service, @Context ServiceDAO serviceDAO, @Context DatatypeDAO datatypeDAO);

    @Mappings({
            @Mapping(
                    source = "inputs",
                    target = "inputs",
                    qualifiedByName = "serviceInput2ServiceVOMap"),

            @Mapping(
                    source = "outputs",
                    target = "outputs",
                    qualifiedByName = "serviceOutput2ServiceVOMap"),
            @Mapping(
                    target = "namespace",
                    expression = "java(service.getNamespace().getNamespace())"),
            @Mapping(target = "componentType",
                     ignore = true),
            @Mapping(
                    source = "properties",
                    target = "properties",
                    qualifiedByName = "serviceProperties2ServiceVOProperties"
            )

    })
    ServiceVO Service2ServiceVO(Service service);


    @Mappings({
            @Mapping(target = "name", expression = "java(map.get(\"name\").toString())"),
            @Mapping(target = "title", expression = "java(map.get(\"title\").toString())"),
            @Mapping(target = "originName", expression = "java(map.get(\"originName\").toString())"),
            @Mapping(target = "description", expression = "java(map.get(\"description\").toString())")}
    )
    Parameter map2Parameter(Map<String, Object> map);

    @Named("serviceVOProperties2ServiceProperties")
    default String serviceVOProperties2ServiceProperties(Map<String,Object> var){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            if(var != null) {
                return objectMapper.writeValueAsString(var);
            }
            else
                return null;
        }
        catch (JsonProcessingException e){
            e.printStackTrace();
            return "";
        }
    }

    @Named("serviceProperties2ServiceVOProperties")
    default Map<String,Object> serviceProperties2ServiceVOProperties(String var){
        Map<String,Object> properties =new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            if(var != null){
                properties = objectMapper.readValue(var, new TypeReference<Map<String,Object>>() {});
            }
            else
                return null;
        }
        catch (JsonProcessingException e){
            e.printStackTrace();
        }
        return properties;
    }

    @Named("serviceVOInput2ServiceInput")
    default List<ServiceInputParameter> serviceVOInput2ServiceParameters(List<Map<String, Object>> var, @Context DatatypeDAO datatypeDAO) {
        List<ServiceInputParameter> list = new ArrayList<>();
        for (Map<String, Object> map : var) {
//            发布组件的input的id为null
            if(map.get("id")==null){
                ServiceInputParameter serviceInputParameter = new ServiceInputParameter();
                Parameter parameter = map2Parameter(map);
                parameter.setDatatype(datatypeDAO.findByName(map.get("datatype").toString()));
                serviceInputParameter.setParameter(parameter);
                list.add(serviceInputParameter);
            }
        }
        return list;
    }

    @Named("serviceVOOutput2ServiceOutput")
    default List<ServiceOutputParameter> serviceVOOutput2ServiceOutputParameters(List<Map<String, Object>> var, @Context DatatypeDAO datatypeDAO) {
        List<ServiceOutputParameter> list = new ArrayList<>();
        for (Map<String, Object> map : var) {
//            发布组件的output的id为null
            if(map.get("id")==null){
                ServiceOutputParameter serviceOutputParameter = new ServiceOutputParameter();
                Parameter parameter = map2Parameter(map);
                parameter.setDatatype(datatypeDAO.findByName(map.get("datatype").toString()));
                serviceOutputParameter.setParameter(parameter);
                list.add(serviceOutputParameter);
            }
        }
        return list;
    }

    @Named("serviceInput2ServiceVOMap")
    default List<Map<String, Object>> serviceInput2ServiceVOMap(List<ServiceInputParameter> var) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (ServiceInputParameter civ : var) {
            Map<String, Object> map = new HashMap<>();
            map.put("id",civ.getId());
            map.put("name", civ.getParameter().getName());
            map.put("title", civ.getParameter().getTitle());
            map.put("originName", civ.getParameter().getOriginName());
            map.put("description", civ.getParameter().getDescription());
            map.put("datatype",civ.getParameter().getDatatype().getName());
            list.add(map);
        }
        return list;
    }

    @Named("serviceOutput2ServiceVOMap")
    default List<Map<String, Object>> serviceOutput2ServiceVOMap(List<ServiceOutputParameter> var) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (ServiceOutputParameter cov : var) {
            Map<String, Object> map = new HashMap<>();
            map.put("id",cov.getId());
            map.put("name", cov.getParameter().getName());
            map.put("title", cov.getParameter().getTitle());
            map.put("originName", cov.getParameter().getOriginName());
            map.put("description", cov.getParameter().getDescription());
            map.put("isSelf", cov.getIsSelf());
            map.put("datatype",cov.getParameter().getDatatype().getName());
            list.add(map);
        }
        return list;
    }

    @AfterMapping
    default void completeService(@MappingTarget Service service, @Context ServiceDAO serviceDAO, ServiceVO serviceVO, @Context DatatypeDAO datatypeDAO) {
        Service service1;
//        上传的组件input和output根据vo中的给数据库新建设置
        if(serviceVO.getId() == null){
            List<ServiceInputParameter> serviceInputParameters = serviceVOInput2ServiceParameters(serviceVO.getInputs(),datatypeDAO);
            List<ServiceOutputParameter> serviceOutputParameters = serviceVOOutput2ServiceOutputParameters(serviceVO.getOutputs(),datatypeDAO);
            service.setInputs(serviceInputParameters);
            service.setOutputs(serviceOutputParameters);
        }
        else{
            service1 = serviceDAO.findById(service.getId()).orElseThrow(() -> new EntityNotFoundException(String.valueOf(service.getId())));
//            发布组件类型的vo->entity转换  发布组件的input output每次都根据vo中的生成并存入数据库
//            发布组件的input，output在首次发布的时候数据库中对应input和output为null。后期重复发布的时候会事前置空，防止累积旧的输入输出
            if(service1.getOutputs().isEmpty()&&service1.getInputs().isEmpty()){
                List<ServiceInputParameter> serviceInputParameters = serviceVOInput2ServiceParameters(serviceVO.getInputs(),datatypeDAO);
                List<ServiceOutputParameter> serviceOutputParameters = serviceVOOutput2ServiceOutputParameters(serviceVO.getOutputs(),datatypeDAO);
                service.setInputs(serviceInputParameters);
                service.setOutputs(serviceOutputParameters);
            }else {
//                查询类型的vo->entity转换
                service.setInputs(service1.getInputs());
                service.setOutputs(service1.getOutputs());
            }
            service.setPath(service1.getPath());
            service.setForkBy(service1.getForkBy());
            service.setGraph(service1.getGraph());
            service.setNamespace(service1.getNamespace());
            service.setUser(service1.getUser());
            service.setFileList(service1.getFileList());
            if (service.getInputs()!=null){
                for(ServiceInputParameter serviceInputParameter:service.getInputs()){
                    serviceInputParameter.setService(service);
                }
            }
            if (service.getOutputs()!=null){
                for(ServiceOutputParameter serviceOutputParameter:service.getOutputs()){
                    serviceOutputParameter.setService(service);
                }
            }
            service.setServiceVersion(service1.getServiceVersion());
            service.setReleased(service1.isReleased());
        }
    }

    @AfterMapping
    default void completeServiceVO(@MappingTarget ServiceVO serviceVO, Service service){
        serviceVO.setToken(service.getToken());
        serviceVO.setComponentType(NodeType.SERVICE);
        if(service.getGraph() != null)
            serviceVO.setGraphId(service.getGraph().getId());
    }
}
