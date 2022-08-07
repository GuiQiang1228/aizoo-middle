package aizoo.viewObject.mapper;

import aizoo.domain.ExperimentJob;
import aizoo.viewObject.object.BaseVO;
import aizoo.viewObject.object.ExperimentJobCheckpointVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface ExperimentJobCheckpointVOEntityMapper {
    ExperimentJobCheckpointVOEntityMapper MAPPER = Mappers.getMapper(ExperimentJobCheckpointVOEntityMapper.class);
    ExperimentJobCheckpointVO jobCheckpoint2JobCheckpointVO(ExperimentJob experimentJob);
}
