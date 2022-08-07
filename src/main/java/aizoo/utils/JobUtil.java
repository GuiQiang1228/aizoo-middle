package aizoo.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class JobUtil {

    private static ObjectMapper objectMapper = new ObjectMapper();

    // slurm service返回的map key，与数据库里的environment map key的对应关系
    // 本map的key是数据库中接受的key，value是slurm service返回的key
    // 本map只用于返回的数据type不是“showJob”时，一般为请求的jobkey非法，需要查slurm_acct_db时
    // 具体的对应关系，请参考数据库的experiment_job的environment字段和slurm_acct_db数据库的cluster_job_table表
    private static Map<String, String> keyInEnv2KeyInSlurmTable = new HashMap<String, String>() {{
        put("job_state", "job_state");
        put("eligible_time", "time_eligible");
        put("start_time", "time_start");
        put("end_time", "time_end");
        put("suspend_time", "time_suspended");
        put("tres_alloc_str", "tres_alloc_str");
        put("run_time", "run_time");
        put("run_time_str", "run_time_str");
    }};

    // 有些特殊的job状态，slurm service的返回值，与数据库里可接受的job状态的返回值对应关系
    // 本map的key是slurm service返回的job状态，value是数据库里存的job状态,所有的状态列表，请参考JobStatus.java
    private static Map<String, String> jobStateMapping = new HashMap<String, String>() {{
        put("NODE FAILURE", "NODE_FAILURE");
        put("SPECIAL EXIT STATE", "SPECIAL_EXIT_STATE");
    }};

    /**
     * 根据slurm service查到的jobInfo，更新数据库里的原始的environment map
     * newJobInfoStr和environmentStr2Update都是只有一个key的map，他们的key值都是jobKey
     * 用到的数据样例参考 docs/DataStructure.md
     *
     * @param jobKey                格式为 {slurm中jobkey}-{本用户对应的集群ip}
     * @param newJobInfoStr         slurm service查到的job数据，newJobInfoStr[jobKey]会被更新到 environment2Update[jobKey]，
     * @param environmentStr2Update 数据库里的原始的environment map的json str
     * @return Map<String, String>类型，格式为：{"environment":"jobKey对应的新的environment的json str","jobStatus":"jobKey对应的新状态"}
     * @throws JsonProcessingException
     */
    public static Map<String, String> updateJobStatusAndEnv(String jobKey, String newJobInfoStr, String environmentStr2Update) throws JsonProcessingException {

        // 1. 将json str解析成map，并根据jobKey取出对应的jobinfo
        // 初始environment map，来自数据库的environment字段，是只有一个key的map，唯一key就是jobKey，待更新
        Map<String, Map<String, Object>> jobKey2JobInfoInEnv = objectMapper.readValue(environmentStr2Update, new TypeReference<Map<String, Map<String, Object>>>() {
        });
        Map<String, Object> jobInfoInEnv = jobKey2JobInfoInEnv.get(jobKey);

        // slurm service查询到的job info，是只有一个key的map，唯一key就是jobKey，需逐一更新到jobKey2JobInfoInEnv中
        Map<String, Map<String, Object>> jobKey2JobInfoFromSlurm = objectMapper.readValue(newJobInfoStr, new TypeReference<Map<String, Map<String, Object>>>() {
        });
        Map<String, Object> jobInfoFromSlurm = jobKey2JobInfoFromSlurm.get(jobKey);


        // 2. 根据slurm service返回的type不同的值，分别组织jobinfo，type=showJob时直接替换原始数据，否则按照不同的key值的对应关系，逐一完成替换
        if (!jobInfoFromSlurm.get("type").equals("showJob")) {
            // 如果这个信息不是来自 slurm service的showJob方法，即为请求的jobkey非法，需要查slurm_acct_db时
            for (String keyInEnv : keyInEnv2KeyInSlurmTable.keySet()) {
                String keyInSlurmTable = keyInEnv2KeyInSlurmTable.get(keyInEnv);
                if (jobInfoFromSlurm.containsKey(keyInSlurmTable)) {
                    Object value = jobInfoFromSlurm.get(keyInSlurmTable);
                    jobInfoInEnv.put(keyInEnv, value);
                }
            }
            jobKey2JobInfoInEnv.put(jobKey, jobInfoInEnv);
        } else {
            // 来自 slurm service的showJob方法则可直接替换全部信息
            jobKey2JobInfoInEnv.put(jobKey, jobInfoFromSlurm);
        }


        // 3. 更新job status，如果与middle端支持的状态不符，则替换成middle端支持的状态
        String jobState = jobInfoFromSlurm.get("job_state").toString();
        if (jobStateMapping.containsKey(jobState))
            jobState = jobStateMapping.get(jobState);

        // 4. 组织返回结果
        String finalEnvStr = objectMapper.writeValueAsString(jobKey2JobInfoInEnv);
        Map<String, String> result = new HashMap<>();
        result.put("environment", finalEnvStr);
        result.put("jobStatus", jobState);

        return result;
    }
}
