package aizoo.viewObject.mapper;

import aizoo.domain.ExperimentJob;
import aizoo.common.JobStatus;
import aizoo.repository.ExperimentJobDAO;
import aizoo.utils.ListEntity2ListVO;
import aizoo.viewObject.object.ExperimentJobVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.HashMap;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface ExperimentJobVOEntityMapper {
    ExperimentJobVOEntityMapper MAPPER = Mappers.getMapper(ExperimentJobVOEntityMapper.class);

    @Mappings({
            @Mapping(
                    source = "environment",
                    target = "environment",
                    qualifiedByName = "jobVOEnvironment2JobEnvironment"),
            @Mapping(target ="id",ignore = true),
            @Mapping(target ="jobKey",ignore = true),
            @Mapping(target ="jobStatus",ignore = true),
            @Mapping(target ="checkPoints",ignore = true),
            @Mapping(
                    source = "args",
                    target = "args",
                    qualifiedByName = "jobVOArgs2JobArgs")
    })
    ExperimentJob jobVO2Job(ExperimentJobVO experimentJobVO, @Context ExperimentJobDAO experimentJobDAO);

    @Mappings({
            @Mapping(
                    source = "jobStatus",
                    target = "jobStatus",
                    qualifiedByName = "jobJobStatus2JobVOJobStatus"),
            @Mapping(
                    source = "environment",
                    target = "environment",
                    qualifiedByName = "jobEnvironment2JobVOEnvironment"),
            @Mapping(target ="checkPoints",ignore = true),
            @Mapping(
            source = "args",
            target = "args",
            qualifiedByName = "jobArgs2JobVOArgs")
    })
    ExperimentJobVO job2JobVO(ExperimentJob experimentJob);

    @Named("jobJobStatus2JobVOJobStatus")
    default String jobJobStatus2JobVOJobStatus(JobStatus jobStatus) {
        if(jobStatus != null){
            return jobStatus.name();
        }
        return "Empty";
    }

    @Named("jobEnvironment2JobVOEnvironment")
    default Map<String, Map<String, Object>> jobEnvironment2JobVOEnvironment(String environment) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Map<String, Object>> map = new HashMap<>();
        try {
            if(environment != null)
                map = mapper.readValue(environment, new TypeReference<Map<String, Map<String, Object>>>() {});
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return map;
    }

    @Named("jobVOEnvironment2JobEnvironment")
    default String jobVOEnvironment2JobEnvironment(Map<String, Map<String, Object>> environment) {
        ObjectMapper mapper = new ObjectMapper();
        String result = null;
        try {
            if(environment != null)
                result = mapper.writeValueAsString(environment);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * jobArgs->jobVOArgs
     *
     * @param var
     * @return
     */
    @Named("jobArgs2JobVOArgs")
    default Map<String, Object> jobArgs2JobVOArgs(String var) {
        Map<String, Object> args = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            if (var != null) {
                args = objectMapper.readValue(var, new TypeReference<Map<String, Object>>() {
                });
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return args;
    }



    /**
     * jobVOArgs->jobArgs
     *
     * @param var
     * @return
     */
    @Named("jobVOArgs2JobArgs")
    default String jobVOArgs2JobArgs(Map<String, Object> var) {
        ObjectMapper objectMapper = new ObjectMapper();
        String result = null;
        try {
            if(var != null)
                result = objectMapper.writeValueAsString(var);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return result;
    }



    @Named("jobVOJobStatus2JobJobStatus")
    default JobStatus jobVOJobStatus2JobJobStatus(String var) {
        return JobStatus.valueOf(var);
    }

    @AfterMapping
    default void completeJob(ExperimentJobVO experimentJobVO, @MappingTarget ExperimentJob experimentJob, @Context ExperimentJobDAO experimentJobDAO) {
        if(experimentJobVO.getId() != null){
            ExperimentJob searchedExperimentJob = experimentJobDAO.findByJobKey(experimentJob.getJobKey());
            experimentJob.setUser(searchedExperimentJob.getUser());
            experimentJob.setGraph(searchedExperimentJob.getGraph());
            experimentJob.setId(searchedExperimentJob.getId());
            experimentJob.setCreateTime(searchedExperimentJob.getCreateTime());
            experimentJob.setUpdateTime(searchedExperimentJob.getUpdateTime());
            experimentJob.setJobStatus(jobVOJobStatus2JobJobStatus(experimentJobVO.getJobStatus()));
            experimentJob.setCheckPoints(searchedExperimentJob.getCheckPoints());
            experimentJob.setComponent(searchedExperimentJob.getComponent());
        }
    }

    @AfterMapping
    default void completeJobVO(ExperimentJob experimentJob, @MappingTarget ExperimentJobVO experimentJobVO) {
        String graphName = experimentJob.getGraph().getName();
        experimentJobVO.setGraphName(graphName);
        String username = experimentJob.getUser().getUsername();
        experimentJobVO.setGraphId(experimentJob.getGraph().getId());
        experimentJobVO.setUsername(username);
        experimentJobVO.setCheckPoints(ListEntity2ListVO.checkPoint2CheckPointVO(experimentJob.getCheckPoints()));
    }

}
