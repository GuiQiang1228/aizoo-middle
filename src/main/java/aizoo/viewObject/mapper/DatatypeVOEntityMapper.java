package aizoo.viewObject.mapper;

import aizoo.domain.Datatype;
import aizoo.viewObject.object.DatatypeVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface DatatypeVOEntityMapper {
    DatatypeVOEntityMapper MAPPER = Mappers.getMapper(DatatypeVOEntityMapper.class);

    DatatypeVO Datatype2DatatypeVO(Datatype datatype);
}
