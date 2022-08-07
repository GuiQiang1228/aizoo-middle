package aizoo.viewObject.mapper;

import aizoo.domain.ApplicationResult;
import aizoo.domain.VisualContainer;
import aizoo.viewObject.object.ApplicationResultVO;
import aizoo.viewObject.object.ApplicationVO;
import aizoo.viewObject.object.VisualContainerVO;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface ApplicationResultVOEntityMapper {
    ApplicationResultVOEntityMapper MAPPER = Mappers.getMapper(ApplicationResultVOEntityMapper.class);

    @Mappings({
            @Mapping(
                    target = "applicationName",
                    expression = "java(appResult.getApplication().getName())"
            ),
            @Mapping(
                    source = "visualContainer",
                    target = "visualContainerVO",
                    ignore = true
            )
    })
    ApplicationResultVO appResult2AppResultVO(ApplicationResult appResult);

    @AfterMapping
    default void completeAppResultVO(@MappingTarget ApplicationResultVO appResultVO, ApplicationResult appResult){
        VisualContainerVO visualContainerVO = ContainerVOEntityMapper.MAPPER.container2ContainerVO(appResult.getVisualContainer());
        appResultVO.setVisualContainerVO(visualContainerVO);
    }
}
