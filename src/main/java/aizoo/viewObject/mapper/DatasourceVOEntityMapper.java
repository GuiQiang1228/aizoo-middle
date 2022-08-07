package aizoo.viewObject.mapper;

import aizoo.domain.*;
import aizoo.common.ComponentType;
import aizoo.repository.*;
import aizoo.viewObject.object.DatasourceVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import javax.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface DatasourceVOEntityMapper {
    DatasourceVOEntityMapper MAPPER = Mappers.getMapper(DatasourceVOEntityMapper.class);
    ObjectMapper objectMapper = new ObjectMapper();

    @Mappings({
            @Mapping(
                    source = "datasourceOutputParameters",
                    target = "outputs",
                    qualifiedByName = "datasourceOutput2DatasourceVOMap"),
            @Mapping(
                    target = "username",
                    expression = "java(datasource.getUser().getUsername())"),
            @Mapping(
                    target = "namespace",
                    expression = "java(datasource.getNamespace().getNamespace())"),
            @Mapping(
                    target = "componentType",
                    ignore = true
            )
    })
    DatasourceVO datasource2DatasourceVO(Datasource datasource);

    @Mappings({
            @Mapping(
                    source = "outputs",
                    target = "datasourceOutputParameters",
                    ignore = true),
            @Mapping(target = "user",
                    expression = "java(userDAO.findByUsername(datasourceVO.getUsername()))"),
            @Mapping(
                    target = "namespace",
                    expression = "java(namespaceDAO.findByNamespace(datasourceVO.getNamespace()))"
            )
    })
    Datasource datasourceVO2Datasource(DatasourceVO datasourceVO, @Context DatasourceDAO datasourceDAO, @Context NamespaceDAO namespaceDAO, @Context UserDAO userDAO, @Context DatatypeDAO datatypeDAO);

    @Named("datasourceOutput2DatasourceVOMap")
    default List<Map<String, Object>> datasourceOutput2DatasourceVOMap(List<DatasourceOutputParameter> var) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (DatasourceOutputParameter cov : var) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", cov.getId());
            map.put("name", cov.getParameter().getName());
            map.put("title", cov.getParameter().getTitle());
            map.put("originName", cov.getParameter().getOriginName());
            map.put("description", cov.getParameter().getDescription());
            if (cov.getParameter().getDatatype() != null)
                map.put("datatype", cov.getParameter().getDatatype().getName());
            else
                map.put("datatype", null);
            list.add(map);
        }
        return list;
    }

    default List<DatasourceOutputParameter> datasourceVOOutput2DatasourceVariables(List<Map<String, Object>> var, @Context DatatypeDAO datatypeDAO) {
        List<DatasourceOutputParameter> list = new ArrayList<>();
        for (Map<String, Object> map : var) {
            DatasourceOutputParameter datasourceOutputParameter = new DatasourceOutputParameter();
            Parameter parameter = map2Parameter(map);
            parameter.setDatatype(datatypeDAO.findByName(map.get("datatype").toString()));
            datasourceOutputParameter.setParameter(parameter);
            list.add(datasourceOutputParameter);
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

    @AfterMapping
    default void completeDatasourceVO(@MappingTarget DatasourceVO datasourceVO) {
        datasourceVO.setComponentType(ComponentType.DATASOURCE);
    }

    @AfterMapping
    default void completeDatasource(@MappingTarget Datasource datasource, DatasourceVO datasourceVO, @Context DatasourceDAO datasourceDAO, @Context DatatypeDAO datatypeDAO) {
        Datasource datasource1;
        if (datasourceVO.getId() != null) {
            datasource1 = datasourceDAO.findById(datasourceVO.getId()).orElseThrow(() -> new EntityNotFoundException(String.valueOf(datasourceVO.getId())));
            datasource.setDatasourceOutputParameters(datasource1.getDatasourceOutputParameters());
        } else {
            List<Map<String, Object>> outputs = datasourceVO.getOutputs();
            List<DatasourceOutputParameter> datasourceOutputParameters = datasourceVOOutput2DatasourceVariables(outputs, datatypeDAO);
            datasource.setDatasourceOutputParameters(datasourceOutputParameters);
        }
    }
}
