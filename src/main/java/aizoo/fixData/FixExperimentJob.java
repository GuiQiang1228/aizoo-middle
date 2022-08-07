//package aizoo.fixData;
//
//import aizoo.domain.ExperimentJob;
//import aizoo.domain.SlurmAccount;
//import aizoo.repository.ExperimentJobDAO;
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import javax.annotation.PostConstruct;
//import java.util.List;
//import java.util.Map;
//
//@Component
//public class FixExperimentJob {
//    @Autowired
//    private ExperimentJobDAO experimentJobDAO;
//
//    private String separator = "-";
//
//    private ObjectMapper objectMapper = new ObjectMapper();
//
//
//    @PostConstruct
//    private void fixEnvAndJobKey() {
//        List<ExperimentJob> experimentJobs = experimentJobDAO.findAll();
//        for (ExperimentJob experimentJob : experimentJobs) {
//            SlurmAccount slurmAccount = experimentJob.getUser().getSlurmAccount();
//            String env = experimentJob.getEnvironment();
//            String originJobKey = experimentJob.getJobKey();
//            if (originJobKey.split(separator).length > 1)
//                continue;
//            String newJobKey = originJobKey + separator + slurmAccount.getIp();
//            experimentJob.setJobKey(newJobKey);
//
//            try {
//                Map<String, Map<String, Object>> envMap = objectMapper.readValue(env, new TypeReference<Map<String, Map<String, Object>>>() {
//                });
//                Map<String, Object> data = envMap.get(originJobKey);
//                if (data == null)
//                    continue;
//                envMap.put(newJobKey, data);
//                envMap.remove(originJobKey);
//                experimentJob.setEnvironment(objectMapper.writeValueAsString(envMap));
//                experimentJobDAO.save(experimentJob);
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//        }
//    }
//}
