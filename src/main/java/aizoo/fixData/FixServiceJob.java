//package aizoo.fixData;
//
//import aizoo.domain.ServiceJob;
//import aizoo.domain.SlurmAccount;
//import aizoo.repository.ServiceJobDAO;
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
//public class FixServiceJob {
//    @Autowired
//    private ServiceJobDAO serviceJobDAO;
//
//    private String separator = "-";
//
//    private ObjectMapper objectMapper = new ObjectMapper();
//
//
//    @PostConstruct
//    private void fixEnvAndJobKey() {
//        List<ServiceJob> serviceJobs = serviceJobDAO.findAll();
//        for (ServiceJob serviceJob : serviceJobs) {
//            SlurmAccount slurmAccount = serviceJob.getUser().getSlurmAccount();
//            String env = serviceJob.getEnvironment();
//            String originJobKey = serviceJob.getJobKey();
//            if (originJobKey.split(separator).length > 1)
//                continue;
//            String newJobKey = originJobKey + separator + slurmAccount.getIp();
//            serviceJob.setJobKey(newJobKey);
//
//            try {
//                Map<String, Map<String, Object>> envMap = objectMapper.readValue(env, new TypeReference<Map<String, Map<String, Object>>>() {
//                });
//                Map<String, Object> data = envMap.get(originJobKey);
//                if (data == null)
//                    continue;
//                envMap.put(newJobKey, data);
//                envMap.remove(originJobKey);
//                serviceJob.setEnvironment(objectMapper.writeValueAsString(envMap));
//                serviceJobDAO.save(serviceJob);
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//        }
//    }
//}
