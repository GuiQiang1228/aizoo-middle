package aizoo.viewObject.mapper;


import aizoo.domain.*;
import aizoo.viewObject.object.CheckPointVO;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface CheckPointVOEntityMapper {

    CheckPointVOEntityMapper MAPPER = Mappers.getMapper(CheckPointVOEntityMapper.class);

    CheckPointVO CheckPoint2CheckPointVO(CheckPoint checkPoint);
}
