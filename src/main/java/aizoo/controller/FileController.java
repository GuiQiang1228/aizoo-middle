package aizoo.controller;

import aizoo.annotation.MultiRequestBody;
import aizoo.aspect.ResourceCheck;
import aizoo.common.ResourceType;
import aizoo.domain.*;
import aizoo.repository.*;
import aizoo.response.BaseResponse;
import aizoo.response.ResponseCode;
import aizoo.response.ResponseResult;
import aizoo.service.*;
import aizoo.utils.FileUtil;
import aizoo.viewObject.mapper.ComponentVOEntityMapper;
import aizoo.viewObject.object.*;
import aizoo.viewObject.pagebleUtils.VO2EntityMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import aizoo.aspect.WebLog;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;

@BaseResponse
@RestController
public class FileController {

    @Autowired
    private FileService fileService;

    @Autowired
    private ComponentService componentService;

    @Autowired
    private ComponentDAO componentDAO;

    @Autowired
    private DatasourceService datasourceService;

    @Autowired
    private ResourceUsageService resourceUsageService;

    @Autowired
    private DatasourceDAO datasourceDAO;

    @Autowired
    private CodeDAO codeDAO;

    @Autowired
    private MirrorJobDAO mirrorJobDAO;

    @Autowired
    private ExperimentJobDAO experimentJobDAO;

    @Value("${file.path}")
    String file_path;

    @Value("${download.url}")
    String downloadUrl;

    @Value("${download.dir}")
    String downloadDir;


    private final static Logger logger = LoggerFactory.getLogger(FileController.class);

    /**
     * 文件切片上传，将切片文件上传至用户临时目录
     *
     * @param chunk     文件切片
     * @param principal 规则
     * @return 无返回值
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/resource/upload/chunk", method = RequestMethod.POST)
    @WebLog(description = "文件切片上传")
    public void uploadChunk(TChunkInfoVO chunk, Principal principal) {
        MultipartFile file = chunk.getChunkFile();
        logger.info("file originName: {}, chunkNumber: {}", file.getOriginalFilename(), chunk.getChunkNumber());
        try {
            byte[] bytes = file.getBytes();
//            切片文件上传至用户临时目录
            Path path = Paths.get(FileUtil.generatePath(file_path, principal.getName(), chunk));
            //文件写入指定路径
            Files.write(path, bytes);
        } catch (IOException e) {
            logger.error("uploadChunk Failed！");
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * 文件切片上传前得检查
     * 检查是否已存在相同得文件名
     *
     * @param namespace 命名空间
     * @param fileName  文件名
     * @return 返回检查结果信息
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/resource/upload/fileCheck", method = RequestMethod.GET)
    @WebLog(description = "文件切片上传前的检查")
    @ResourceCheck(resourceTypes = {ResourceType.DISK})
    public ResponseResult uploadFileCheck(String namespace, String fileName) {
        String result = fileService.fileCheck(namespace, fileName);
        if (!result.equals("SUCCESS")) {
            return new ResponseResult(ResponseCode.CHUNK_CHECK_ERROR.getCode(), "已存在相同的文件名：" + result + ",请改名后重试", null);
        }
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
    }


    /**
     * 文件切片上传前的检查
     * 检查用户剩余硬盘容量是否足够支持本次上传
     *
     * @param size 待上传文件大小
     * @return 返回检查结果信息
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/resource/upload/fileDiskCheck", method = RequestMethod.GET)
    @WebLog(description = "文件上传前的空间检查")
    @ResourceCheck(resourceTypes = {ResourceType.DISK})
    public ResponseResult uploadFileDiskCheck(double size, Principal principal) {
        Boolean result = fileService.fileDiskCheck(size, principal.getName());
        if (!result) {
            return new ResponseResult(ResponseCode.DISK_CHECK_ERROR.getCode(), ResponseCode.DISK_CHECK_ERROR.getMsg(), null);
        }
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
    }

    /**
     * 上传数据资源
     *
     * @param datasourceVO 数据资源
     * @param fileInfo     文件输入流
     * @return 上传数据资源信息
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/resource/upload/mergeDatasource", method = RequestMethod.POST)
    @WebLog(description = "上传数据资源")
    public ResponseResult uploadDatasource(@MultiRequestBody("datasource") DatasourceVO datasourceVO,
                                           @MultiRequestBody("fileInfo") TFileInfoVO fileInfo) {
        try {
            datasourceService.uploadDatasource(fileInfo, datasourceVO);
        } catch (Exception e) {
            logger.error("uploadDatasource Failed!");
            logger.error(e.getMessage(), e);
            return new ResponseResult(ResponseCode.DATASOURCE_UPLOAD_ERROR.getCode(),
                    ResponseCode.DATASOURCE_UPLOAD_ERROR.getMsg() + ":" + e.getMessage(), null);
        }
        return new ResponseResult(ResponseCode.SUCCESS.getCode(),
                ResponseCode.SUCCESS.getMsg(), null);
    }

    /**
     * 上传组件
     *
     * @param componentVO 组件
     * @param fileInfo    文件输入流
     * @return 上传成功或者失败信息
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/resource/upload/mergeComponent", method = RequestMethod.POST)
    @WebLog(description = "上传组件")
    public ResponseResult uploadComponent(@MultiRequestBody("component") ComponentVO componentVO,
                                          @MultiRequestBody("fileInfo") TFileInfoVO fileInfo) {
        try {
            componentService.uploadComponent(fileInfo, componentVO);
        } catch (Exception e) {
            logger.error("uploadComponent Failed！");
            logger.error(e.getMessage(), e);
            return new ResponseResult(ResponseCode.COMPONENT_UPLOAD_ERROR.getCode(),
                    ResponseCode.COMPONENT_UPLOAD_ERROR.getMsg() + ":" + e.getMessage(), null);
        }
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
    }

    /**
     * 解析用户上传组件前得资源配置文件
     *
     * @param file 资源配置文件
     * @return 解析文件信息
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/resource/upload/parseJson", method = RequestMethod.POST)
    @WebLog(description = "解析用户上传组件前的资源配置文件")
    public ResponseResult parseJsonFile(MultipartFile file) {
        try {
            ComponentVO componentVO = FileUtil.parseJsonFile(file);
            return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), componentVO);
        } catch (Exception e) {
            logger.error("parseJsonFile Failed！");
            logger.error(e.getMessage(), e);
            return new ResponseResult(ResponseCode.JSON_FILE_PARSE_ERROR.getCode(),
                    ResponseCode.JSON_FILE_PARSE_ERROR.getMsg(), null);
        }
    }

    /**
     * 获取所有用户拥有的组件文件
     *
     * @param pageNum  页号
     * @param pageSize 页面大小
     * @return 用户所有拥有的组件文件
     */
    //管理员使用的
    @RequestMapping(value = "/admin/resource/component", method = RequestMethod.GET)
    @WebLog(description = "获取所有用户拥有的组件文件")
    public Page<ComponentVO> getComponentUploaded(@RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                                  @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
//        以数据库中的updateTime为准，同时updateTime必须非空
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.Direction.DESC, "updateTime");
        Page<Component> componentsPage = componentDAO.findByComposed(false, pageable);
        return VO2EntityMapper.mapEntityPage2VOPage(ComponentVOEntityMapper.MAPPER::component2ComponentVO, componentsPage);
    }

    /**
     * 删除资源管理页面用户拥有的组件/数据资源
     *
     * @param type      类型，数据资源或者组件
     * @param id        组件或数据资源id
     * @param principal 规则
     * @return 删除信息
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/resource/delete", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "删除资源管理页面用户拥有的组件/数据资源")
    public ResponseResult deleteData(@MultiRequestBody("type") String type,
                                     @MultiRequestBody("id") long id, Principal principal) throws Exception {
        //删除数据资源
        if (type.equals("datasource")) {
            //根据id查找数据资源
            Datasource datasource = datasourceDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
            //判断用户名是否相等
            if (!datasource.getUser().getUsername().equals(principal.getName())) {
                return new ResponseResult(ResponseCode.DELETE_OUT_OF_BOUNDS.getCode(), ResponseCode.DELETE_OUT_OF_BOUNDS.getMsg(), null);
            }
            //删除数据资源
            datasourceService.deleteDatasource(id);
        }
        //删除组件
        else {
            //根据id查找组件
            Component component = componentDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
            //判断用户名是否相等
            if (!component.getUser().getUsername().equals(principal.getName())) {
                return new ResponseResult(ResponseCode.DELETE_OUT_OF_BOUNDS.getCode(), ResponseCode.DELETE_SHARE_RECORD_FAILED.getMsg(), null);
            }
            //删除组件
            componentService.deleteUploadComponent(id);
        }
        resourceUsageService.updateDiskCapacity(principal.getName());
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
    }

    /**
     * 下载实验结果
     * 若任务下载出现错误，返回错误信息，否则返回下载路径信息
     *
     * @param jobId     实验id
     * @param principal 规则
     * @return 实验结果信息
     */
    @RequestMapping(value = "/api/job/downResult", method = RequestMethod.GET)
    @WebLog(description = "下载实验结果")
    public ResponseResult downloadExperimentResult(@RequestParam long jobId, HttpServletRequest request, Principal principal) throws Exception {
        String filePath = fileService.getExperimentDownResult(jobId, principal.getName());
        if (filePath == null) {
            String message = ":任务下载出现错误，请重试！";
            return new ResponseResult(ResponseCode.JOB_DOWNLOAD_ERROR.getCode(), ResponseCode.JOB_DOWNLOAD_ERROR.getMsg() + message, null);
        }
        String urlPath = filePath.replaceAll(downloadDir, "");
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), downloadUrl + urlPath);
    }

    /**
     * 下载服务结果
     * 若服务下载出现错误，返回错误信息，否则返回下载路径信息
     *
     * @param jobId     实验id
     * @param principal 规则
     * @return 服务结果信息
     */
    @RequestMapping(value = "/api/service/downResult", method = RequestMethod.GET)
    @WebLog(description = "下载服务结果")
    public ResponseResult downloadServiceResult(@RequestParam long jobId, HttpServletRequest request, Principal principal) throws Exception {
        String filePath = fileService.getServiceDownResult(jobId, principal.getName());
        if (filePath == null) {
            String message = ":任务下载出现错误，请重试！";
            return new ResponseResult(ResponseCode.JOB_DOWNLOAD_ERROR.getCode(), ResponseCode.JOB_DOWNLOAD_ERROR.getMsg() + message, null);
        }
        String urlPath = filePath.replaceAll(downloadDir, "");
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), downloadUrl + urlPath);
    }

    /**
     * 下载镜像实验结果
     * 若任务下载出现错误，返回错误信息，否则返回下载路径信息
     *
     * @param jobId     实验id
     * @param principal 规则
     * @return 实验结果信息
     */
    @RequestMapping(value = "/api/mirror/job/downResult", method = RequestMethod.GET)
    @WebLog(description = "下载镜像实验结果")
    public ResponseResult downloadMirrorJobResult(@RequestParam long jobId, HttpServletRequest request, Principal principal) throws Exception {
        String filePath = fileService.getMirrorJobDownResult(jobId, principal.getName());
        if (filePath == null) {
            String message = ":任务下载出现错误，请重试！";
            return new ResponseResult(ResponseCode.JOB_DOWNLOAD_ERROR.getCode(), ResponseCode.JOB_DOWNLOAD_ERROR.getMsg() + message, null);
        }
        String urlPath = filePath.replaceAll(downloadDir, "");
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), downloadUrl + urlPath);
    }

    /**
     * 新建组件
     * 若新建组件出现错误，返回错误信息
     *
     * @param componentVO 新组件
     * @return 新建组件信息
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/resource/buildComponent", method = RequestMethod.POST)
    @WebLog(description = "新建组件")
    public ResponseResult buildComponent(@MultiRequestBody("component") ComponentVO componentVO, HttpServletRequest request) {
        try {
            String path = componentService.buildComponent(componentVO);
            String host = request.getServerName() + ":" + request.getServerPort();
            String url = host + File.separator + path;
            return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), url);
        } catch (Exception e) {
            logger.error("buildComponent Failed！");
            return new ResponseResult(ResponseCode.COMPONENT_BUILD_ERROR.getCode(),
                    ResponseCode.COMPONENT_BUILD_ERROR.getMsg() + ":" + e.getMessage(), null);
        }
    }

    /**
     * 获取组件源码的url
     *
     * @param id 组件id
     * @return 组件源码url
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/resource/modifyComponent", method = RequestMethod.POST)
    @WebLog(description = "修改组件")
    public ResponseResult modifyComponent(@MultiRequestBody long id, HttpServletRequest request) {
        try {
            //根据id获取组件源码的路径
            String path = componentService.modifyComponent(id);
            //获取主机号和端口号
            String host = request.getServerName() + ":" + request.getServerPort();
            //url=主机号：端口号/path
            String url = host + File.separator + path;
            return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), url);
        } catch (EntityNotFoundException e) {
            logger.error("modifyComponent Failed！EntityNotFoundException");
            return new ResponseResult(ResponseCode.FILE_NOT_EXIT_ERROR.getCode(),
                    ResponseCode.FILE_NOT_EXIT_ERROR.getMsg(), null);
        } catch (Exception e) {
            logger.error("modifyComponent Failed！");
            return new ResponseResult(ResponseCode.COMPONENT_MODIFY_ERROR.getCode(),
                    ResponseCode.COMPONENT_MODIFY_ERROR.getMsg() + ":" + e.getMessage(), null);
        }
    }

    /**
     * 下载算子的结构文件和源文件
     *
     * @param id 算子id
     * @return 结构文件和源文件信息
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/resource/download/file", method = RequestMethod.GET)
    @WebLog(description = "下载算子的结构文件和源文件")
    public ResponseResult downloadComponentFiles(@RequestParam Long id) throws Exception {
        String filePath = fileService.downloadAtomicComponentFiles(id);
        String urlPath = filePath.replaceAll(downloadDir, "");
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), downloadUrl + urlPath);
    }

    /**
     * 删除指定文件
     * @param certainFilePath id后的文件所在路径
     * @param fileId 文件所属code或者mirrorjob或者experimentjob的id
     * @param fileType 文件所属类型code或者mirrorjob或者experimentjob
     * @param principal 用户信息
     * @return 正常删除返回200，无权限返回10072
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/resource/deleteCertainFile", method = RequestMethod.POST)
    @WebLog(description = "删除指定文件")
    public ResponseResult deleteCertainFile(@MultiRequestBody(value = "certainFilePath") String certainFilePath,
                                            @MultiRequestBody(value = "fileId") long fileId,
                                            @MultiRequestBody(value = "fileType") String fileType,
                                            Principal principal) throws Exception {
        logger.info("删除指定文件={}",certainFilePath);
        String fileUserName;
        if(fileType.equals("code")){
            Code code = codeDAO.findById(fileId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(fileId)));
            fileUserName = code.getUser().getUsername();
        }else if(fileType.equals("mirrorjob")){
            MirrorJob mirrorJob = mirrorJobDAO.findById(fileId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(fileId)));
            fileUserName = mirrorJob.getUser().getUsername();
        }else{
            ExperimentJob experimentJob = experimentJobDAO.findById(fileId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(fileId)));
            fileUserName = experimentJob.getUser().getUsername();
        }
        //判断该用户是否有权限删除该文件
        logger.info("1.fileUserName={},principalName={}",fileUserName,principal.getName());
        if(!fileUserName.equals(principal.getName())){
            return new ResponseResult(ResponseCode.DELETE_OUT_OF_BOUNDS.getCode(), ResponseCode.DELETE_OUT_OF_BOUNDS.getMsg(), null);
        }else{
            //得到文件的所有绝对路径
            //{filePath}/{username}/mirror/Id/xxxx格式
            String id = fileId+"";
            String absolutFilePath = Paths.get(file_path,principal.getName(),fileType,id,certainFilePath).toString();
            logger.info("2.totalFilePath={}",absolutFilePath);
            FileUtil.deleteFile(new File(absolutFilePath));
            logger.info("删除指定文件成功");
            //正常删除该文件后更新用户磁盘空间
            resourceUsageService.updateDiskCapacity(principal.getName());
            return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
        }
    }

    /**
     * @Description: 处理前端发来的文件信息，组织访问notebook的url，获取文件内容
     * @param type:          文件类型
     * @param id:            文件id
     * @param relativePath:  文件相对路径
     * @param principal:     用户信息
     * @param request:       http请求信息
     * @return: aizoo.response.ResponseResult
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/job/readOnlyByNoteBook", method = RequestMethod.POST)
    @WebLog(description = "利用notebook读取文件内容")
    public ResponseResult readOnlyByNoteBook(@RequestParam String type,
                                             @RequestParam String id,
                                             @RequestParam String relativePath,
                                             Principal principal,
                                             HttpServletRequest request) {
        String path = "";
        String userName = principal.getName();
        try {
            path = fileService.getReadFileUrlPath(type,id,relativePath,userName);
            //获取主机号和端口号
            String host = request.getServerName() + ":" + request.getServerPort();
            //url=主机号：端口号/path
            String url = host + File.separator + path;
            return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), url);
        } catch (EntityNotFoundException e) {
            logger.error("readNoteBook Failed！EntityNotFoundException");
            return new ResponseResult(ResponseCode.FILE_NOT_EXIT_ERROR.getCode(),
                    ResponseCode.FILE_NOT_EXIT_ERROR.getMsg(), null);
        } catch (Exception e) {
            logger.error("readNoteBook Failed！");
            return new ResponseResult(ResponseCode.COMPONENT_MODIFY_ERROR.getCode(),
                    ResponseCode.COMPONENT_MODIFY_ERROR.getMsg() + ":" + e.getMessage(), null);
        }
    }

}