package aizoo.viewObject.mapper;

import aizoo.common.JobStatus;
import aizoo.domain.Application;
import aizoo.domain.ApplicationResult;
import aizoo.repository.ApplicationDAO;
import aizoo.repository.UserDAO;
import aizoo.viewObject.object.ApplicationResultVO;
import aizoo.viewObject.object.ApplicationVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import javax.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface ApplicationVOEntityMapper {
    ApplicationVOEntityMapper MAPPER = Mappers.getMapper(ApplicationVOEntityMapper.class);
    ObjectMapper objectMapper = new ObjectMapper();

    @Mappings({
            @Mapping(
                    target = "username",
                    expression = "java(app.getUser().getUsername())"
            ),
            @Mapping(
                    source = "environment",
                    target = "environment",
                    qualifiedByName = "entityEnvironment2VOEnvironment"),
            @Mapping(
                    source = "appResults",
                    target = "appResultVOMap",
                    ignore = true
            ),
            @Mapping(
                    target = "path",
                    ignore = true
            ),
            @Mapping(
                    source = "args",
                    target = "args",
                    qualifiedByName = "appArgs2AppVOArgs")
    })
    ApplicationVO application2ApplicationVO(Application app);

    @Mappings({
            @Mapping(
                    source = "environment",
                    target = "environment",
                    qualifiedByName = "VOEnvironment2EntityEnvironment"),
            @Mapping(
                    target = "user",
                    expression = "java(userDAO.findByUsername(appVO.getUsername()))"),
            @Mapping(target ="jobStatus",ignore = true),
            @Mapping(
                    source = "args",
                    target = "args",
                    qualifiedByName = "appVOArgs2AppArgs")
    })
    Application applicationVO2Application(ApplicationVO appVO, @Context UserDAO userDAO);

    @Named("entityEnvironment2VOEnvironment")
    default Map<String, Map<String, Object>> entityEnvironment2VOEnvironment(String environment) {
        if(environment != null){
            Map<String, Map<String, Object>> map = new HashMap<>();
            try {
                map = objectMapper.readValue(environment, new TypeReference<Map<String, Map<String, Object>>>() {});
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return map;
        }
        return null;
    }

    @Named("VOEnvironment2EntityEnvironment")
    default String VOEnvironment2EntityEnvironment(Map<String, Map<String, Object>> environment) {
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
     * appArgs->appVOArgs
     *
     * @param var
     * @return
     */
    @Named("appArgs2AppVOArgs")
    default Map<String, Object> appArgs2AppVOArgs(String var) {
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
     * appVOArgs->appArgs
     *
     * @param var
     * @return
     */
    @Named("appVOArgs2AppArgs")
    default String appVOArgs2AppArgs(Map<String, Object> var) {
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
     * Application->ApplicationVO,完成时，补全属性
     * @param app
     * @param appVO
     * @param appDAO
     */
    @AfterMapping
    default void completeApplication(@MappingTarget Application app, ApplicationVO appVO, @Context ApplicationDAO appDAO){
        if(appVO.getId() != null){
            Application app1 = appDAO.findById(appVO.getId()).orElseThrow(() -> new EntityNotFoundException(String.valueOf(appVO.getId())));
            app.setGraph(app1.getGraph());
            app.setJobStatus(app1.getJobStatus());
            app.setCreateTime(app1.getCreateTime());
            app.setUpdateTime(app1.getUpdateTime());
        }
        if(appVO.getJobStatus() != null)
            app.setJobStatus(JobStatus.valueOf(appVO.getJobStatus()));
    }

    /**
     * ApplicationVO->Application,完成时补全属性
     * @param app
     * @param appVO
     */
    @AfterMapping
    default void completeApplicationVO(Application app, @MappingTarget ApplicationVO appVO){
        String graphName = app.getGraph().getName();
        appVO.setGraphName(graphName);
        // （相对路径）{username}/app_result/{appid}  这个path前端用到过吗？
        String path = app.getUser().getUsername()+"/app_result/" + app.getId().toString();
        appVO.setPath(path);
        // ApplicationResult-->ApplicationResultVO
        List<ApplicationResult> appResults = app.getAppResults();
        Map<String,List<ApplicationResultVO> > appResultVOMap = new HashMap<>();
        for(ApplicationResult appResult: appResults){
            String inputFileName = appResult.getInputFile();
            if(appResultVOMap.containsKey(inputFileName)){
                List<ApplicationResultVO> applicationVOS = appResultVOMap.get(inputFileName);
                applicationVOS.add(ApplicationResultVOEntityMapper.MAPPER.appResult2AppResultVO(appResult));
            }
            else {
                List<ApplicationResultVO> applicationVOS = new ArrayList<>();
                applicationVOS.add(ApplicationResultVOEntityMapper.MAPPER.appResult2AppResultVO(appResult));
                appResultVOMap.put(inputFileName,applicationVOS);
            }
        }
        appVO.setAppResultVOMap(appResultVOMap);
        appVO.setGraphId(app.getGraph().getId());
    }
}
