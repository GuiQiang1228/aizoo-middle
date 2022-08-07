//package aizoo.fixData;
//
//import aizoo.domain.Application;
//import aizoo.domain.SlurmAccount;
//import aizoo.repository.ApplicationDAO;
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
//public class FixApplication {
//    @Autowired
//    private ApplicationDAO applicationDAO;
//
//    private String separator = "-";
//
//    private ObjectMapper objectMapper = new ObjectMapper();
//
//
//    @PostConstruct
//    private void fixEnvAndJobKey() {
//        List<Application> applications = applicationDAO.findAll();
//        for (Application application : applications) {
//            SlurmAccount slurmAccount = application.getUser().getSlurmAccount();
//            String env = application.getEnvironment();
//            String originJobKey = application.getJobKey();
//            if (originJobKey.split(separator).length > 1)
//                continue;
//            String newJobKey = originJobKey + separator + slurmAccount.getIp();
//            application.setJobKey(newJobKey);
//
//            try {
//                Map<String, Map<String, Object>> envMap = objectMapper.readValue(env, new TypeReference<Map<String, Map<String, Object>>>() {
//                });
//                Map<String, Object> data = envMap.get(originJobKey);
//                if (data == null)
//                    continue;
//                envMap.put(newJobKey, data);
//                envMap.remove(originJobKey);
//                application.setEnvironment(objectMapper.writeValueAsString(envMap));
//                applicationDAO.save(application);
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//        }
//    }
//}
