package aizoo.utils;

import aizoo.service.JobService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class SlurmUtil {

    private static final Logger logger = LoggerFactory.getLogger(SlurmUtil.class);

    /**
     * 功能：整合Slurm相关参数
     * @param jobName Job名称
     * @param environment 程序运行环境
     * @param paths 包括两个值，其中paths[0]为代码执行路径, paths[1]日志存储路径, path[2]为镜像路径, path[3]为挂载路径
     * @param slurmArgs Job相关的时间信息，map类型，包含连个键值对，两个键分别为"begin"和"time"
     * @return Map类型，由所有传入的参数整合得到完整的Job信息。
     */
    public static String getSlurmArgs(String jobName, String environment, String[] paths, Map slurmArgs, String rootPath, String command) {
        String filePath = paths[0];
        String logPath = paths[1];
        Map<String,Object> map = new HashMap<>();
        String json = "{\n" +
                "\t\"job_name\": \"job_test\",\n" +
                "\t\"slurm_kwargs\": {\n" +
                "\t\t\"partition\": \"debug\"\n" +
                "\t},\n" +
                "\t\"scripts_dir\": \"test_script_dir\",\n" +
                "\t\"log_dir\": \"testlog_dir\",\n" +
                "\t\"environment\": \"python3/3.7.2\",\n" +
                "\t\"file_path\": \"test.py\"\n" +
                "}";
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            map = objectMapper.readValue(json, new TypeReference<Map<String,Object>>() {});
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage(), e);
        }
        if(paths.length>2){
            String mirrorPath = paths[2];
            String mountPath = paths[3];
            map.put("mirror_path", mirrorPath);
            map.put("mount_path", mountPath);
        }
        map.replace("scripts_dir", rootPath);
        map.put("job_name", jobName);
        map.put("log_dir", logPath);
        map.put("environment", environment);
        map.put("file_path", filePath);
        map.put("command", command);

        String begin = (String) slurmArgs.get("begin");
        if(begin.length()>0){
            begin = begin.substring(0,begin.lastIndexOf('.')).replace('T',' ');
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime data = LocalDateTime.parse(begin, dateTimeFormatter);
            // 增加8小时
            data = data.plusHours(8);
            begin = data.format(dateTimeFormatter).replace(' ','T');
            slurmArgs.put("begin", begin);
        }else{
            slurmArgs.remove("begin");
        }
        String time = (String) slurmArgs.get("time");
        if (time != null) {
            time += ":00:00";
        }
        slurmArgs.put("time", time);
        map.put("slurm_kwargs", slurmArgs);
        String runArgs= null;
        try {
            runArgs = objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage(), e);
        }
        return runArgs;
    }
}
