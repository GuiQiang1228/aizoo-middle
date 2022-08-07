package aizoo.service;

import aizoo.domain.Mirror;
import aizoo.domain.User;
import aizoo.repository.*;
import aizoo.utils.FileUtil;
import aizoo.viewObject.mapper.MirrorVOEntityMapper;
import aizoo.viewObject.object.MirrorVO;
import aizoo.viewObject.object.TFileInfoVO;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Paths;

@Service("MirrorService")
public class MirrorService {
    @Autowired
    SlurmService slurmService;

    @Autowired
    MirrorJobDAO mirrorJobDAO;

    @Value("${file.path}")
    private String filePath;

    @Autowired
    UserDAO userDAO;

    @Autowired
    CodeDAO codeDAO;

    @Autowired
    MirrorDAO mirrorDAO;

    @Autowired
    TranslationService translationService;

    @Autowired
    ResourceUsageService resourceUsageService;

    @Autowired
    ResourceUsageDAO resourceUsageDAO;

    @Autowired
    JobService jobService;

    private static final Logger logger = LoggerFactory.getLogger(MirrorService.class);

    /**
     * 检查该mirror是否已经注册
     * @param mirrorName 待检查的镜像名
     * @param userName 用户名
     * @return 若已经注册返回false，若未注册返回true
     */
    public boolean checkMirrorName(String mirrorName, String userName) {
        logger.info("check mirrroName: {}, userName: {}", mirrorName, userName);
        Mirror mirror = mirrorDAO.findByUserUsernameAndName(userName, mirrorName);
        if (mirror == null) {
            logger.info("【该镜像名称不存在，可以注册】");
            return true;
        }
        logger.error("【该镜像名称已存在，不可以注册】");
        return false;
    }

    /**
     * 镜像上传 镜像可选择所有公共的或自己的
     * @param mirrorVO 镜像
     * @param fileInfo 文件输入流
     * @return 上传成功或者失败信息
     */
    @Transactional
    public void uploadMirror(TFileInfoVO fileInfo, MirrorVO mirrorVO) throws Exception {
        logger.info("Start upload Mirror");
        // 将mirror由VO转换为entity
        Mirror mirror = MirrorVOEntityMapper.MAPPER.mirrorVO2Mirror(mirrorVO, userDAO);
        logger.info("fileInfo: {}", fileInfo.toString());
        // 得到附件名称
        String filename = fileInfo.getName();
        // 获得用户名 设置user
        String userName = mirrorVO.getUserName();
        User user = userDAO.findByUsername(userName);
        mirror.setUser(user);
        mirrorDAO.save(mirror);
        if (mirrorVO.getPrivacy().equals("public")) {
            userName = "";
        }
        // tempFile是具体到temp文件夹里的具体文件, 具体路径: {file.path}/[username]/temp/{identifier}/{filename}
        String tempFile = Paths.get(filePath, userName, "temp", fileInfo.getUniqueIdentifier(), filename).toString();
        // tempFolder是temp文件夹的路径, 具体路径: {file.path}/[username]/temp/{identifier}
        String tempFolder = Paths.get(filePath, userName, "temp", fileInfo.getUniqueIdentifier()).toString();
        // 设置目标存放路径, targetPath: {file.path}/[username]/mirror/{mirror.id}/{filename}
        String targetPath = Paths.get(filePath, userName, "mirrors", mirror.getId().toString(), filename).toString();
        // 目标存放文件夹
        String targetFolderPath = Paths.get(filePath, userName, "mirrors", mirror.getId().toString()).toString();
        logger.info("Path of tempFile: {}, tempFolder: {}, targetPath: {}, targetFolderPath: {}", tempFile, tempFolder, targetPath, targetFolderPath);

        try {
            logger.info("Start merge file,tempFolder: {}", tempFolder);
            //进行在临时目录下切片文件的合并操作, tempFile为合并后的目标文件路径, tempFolder为文件夹路径, filename为不参与合并的文件名
            FileUtil.merge(tempFile, tempFolder, filename);
            logger.info("Start copy file,targetFolder: {}", targetFolderPath);
            // 将mirror从临时目录复制到目标存放路径下
            FileUtil.copyFile(tempFile, targetFolderPath);
            mirror.setPath(targetPath);
            mirrorDAO.save(mirror);
        }
        catch (Exception e) {
            //删除目录中本次上传的文件
            File f = new File(tempFile);
            FileUtil.deleteFile(f);
            //删除数据库中的信息
            mirrorDAO.delete(mirror);
            logger.error("上传镜像失败, 删除目录中本次上传的镜像");
            logger.error("上传镜像失败: {}", e.getMessage());
        } finally {
            logger.info("Start delete temp folder and file");
            //将临时目录下的文件和文件夹删除
            FileUtils.deleteDirectory(new File(tempFolder));
        }
        logger.info("End upload mirror");
    }
}
