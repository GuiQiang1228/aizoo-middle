package aizoo.viewObject.mapper;

import aizoo.common.JobStatus;
import aizoo.domain.ExperimentJob;
import aizoo.domain.MirrorJob;
import aizoo.repository.ExperimentJobDAO;
import aizoo.repository.MirrorJobDAO;
import aizoo.utils.ListEntity2ListVO;
import aizoo.viewObject.object.ExperimentJobVO;
import aizoo.viewObject.object.MirrorJobVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.HashMap;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface MirrorJobVOEntityMapper {
    MirrorJobVOEntityMapper MAPPER = Mappers.getMapper(MirrorJobVOEntityMapper.class);

    @Mappings({
            @Mapping(
                    source = "environment",
                    target = "environment",
                    qualifiedByName = "jobVOEnvironment2JobEnvironment"),
            @Mapping(
                    source = "args",
                    target = "args",
                    qualifiedByName = "jobVOArgs2JobArgs"),
            @Mapping(
                    source = "userArgs",
                    target = "userArgs",
                    qualifiedByName = "jobVOUserArgs2JobUserArgs"),
            @Mapping(target ="id",ignore = true),
            @Mapping(target ="jobKey",ignore = true),
            @Mapping(target ="jobStatus",ignore = true)
    })
    MirrorJob jobVO2Job(MirrorJobVO mirrorJobVO, @Context MirrorJobDAO mirrorJobDAO);

    @Mappings({
            @Mapping(
                    source = "jobStatus",
                    target = "jobStatus",
                    qualifiedByName = "jobJobStatus2JobVOJobStatus"),
            @Mapping(
                    source = "environment",
                    target = "environment",
                    qualifiedByName = "jobArgs2JobVOEnvironment"),
            @Mapping(
                    source = "args",
                    target = "args",
                    qualifiedByName = "jobArgs2JobVOArgs"),
            @Mapping(
                    source = "userArgs",
                    target = "userArgs",
                    qualifiedByName = "jobUserArgs2JobVOUserArgs"),
            @Mapping(target ="jobKey",ignore = true)
    })
    MirrorJobVO job2JobVO(MirrorJob mirrorJob);

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
     * jobUserArgs->jobVOUserArgs
     *
     * @param var
     * @return
     */
    @Named("jobUserArgs2JobVOUserArgs")
    default Map<String, Object> jobUserArgs2JobVOUserArgs(String var) {
        Map<String, Object> userArgs = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            if (var != null) {
                userArgs = objectMapper.readValue(var, new TypeReference<Map<String, Object>>() {
                });
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return userArgs;
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



    /**
     * jobVOUserArgs->jobUserArgs
     *
     * @param var
     * @return
     */
    @Named("jobVOUserArgs2JobUserArgs")
    default String jobVOUserArgs2JobUserArgs(Map<String, Object> var) {
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
    default void completeJob(MirrorJobVO mirrorJobVO, @MappingTarget MirrorJob mirrorJob, @Context MirrorJobDAO mirrorJobDAO) {
        if(mirrorJobVO.getId() != null){
            MirrorJob searchedMirrorJob= mirrorJobDAO.findByJobKey(mirrorJob.getJobKey());
            mirrorJob.setUser(searchedMirrorJob.getUser());
            mirrorJob.setId(searchedMirrorJob.getId());
            mirrorJob.setCreateTime(searchedMirrorJob.getCreateTime());
            mirrorJob.setUpdateTime(searchedMirrorJob.getUpdateTime());
            mirrorJob.setJobStatus(jobVOJobStatus2JobJobStatus(mirrorJobVO.getJobStatus()));
        }
    }

    @AfterMapping
    default void completeJobVO(MirrorJob mirrorJob, @MappingTarget MirrorJobVO mirrorJobVO) {
        String username = mirrorJob.getUser().getUsername();
        mirrorJobVO.setUsername(username);
        mirrorJobVO.setMirrorId(mirrorJob.getMirror().getId());
        mirrorJobVO.setCodeId(mirrorJob.getCode().getId());
    }
}
