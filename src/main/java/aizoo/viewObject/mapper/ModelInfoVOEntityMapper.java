package aizoo.viewObject.mapper;

import aizoo.domain.ModelInfo;
import aizoo.viewObject.object.ModelInfoVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface ModelInfoVOEntityMapper {
    ModelInfoVOEntityMapper MAPPER = Mappers.getMapper(ModelInfoVOEntityMapper.class);
    ObjectMapper objectMapper = new ObjectMapper();

    @Mappings({
            @Mapping(
                    target = "username",
                    expression = "java(modelInfo.getUser().getUsername())"
            ),
            @Mapping(
                    target = "modelCategoryId",
                    expression = "java(modelInfo.getModelCategory().getId())"
            ),
    })
    ModelInfoVO modelInfo2ModelInfoVO(ModelInfo modelInfo);
}
