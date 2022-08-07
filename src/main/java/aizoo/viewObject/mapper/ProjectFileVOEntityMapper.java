package aizoo.viewObject.mapper;



import aizoo.domain.*;
import aizoo.repository.*;

import javax.persistence.EntityNotFoundException;

import aizoo.viewObject.object.ComponentVO;
import aizoo.viewObject.object.ProjectFileVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface ProjectFileVOEntityMapper {
    ProjectFileVOEntityMapper MAPPER = Mappers.getMapper(ProjectFileVOEntityMapper.class);
    ObjectMapper objectMapper = new ObjectMapper();
    @Mappings({
            @Mapping(
                    target = "username",
                    expression = "java(projectFile.getUser().getUsername())"),
            @Mapping(
                    target = "projectId",
                    expression = "java(projectFile.getProject().getId())")
    })
    ProjectFileVO projectFile2projectFileVO(ProjectFile projectFile);

    @Mappings({
            @Mapping(
                    target = "user",
                    expression = "java(userDAO.findByUsername(projectFileVO.getUsername()))")
    })
    ProjectFile projectFileVO2projectFile(ProjectFileVO projectFileVO, @Context ProjectFileDAO projectFileDAO, @Context UserDAO userDAO, @Context ProjectDAO projectDAO);

    @AfterMapping
    default void completeProjectFile(@MappingTarget ProjectFile projectFile, ProjectFileVO projectFileVO, @Context ProjectDAO projectDAO) {
        Project project = projectDAO.findById(projectFileVO.getProjectId()).orElseThrow(()->new EntityNotFoundException());
       projectFile.setProject(project);
    }

}
