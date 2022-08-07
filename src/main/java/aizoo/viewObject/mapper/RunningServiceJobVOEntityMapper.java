package aizoo.viewObject.mapper;

import aizoo.domain.ServiceJob;
import aizoo.viewObject.object.RunningServiceJobVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface RunningServiceJobVOEntityMapper {
    RunningServiceJobVOEntityMapper MAPPER = Mappers.getMapper(RunningServiceJobVOEntityMapper.class);

    RunningServiceJobVO Entity2VO(ServiceJob serviceJob);
}
