//package aizoo.fixData;
//
//import aizoo.domain.CheckPoint;
//import aizoo.domain.ExperimentJob;
//import aizoo.repository.CheckPointDAO;
//import aizoo.repository.ExperimentJobDAO;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//
//import javax.annotation.PostConstruct;
//import java.io.File;
//import java.nio.file.Paths;
//import java.util.ArrayList;
//import java.util.List;
//
//@Component
//public class FixCheckpoint {
//    @Autowired
//    private CheckPointDAO checkPointDAO;
//
//    @Autowired
//    private ExperimentJobDAO experimentJobDAO;
//
//    @Value("${file.path}")
//    String file_path;
//
//    @PostConstruct
//    public void changeCheckPointFileName() throws Exception {
//        // checkpoint删掉之后，系统会自己重新拷
//        List<CheckPoint> checkPoints = checkPointDAO.findAll();
//        for (CheckPoint checkPoint : checkPoints) {
//            ExperimentJob experimentJob = checkPoint.getExperimentJob();
//            String checkPointPath = Paths.get(file_path, experimentJob.getUser().getUsername(), "checkpoint").toString();
//            File checkpointDir = new File(checkPointPath);
//            if (checkpointDir.exists()) {
//                deleteDir(checkpointDir);
//            }
//            experimentJob.setCheckPoints(new ArrayList<>());
//            experimentJobDAO.save(experimentJob);
//        }
//        checkPointDAO.deleteAll(checkPoints);
//    }
//
//    private void deleteDir(File file) {
//        if (!file.isDirectory()) {
//            file.delete();
//            return;
//        }
//        for (File childFile : file.listFiles()) {
//            deleteDir(childFile);
//        }
//    }
//
//}
