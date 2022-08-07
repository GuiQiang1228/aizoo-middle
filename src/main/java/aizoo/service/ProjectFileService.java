package aizoo.service;

import aizoo.domain.ProjectFile;
import aizoo.repository.ProjectDAO;
import aizoo.repository.ProjectFileDAO;
import aizoo.repository.UserDAO;
import aizoo.utils.FileUtil;
import aizoo.viewObject.mapper.ProjectFileVOEntityMapper;
import aizoo.viewObject.object.ProjectFileVO;
import aizoo.viewObject.object.TFileInfoVO;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.nio.file.Paths;

@Service("ProjectFileService")
public class ProjectFileService {

    @Autowired
    ProjectFileDAO projectFileDAO;

    @Autowired
    UserDAO userDAO;

    @Autowired
    ProjectDAO projectDAO;

    @Value("${file.path}")
    String file_path;

    @Value("${download.dir}")
    String downloadDir;

    private final static Logger logger = LoggerFactory.getLogger(ProjectFileService.class);
    /**
     * 通过路径检查是否存在相同文件名，通过FileUtil中的fileExists方法判断文件是否存在
     * @param projectId 项目id
     * @param fileName 文件名
     * @param username 用户名
     * @return filenames(String) / "SUCCESS"
     */
    public String fileCheck(long projectId, String fileName, String username) {
        // 1、将文件名用“，”进行分割,然后组成文件名list
        String[] fileList = fileName.split(",");
        // 2、逐一查看组成的文件名list是否对应
        for (String fileNames : fileList) {
            // 项目文件路径为  默认路径/username/project/项目id/文件名
            String file = Paths.get(file_path, username, "project", String.valueOf(projectId), fileNames).toString();
            // 如果文件路径相同,通过FileUtil中的fileExists方法判断文件是否存在,返回文件的路径path
            // 如果文件存在，返回已存在的文件名
            if (FileUtil.fileExists(file)) {
                return fileNames;
            }
        }
        // 如果文件不存在，则返回“SUCCESS”
        return "SUCCESS";
    }

    /**
     * 通过ProjectFileVOEntityMapper组织projectFile的数据格式，以及由username和projectId得到目标存放路径，进行文件上传
     * @param fileInfo 文件信息
     * @param projectFileVO 项目文件信息
     * @throws Exception
     */
    @Transactional
    public void uploadProjectFile(TFileInfoVO fileInfo, ProjectFileVO projectFileVO) throws Exception {
        // 日志信息中写入"Start upload ProjectFile"
        logger.info("Start upload ProjectFile");
        // 1、通过ProjectFileVOEntityMapper组织projectFile的数据格式，以及由username和projectId得到目标存放路径
        ProjectFile projectFile = ProjectFileVOEntityMapper.MAPPER.projectFileVO2projectFile(projectFileVO,
                projectFileDAO, userDAO, projectDAO);
        // 获取文件名
        String filename = fileInfo.getName();
        logger.info("fileInfo: {}", fileInfo.toString());
        logger.info("projectFileVO: {}", projectFileVO.toString());
        // 该项目文件临时文件夹下的具体路径
        String file = Paths.get(file_path, projectFileVO.getUsername(), "temp", fileInfo.getUniqueIdentifier(), filename).toString();
        String tempFolder = Paths.get(file_path, projectFileVO.getUsername(), "temp", fileInfo.getUniqueIdentifier()).toString();
        //targetpath是 {file.path}/username/project/projectid，其中file.path在application-main.properties文件中声明，此处由@Value注解注入为file_path
        String targetPath = Paths.get(file_path, projectFileVO.getUsername(), "project", String.valueOf(projectFileVO.getProjectId())).toString();
        projectFile.setPath(Paths.get(targetPath, filename).toString().replace("\\", "/"));
        // 2、进行文件上传
        try {
        //  进行在临时目录下切片文件的合并操作
            FileUtil.merge(file, tempFolder, filename);
        //  将项目文件从临时目录复制到目标存放路径下
            FileUtil.copyFile(file, targetPath);
            projectFileDAO.save(projectFile);
        } catch (Exception e) {
            // 删除目录中本次上传的文件
            File f = new File(file);
            FileUtil.deleteFile(f);
            logger.error("上传文件失败，删除目录中本次上传的文件");
            logger.error("上传文件失败,错误信息: ", e);
            throw e;
        } finally {
            // 将临时目录下的文件和文件夹删除
            String tempPath = Paths.get(file_path, projectFileVO.getUsername(), "temp", fileInfo.getUniqueIdentifier()).toString();
            FileUtils.deleteDirectory(new File(tempPath));
        }
        // 日志信息中写入"End upload ProjectFile"
        logger.info("End upload ProjectFile");

    }

    /**
     * 通过文件的id找到文件，下载项目文件
     * @param id 文件id
     * @param response 用户的下载请求
     * @param username 用户名
     * @return 下载地址
     * @throws Exception
     */
//    @Transactional(rollbackFor = Exception.class)
    public String downloadProjectFile(long id, HttpServletResponse response, String username) throws Exception {
        // 1、通过文件的id找到文件
        logger.info("通过文件的id找到文件");
        ProjectFile projectFile = projectFileDAO.findById(id).orElseThrow(() -> new EntityNotFoundException());
        // 判断文件的用户名是否与请求的用户名相同，不相同即下载失败
        if (!projectFile.getUser().getUsername().equals(username))
            return "fail";
        // 2、组织文件的目标路径，拷贝文件里的所有信息
        logger.info("组织文件的目标路径，拷贝文件里的所有信息");
        File targetPath = new File(Paths.get(downloadDir, username, "project", String.valueOf(projectFile.getProject().getId())).toString());
        try {
            // 如果文件目标路径不存在，创建文件夹
            if (!targetPath.exists()) {
                targetPath.mkdirs();
            }
            File file = new File(projectFile.getPath());
            // 拷贝文件里的所有信息
            FileUtils.copyFileToDirectory(file, targetPath);
            File targetFile = new File(targetPath.getAbsolutePath() + File.separator + file.getName());
            return targetFile.getAbsolutePath();
        } catch (Exception e) {
            // 下载文件失败
            logger.error("downloadProjectFile failed! error=", e);
            try {
                FileUtil.deleteFile(targetPath);
            } catch (Exception exception) {
                logger.error("downloadProjectFile failed! And target file delete failed! error=", e);
            }
            return null;
        }
    }

    /**
     * 通过id找到文件，删除项目文件
     * @param id 文件id
     * @throws Exception
     */
    @Transactional
    public void deleteFile(Long id) throws Exception {
        // 日志信息中写入"Start delete projectFile"
        logger.info("Start delete projectFile");
        // 1、通过文件的id找到文件
        logger.info("通过文件的id找到文件");
        ProjectFile projectFile = projectFileDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
        logger.info("findById id: {}", id);
        // 2、获得文件的路径
        logger.info("获得文件的路径");
        String filePath = projectFile.getPath();
        projectFile.getProject().getProjectFiles().remove(projectFile);
        projectFile.setProject(null);
        projectFile.setUser(null);
        // 输出的datasource置空，删除文件信息
        projectFileDAO.delete(projectFile);
        FileUtil.deleteFile(new File(filePath));
        logger.info("End delete projectFile");
    }
}
