package aizoo.service;

import aizoo.controller.MirrorJobController;
import aizoo.domain.CheckPoint;
import aizoo.domain.ExperimentJob;
import aizoo.domain.Namespace;
import aizoo.repository.CheckPointDAO;
import aizoo.repository.ExperimentJobDAO;
import aizoo.repository.NamespaceDAO;
import aizoo.utils.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.io.File;

@Service("CheckPointService")
public class CheckPointService {
    @Value("${file.path}")
    String file_path;

    @Autowired
    CheckPointDAO checkPointDAO;

    @Autowired
    NamespaceDAO namespaceDAO;

    @Autowired
    ExperimentJobDAO experimentJobDAO;
    private static final Logger logger = LoggerFactory.getLogger(MirrorJobController.class);

    /**
     * 删除CheckPoint资源方法
     *
     * @param id 根据CheckPoint资源ID定位数据库中的，id由前端传参
     * @throws Exception
     */
    @Transactional
    public void deleteCheckPoint(Long id) throws Exception {
        logger.info("Start delete CheckPoint");
        // 根据ID去数据库中查找对应的CheckPoint资源，如果没有找到抛出一个notfound异常
        CheckPoint checkPoint = checkPointDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
        logger.info("findById CheckPointId: {}", id);
        // 获取CheckPoint资源在服务器上的存放位置
        String filePath = checkPoint.getPath();
        logger.info("CheckPoint Path: {}", filePath);

        Namespace namespace = checkPoint.getNamespace();
        checkPoint.setNamespace(null);

        // 解除experimentJob的双向关联
        ExperimentJob experimentJob = checkPoint.getExperimentJob();
        checkPoint.setExperimentJob(null);
        if (experimentJob != null) {
            experimentJob.getCheckPoints().remove(checkPoint);
            experimentJobDAO.save(experimentJob);
        }

        checkPoint.setUser(null);
        // 开始删除CheckPoint
        checkPointDAO.delete(checkPoint);
        //删除服务器上的CheckPoint资源文件
        FileUtil.deleteFile(new File(filePath));


        //获取路径/data/aizoo-slurm/project/aizoo-release/aizoo-back-interpreter/files/userName/checkpoint/experimentJob_id/
        String fileidpath = filePath.substring(0, filePath.lastIndexOf(File.separator));
        logger.info("fileidpath: {}", fileidpath);
        File sourceFile = new File(fileidpath);
        File[] listFiles = sourceFile.listFiles();
        //判断文件夹下是否为空，空则删除namespace以及该文件夹
        if (listFiles == null || listFiles.length == 0) {
            namespaceDAO.delete(namespace);
            FileUtil.deleteFile(sourceFile);
        }
        logger.info("End delete CheckPoint");
    }
}