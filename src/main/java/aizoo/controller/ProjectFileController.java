package aizoo.controller;

import aizoo.annotation.MultiRequestBody;
import aizoo.aspect.ResourceCheck;
import aizoo.aspect.WebLog;
import aizoo.common.ResourceType;
import aizoo.domain.ProjectFile;
import aizoo.repository.ProjectFileDAO;
import aizoo.response.BaseResponse;
import aizoo.response.ResponseCode;
import aizoo.response.ResponseResult;
import aizoo.service.ProjectFileService;
import aizoo.viewObject.object.ProjectFileVO;
import aizoo.viewObject.object.TFileInfoVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;

@BaseResponse
@RestController
public class ProjectFileController {

    @Value("${file.path}")
    String file_path;

    @Autowired
    ProjectFileService projectFileService;

    @Autowired
    ProjectFileDAO projectFileDAO;

    @Value("${download.url}")
    String downloadUrl;

    @Value("${download.dir}")
    String downloadDir;

    private final static Logger logger = LoggerFactory.getLogger(ProjectFileController.class);

    /**
     * 通过projectFileService中的fileCheck方法检查文件
     * @param projectId 项目id
     * @param fileName 文件名
     * @param principal 获取用户信息
     * @return 消息体(成功/失败)
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/project/file/upload/check", method = RequestMethod.GET)
    @WebLog(description = "文件切片上传前的检查")
    @ResourceCheck(resourceTypes = {ResourceType.DISK})
    public ResponseResult uploadFileCheck(long projectId, String fileName, Principal principal) {
        // 1、通过projectFileService中的fileCheck方法检查文件名
        // 将fileCheck方法返回的结果组织成String类型的result
        logger.info("通过projectFileService中的fileCheck方法检查文件名");
        String result = projectFileService.fileCheck(projectId, fileName, principal.getName());
        // 2、根据result组织返回的结果
        // 如果result不是"SUCCESS",将fileCheck方法返回的fileName（文件名）组织成message
        logger.info("根据result组织返回的结果");
        if (!result.equals("SUCCESS")) {
            return new ResponseResult(ResponseCode.CHUNK_CHECK_ERROR.getCode(), "已存在相同的文件名：" + result + ",请改名后重试", null);
        }
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
    }

    /**
     * 通过projectFileService中的uploadProjectFile方法上传项目文件
     * @param projectFileVO 项目文件信息
     * @param fileInfo 文件信息
     * @return 消息体(成功/失败)
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/project/file/upload/mergeFile", method = RequestMethod.POST)
    @WebLog(description = "上传项目文件")
    public ResponseResult uploadProjectFile(@MultiRequestBody("projectFile") ProjectFileVO projectFileVO,
                                            @MultiRequestBody("fileInfo") TFileInfoVO fileInfo) {
        // 通过projectFileService中的uploadProjectFile方法进行文件上传
        try {
            projectFileService.uploadProjectFile(fileInfo, projectFileVO);
        } catch (Exception e) {
            // 日志信息中写入上传失败的message
            logger.error("uploadProjectFile fail, error: {}", e);
            return new ResponseResult(ResponseCode.PROJECT_FILE_UPLOAD_ERROR.getCode(),
                    ResponseCode.PROJECT_FILE_UPLOAD_ERROR.getMsg() + ":" + e.getMessage(), null);
        }
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
    }

    /**
     * 用项目文件id,用户名，以及用户的下载请求验证，然后通过projectFileService中的downloadProjectFile方法获得文件的路径进行文件下载
     * @param projectFileId 项目文件id
     * @param response 用户的下载请求
     * @param principal 获取用户信息
     * @return 下载地址
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/project/file/download", method = RequestMethod.GET)
    @WebLog(description = "下载项目文件")
    public ResponseResult downloadProjectFile(long projectFileId, HttpServletResponse response, Principal principal) {
        // 通过项目文件id,用户名，以及用户的下载请求进行项目文件下载
        try {
            // 1、通过projectFileService中的downloadProjectFile方法获得文件的路径
            logger.info("通过projectFileService中的downloadProjectFile方法获得文件的路径");
            String filePath = projectFileService.downloadProjectFile(projectFileId, response, principal.getName());
            String urlPath = filePath.replaceAll(downloadDir, "");
            // 2、判断文件的路径是否存在，存在即返回文件
            logger.info("判断文件的路径是否存在，存在即返回文件");
            if (!filePath.equals("fail")) {
                // 文件存在，返回项目文件
                return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), downloadUrl+urlPath);
            } else
                return new ResponseResult(ResponseCode.DOWNLOAD_OUT_OF_BOUNDS.getCode(), ResponseCode.DOWNLOAD_OUT_OF_BOUNDS.getMsg(), null);
        } catch (Exception e) {
            logger.error("downloadProjectFile fail, error: {}", e);
            return new ResponseResult(ResponseCode.PROJECT_FILE_DOWNLOAD_ERROR.getCode(),
                    ResponseCode.PROJECT_FILE_DOWNLOAD_ERROR.getMsg() + ":" + e.getMessage(), null);
        }
    }

    /**
     * 通过文件的id找到文件，调用projectFileService中的deleteFile方法删除项目文件
     * @param id 文件id
     * @param principal 用户信息
     * @return 消息体(成功/失败)
     * @throws Exception
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/project/file/delete", method = RequestMethod.POST)
    @WebLog(description = "删除项目文件")
    public ResponseResult deleteProjectFile(@MultiRequestBody("id") long id, Principal principal) throws Exception {
        // 1、通过文件的id找到文件
        logger.info("通过文件的id找到文件");
        ProjectFile projectFile = projectFileDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
        // 2、验证文件的用户名与请求删除的用户信息是否相同，相同即删除
        logger.info("验证文件的用户名与请求删除的用户信息是否相同，相同即删除");
        if (!projectFile.getUser().getUsername().equals(principal.getName())) {
            return new ResponseResult(ResponseCode.DELETE_OUT_OF_BOUNDS.getCode(), ResponseCode.DELETE_OUT_OF_BOUNDS.getMsg(), null);
        }
        // 3、调用projectFileService中的deleteFile方法删除文件
        logger.info("调用projectFileService中的deleteFile方法删除文件");
        projectFileService.deleteFile(id);
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
    }
}
