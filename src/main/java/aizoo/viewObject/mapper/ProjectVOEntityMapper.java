package aizoo.viewObject.mapper;

import aizoo.domain.Component;
import aizoo.domain.Project;
import aizoo.viewObject.object.ComponentVO;
import aizoo.viewObject.object.ProjectVO;
import aizoo.viewObject.pagebleUtils.VO2EntityMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface ProjectVOEntityMapper {
    ProjectVOEntityMapper MAPPER = Mappers.getMapper(ProjectVOEntityMapper.class);
    ObjectMapper objectMapper = new ObjectMapper();

    ProjectVO project2ProjectVO(Project project);


    @AfterMapping
    default void completeProjectVO(Project project, @MappingTarget ProjectVO projectVO) {
        projectVO.setApplicationVOList(VO2EntityMapper.mapEntityList2VOList(ApplicationVOEntityMapper.MAPPER::application2ApplicationVO, project.getApplications()));
        projectVO.setComponentVOList(VO2EntityMapper.mapEntityList2VOList(ComponentVOEntityMapper.MAPPER::component2ComponentVO, project.getComponents()));
        projectVO.setGraphVOList(VO2EntityMapper.mapEntityList2VOList(GraphVOEntityMapper.MAPPER::graph2GraphVO, project.getGraphs()));
        projectVO.setDatasourceVOList(VO2EntityMapper.mapEntityList2VOList(DatasourceVOEntityMapper.MAPPER::datasource2DatasourceVO, project.getDatasourceList()));
        projectVO.setExperimentJobVOList(VO2EntityMapper.mapEntityList2VOList(ExperimentJobVOEntityMapper.MAPPER::job2JobVO, project.getExperimentJobs()));
        projectVO.setServiceVOList(VO2EntityMapper.mapEntityList2VOList(ServiceVOEntityMapper.MAPPER::Service2ServiceVO, project.getServices()));
        projectVO.setServiceJobVOList(VO2EntityMapper.mapEntityList2VOList(ServiceJobVOEntityMapper.MAPPER::serviceJob2ServiceJobVO, project.getServiceJobs()));
        projectVO.setCodeVOList(VO2EntityMapper.mapEntityList2VOList(CodeVOEntityMapper.MAPPER::code2CodeVO, project.getCodes()));
        projectVO.setMirrorVOList(VO2EntityMapper.mapEntityList2VOList(MirrorVOEntityMapper.MAPPER::mirror2MirrorVO, project.getMirrors()));
        projectVO.setMirrorJobVOList(VO2EntityMapper.mapEntityList2VOList(MirrorJobVOEntityMapper.MAPPER::job2JobVO, project.getMirrorJobs()));
        projectVO.setProjectFileVOList(VO2EntityMapper.mapEntityList2VOList(ProjectFileVOEntityMapper.MAPPER::projectFile2projectFileVO, project.getProjectFiles()));
    }
}
