package aizoo.service;

import aizoo.domain.ExperimentJob;
import aizoo.domain.Graph;
import aizoo.repository.*;
import aizoo.utils.DAOUtil;
import aizoo.viewObject.mapper.ExperimentJobVOEntityMapper;
import aizoo.viewObject.mapper.GraphVOEntityMapper;
import aizoo.viewObject.object.GraphVO;
import aizoo.viewObject.object.ExperimentJobVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class DockerService {
    @Autowired
    private GraphDAO graphDAO;

    @Autowired
    ExperimentJobDAO experimentJobDAO;

    @Autowired
    private ComponentDAO componentDAO;

    @Autowired
    TranslationService translationService;

    @Autowired
    DAOUtil daoUtil;

    @Value("${file.path}")
    String filePath;

//    public void downDockerPackage(GraphVO graphVO, ExperimentJobVO experimentJobVO, String username) throws Exception {
//        Graph graph = GraphVOEntityMapper.MAPPER.graphVO2Graph(graphVO, graphDAO,componentDAO, daoUtil);
//        ExperimentJob experimentJob = ExperimentJobVOEntityMapper.MAPPER.jobVO2Job(experimentJobVO, experimentJobDAO);
//        String[] paths = translationService.translateJob(graph, experimentJob, username);
//        String fileDirPath = paths[1];
//        System.out.println(paths[1]);
//        if(paths[1].endsWith("runtime_log"))
//            fileDirPath = paths[1].substring(0,paths[1].lastIndexOf(File.separator));
//        // 截取打包的docker镜像存放的目录名
//        String packageOutputDirName = fileDirPath.substring(fileDirPath.lastIndexOf(File.separator)+1);
//        // dockerPackage：用户存放docker镜像包的目录
//        Path dockerPackageDirPath = Paths.get(filePath, username, "docker_packages");
//        File dockerPackage = dockerPackageDirPath.toFile();
//        if(! dockerPackage.exists())
//            dockerPackage.mkdirs();
//
//    }
}
