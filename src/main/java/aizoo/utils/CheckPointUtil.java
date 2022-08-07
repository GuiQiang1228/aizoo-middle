package aizoo.utils;

import aizoo.domain.*;
import aizoo.service.DatasourceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@Service("CheckPointUtil")
public class CheckPointUtil {
    /**
     * 将CheckPoint信息复制到指定目录下
     * @param experimentJob 实验任务实体
     * @param toPath 指定的目录
     * @return checkPointList存放checkPoint的name和真实路径
     * @throws IOException
     */

    private static final Logger logger = LoggerFactory.getLogger(DatasourceService.class);

    public Map<String,String> copyCheckPointToDir(ExperimentJob experimentJob, String toPath) throws IOException {
        logger.info("Start copy checkpoint to dir,experimentJobId: {}", experimentJob.getId());
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> map = objectMapper.readValue(experimentJob.getEnvironment(), new TypeReference<Map<String, Object>>() {});
        Map<String, Object> jobEnv = (Map<String, Object>) map.get(experimentJob.getJobKey());
        String stdOut = (String) jobEnv.get("std_out");
        //获取运行代码根目录 xxx/aizoo-back-interpreter/files/out/{uuid}/
        String savePath = stdOut.substring(0, stdOut.indexOf("runtime_log"));
        //源目录 xxx/aizoo-back-interpreter/files/out/{uuid}/ml_model
        File source = new File(savePath + "ml_model");
        //目的目录
        File target = new File(toPath);
        // 判断目录是否存在
        if (!target.exists()) {
            target.mkdir();
        }
        logger.info("sourceDir: {}", source.getAbsolutePath());
        logger.info("targetDir: {}", target.getAbsolutePath());
        //返回checkPointList存放checkPoint的name和真实路径
        return FileUtil.copyCheckPoint(source,target);
    }

    /**
     * 检查所有的checkPoint是否都已保存
     * @param experimentJob 实验任务实体
     * @param toPath 路径
     * @return 若源目录中的文件都存在于目标目录，返回true，否则返回false
     * @throws JsonProcessingException
     */
    public static boolean allCheckPointHasSave(ExperimentJob experimentJob, String toPath) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> map = objectMapper.readValue(experimentJob.getEnvironment(), new TypeReference<Map<String, Object>>() {});
        Map<String, Object> jobEnv = (Map<String, Object>) map.get(experimentJob.getJobKey());
        System.out.println(experimentJob.getJobKey());
        //获取输出信息
        String stdOut = (String) jobEnv.get("std_out");
        System.out.println(stdOut);
        //获取运行代码根目录  xxx/aizoo-back-interpreter/files/out/{uuid}/
        String savePath = stdOut.substring(0, stdOut.indexOf("runtime_log"));
        //源目录 xxx/aizoo-back-interpreter/files/out/{uuid}/ml_model
        File source = new File(savePath + "ml_model");
        //目标路径
        File target = new File(toPath);
        // 若源目录中的文件都存在于目标目录，返回true，否则返回false
        return FileUtil.copyCheckPointCheck(source,target);
    }
}
