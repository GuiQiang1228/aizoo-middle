package aizoo.grpcObject.mapper;

import aizoo.domain.Application;
import aizoo.domain.ExperimentJob;
import aizoo.domain.MirrorJob;
import aizoo.grpcObject.object.JobObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.Map;

/**
 * Application 或 ExperimentJob 转成 JobObject
 */
@Mapper(componentModel = "spring")
public interface JobObjectEntityMapper {
    JobObjectEntityMapper MAPPER = Mappers.getMapper(JobObjectEntityMapper.class);
    ObjectMapper objectMapper = new ObjectMapper();

    // 对应completeJobObject1方法，即将experimentJob转为JobObject
    @Mappings({
            @Mapping(
                    target = "environment",
                    ignore = true),
    })
    JobObject experimentJob2JobObject(ExperimentJob experimentJob);

    //  对应completeJobObject2方法，即将application转为JobObject
    @Mappings({
            @Mapping(
                    target = "environment",
                    ignore = true),
    })
    JobObject appplication2JobObject(Application app);

    //  对应completeJobObject3方法，即将mirrorJob转为JobObject
    @Mappings({
            @Mapping(
                    target = "environment",
                    ignore = true),
    })
    JobObject mirrorJob2JobObject(MirrorJob mirrorJob);

    /**
     * 将experimentJob转换成jobObject
     *
     * @param experimentJob  source job
     * @param jobObject  target job
     */
    @AfterMapping
    default void completeJobObject1(ExperimentJob experimentJob, @MappingTarget JobObject jobObject) {
        try {
            if (experimentJob.getEnvironment() != null) {
                //将environment读取为map格式并保存进jobObject,environment的具体格式可在数据库中的experiment_job表的environment列中看到
                // { 最外层的Sting是这个job的ID，内层的是key-value模式的键值对
                //   "experimentJobID"：{
                //      account": "test",
                //		"accrue_time": "2022-01-09T22:23:15",
                //		"batch_host": "gpu03",
                //      .......
                //   }
                // }

                Map<String, Map<String, Object>> environmentObject = objectMapper.readValue(experimentJob.getEnvironment(), new TypeReference<Map<String, Map<String, Object>>>() {
                });
                jobObject.setEnvironment(environmentObject);
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    /**
     * 将application转换成jobObject
     * @param app       source application
     * @param jobObject target job
     */
    @AfterMapping
    default void completeJobObject2(Application app, @MappingTarget JobObject jobObject){
        try {
            //将environment读取为map格式并保存进jobObject,environment的具体格式可在数据库中的app_result表的environment列中看到
            // 具体结构形式类似experiment_job里
            if (app.getEnvironment() != null) {
                Map<String, Map<String, Object>> environmentObject = objectMapper.readValue(app.getEnvironment(), new TypeReference<Map<String, Map<String, Object>>>() {
                });
                jobObject.setEnvironment(environmentObject);
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    /**
     * 将mirrorJob转换成jobObject
     * @param mirrorJob       source mirrorJob
     * @param jobObject target job
     */
    @AfterMapping
    default void completeJobObject3(MirrorJob mirrorJob, @MappingTarget JobObject jobObject){
        try {
            //将environment读取为map格式并保存进jobObject,environment的具体格式可在数据库中的app_result表的environment列中看到
            // 具体结构形式类似experiment_job里
            if (mirrorJob.getEnvironment() != null) {
                Map<String, Map<String, Object>> environmentObject = objectMapper.readValue(mirrorJob.getEnvironment(), new TypeReference<Map<String, Map<String, Object>>>() {
                });
                jobObject.setEnvironment(environmentObject);
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}

