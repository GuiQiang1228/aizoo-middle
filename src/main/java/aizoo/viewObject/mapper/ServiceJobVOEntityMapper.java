package aizoo.viewObject.mapper;

import aizoo.common.JobStatus;
import aizoo.domain.ServiceJob;
import aizoo.repository.ServiceJobDAO;
import aizoo.viewObject.object.ServiceJobVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.HashMap;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface ServiceJobVOEntityMapper {
    ServiceJobVOEntityMapper MAPPER = Mappers.getMapper(ServiceJobVOEntityMapper.class);

    @Mappings({
            @Mapping(
                    source = "environment",
                    target = "environment",
                    qualifiedByName = "serviceJobVOEnvironment2ServiceJobEnvironment"),
            @Mapping(target ="id",ignore = true),
            @Mapping(target ="jobKey",ignore = true),
            @Mapping(target ="jobStatus",ignore = true),
            @Mapping(
                    source = "args",
                    target = "args",
                    qualifiedByName = "jobVOArgs2JobArgs")
    })
    ServiceJob serviceJobVO2ServiceJob(ServiceJobVO serviceJobVO, @Context ServiceJobDAO serviceJobDAO);


    @Mappings({
            @Mapping(
                    source = "jobStatus",
                    target = "jobStatus",
                    qualifiedByName = "serviceJobJobStatus2ServiceJobVOJobStatus"),
            @Mapping(
                    source = "environment",
                    target = "environment",
                    qualifiedByName = "jobEnvironment2JobVOEnvironment"),
            @Mapping(
                    source = "args",
                    target = "args",
                    qualifiedByName = "jobArgs2JobVOArgs")
    })
    ServiceJobVO serviceJob2ServiceJobVO(ServiceJob serviceJob);

    @Named("serviceJobJobStatus2ServiceJobVOJobStatus")
    default String serviceJobJobStatus2ServiceJobVOJobStatus(JobStatus jobStatus) {
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

    @Named("serviceJobVOEnvironment2ServiceJobEnvironment")
    default String serviceJobVOEnvironment2ServiceJobEnvironment(Map<String, Map<String, Object>> environment) {
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



    @AfterMapping
    default void completeServiceJob(ServiceJobVO serviceJobVO, @MappingTarget ServiceJob serviceJob, @Context ServiceJobDAO serviceJobDAO) {
        if(serviceJobVO.getId() != null){
            ServiceJob searchedServiceJob = serviceJobDAO.findByJobKey(serviceJob.getJobKey());
            serviceJob.setUser(searchedServiceJob.getUser());
            serviceJob.setGraph(searchedServiceJob.getGraph());
            serviceJob.setId(searchedServiceJob.getId());
            serviceJob.setCreateTime(searchedServiceJob.getCreateTime());
            serviceJob.setUpdateTime(searchedServiceJob.getUpdateTime());
        }
    }

    @AfterMapping
    default void completeServiceJobVO(ServiceJob serviceJob, @MappingTarget ServiceJobVO serviceJobVO) {
        String graphName = serviceJob.getGraph().getName();
        String username = serviceJob.getUser().getUsername();
        serviceJobVO.setGraphName(graphName);
        serviceJobVO.setUsername(username);
        serviceJobVO.setGraphId(serviceJob.getGraph().getId());
    }
}
