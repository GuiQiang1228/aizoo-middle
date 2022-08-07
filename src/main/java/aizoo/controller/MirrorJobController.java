package aizoo.controller;

import aizoo.annotation.MultiRequestBody;
import aizoo.aspect.ResourceCheck;
import aizoo.aspect.WebLog;
import aizoo.common.ResourceType;
import aizoo.common.exception.NoSlurmAccountException;
import aizoo.domain.*;
import aizoo.repository.*;
import aizoo.response.BaseResponse;
import aizoo.response.ResponseCode;
import aizoo.response.ResponseResult;
import aizoo.service.FileService;
import aizoo.service.MirrorJobService;
import aizoo.viewObject.mapper.MirrorJobVOEntityMapper;
import aizoo.viewObject.object.*;
import aizoo.viewObject.pagebleUtils.VO2EntityMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import aizoo.Client;

import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@BaseResponse
@RestController
public class MirrorJobController {
    @Autowired
    MirrorJobDAO mirrorJobDAO;

    @Autowired
    MirrorDAO mirrorDAO;

    @Autowired
    Client client;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MirrorJobService mirrorJobService;

    @Autowired
    ExperimentJobDAO experimentJobDAO;

    @Autowired
    ServiceJobDAO serviceJobDAO;

    @Autowired
    ApplicationDAO applicationDAO;

    @Autowired
    FileService fileService;

    @Autowired
    CodeDAO codeDAO;

    @Value("${file.path}")
    private String filePath;

    @Value("${save.path}")
    private String savePath;

    @Value("${download.url}")
    String downloadUrl;

    @Value("${download.dir}")
    String downloadDir;

    private static final Logger logger = LoggerFactory.getLogger(MirrorJobController.class);

    /**
     * 利用springboot自带分页功能进行分页搜索
     * 参数为搜索需要的信息
     *
     * @param jobName         job名称
     * @param desc            job描述
     * @param jobStatus       job状态
     * @param startUpdateTime 开始更新时间
     * @param endUpdateTime   结束更新时间
     * @param pageNum         当前页号
     * @param pageSize        每页有几条记录
     * @param principal       用户信息
     * @return 返回查询到的转换格式后的Page对象
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/mirror/job/searchPage", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "万能分页mirrorJob搜索")
    public Page<MirrorJobVO> searchPage(@RequestParam(value = "jobName", required = false, defaultValue = "") String jobName,
                                        @RequestParam(value = "desc", required = false, defaultValue = "") String desc,
                                        @RequestParam(value = "jobStatus", required = false, defaultValue = "") String jobStatus,
                                        @RequestParam(value = "startUpdateTime", required = false, defaultValue = "") String startUpdateTime,
                                        @RequestParam(value = "endUpdateTime", required = false, defaultValue = "") String endUpdateTime,
                                        @RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                        @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize, Principal principal) {
        String userName = principal.getName();
        //不带排序的pageable对象
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        //加入了pageable对象的查询语句，通过该pageable对象分析生成一个带分页查询的sql语句，返回存储JPA查询数据库的结果集（jobsPage)
        Page<MirrorJob> jobsPage = mirrorJobDAO.searchMirrorJob(jobName, desc, jobStatus, startUpdateTime, endUpdateTime, userName, pageable);
        //利用jpa中的page.map方法转换jobsPage的内部对象(转换为JobVO)
        //第一个参数利用双冒号::简化方法引用，实际是调用job2JobVO方法
        return VO2EntityMapper.mapEntityPage2VOPage(MirrorJobVOEntityMapper.MAPPER::job2JobVO, jobsPage);
    }


    /**
     * 该方法用于执行镜像实验
     *
     * @param relativePath 前端传来的用户所使用的入口文件的相对地址
     * @param mirrorJobVO  前端传来的镜像实验信息
     * @param codeId       前端传来的用户使用的code的id
     * @param userArgs     前端传来的用户自己填的入参
     * @param principal    用户信息
     * @return 返回包含执行结果的ResponseResult
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/mirror/job/execute", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "执行mirrorJob")
    @ResourceCheck(resourceTypes = {ResourceType.DISK, ResourceType.CPU, ResourceType.GPU, ResourceType.MEMORY,
            ResourceType.EXPERIMENT_RUNNING_NUMBER, ResourceType.EXPERIMENT_TOTAL_NUMBER})
    public ResponseResult mirrorJobExecute(@MultiRequestBody("relativePath") String relativePath,
                                           @MultiRequestBody("codeId") long codeId,
                                           @MultiRequestBody("mirrorJob") MirrorJobVO mirrorJobVO,
                                           @MultiRequestBody(value = "userArgs", required = false) List<List<String>> userArgs, Principal principal,
                                           @MultiRequestBody("mirrorId") long mirrorId,
                                           @MultiRequestBody("initJobId") long initJobId) {
        try {
            //根据code、镜像实验、用户入参、用户信息执行实验
            logger.info("并根据code执行镜像实验");
            mirrorJobService.executeMirrorJob(relativePath, codeId, mirrorJobVO, userArgs, principal.getName(), mirrorId);
            //若job为复用job，删除原来的job
            Optional<MirrorJob> optional = mirrorJobDAO.findById(initJobId);
            if(optional.isPresent())
                mirrorJobService.removeMirrorJobById(initJobId);
        } catch (NoSlurmAccountException e) {
            //查找slurm账户失败
            logger.error("查询项目中gpu服务器失败, 没有Slurm信息。username = {}, 错误：", principal.getName(), e.getMessage());
            return new ResponseResult(ResponseCode.NO_SLURM_ACCOUNT_ERROR.getCode(), ResponseCode.NO_SLURM_ACCOUNT_ERROR.getMsg(), null);
        } catch (Exception e) {
            //运行失败
            logger.error("运行失败, 错误:{}", e.getMessage());
            return new ResponseResult(ResponseCode.RUN_ERROR.getCode(), ResponseCode.RUN_ERROR.getMsg(), null);
        }
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
    }

    /**
     * 从slurm中获取当前job的状态
     * 如果与数据库中的不一致则返回false，并更新数据库
     * 一致则终止任务并查询slurm更新数据库并返回true
     *
     * @param jobId     指定镜像实验的id
     * @param principal 当前用户的信息
     * @return 返回带有处理结果的ResponseResult, 具体信息为布尔类型的变量
     * @throws Exception
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/mirror/job/stop", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "终止指定镜像实验")
    public ResponseResult stopJob(@MultiRequestBody long jobId, Principal principal) throws Exception {
        boolean result = false;
        try {
            //1.根据jobid找到数据库中对应镜像实验的信息
            MirrorJob mirrorJob = mirrorJobDAO.findById(jobId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(jobId)));
            //2.实验对应用户名称与当前用户名是否一致，不一致则无权限终止
            if (!principal.getName().equals(mirrorJob.getUser().getUsername())) {
                return new ResponseResult(ResponseCode.JOBSTOP_OUT_OF_BOUNDS.getCode(), ResponseCode.JOBSTOP_OUT_OF_BOUNDS.getMsg(), result);
            }
            logger.info("stop job successfully, jobId = {}", jobId);
            //一致则终止指定实验
            result = mirrorJobService.slurmStopMirrorJob(jobId);
        } catch (JsonProcessingException e) {
            logger.error("终止实验失败: jobId = {}, 错误: ", jobId, e.getMessage());
            logger.error("End stop Job");
        }
        //3.返回带有处理结果的ResponseResult
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), result);
    }

    /**
     * 删除指定的镜像实验
     *
     * @param jobId     指定镜像实验的id
     * @param principal 当前用户信息
     * @return 返回带有处理结果的ResponseResult
     * @throws Exception
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/mirror/job/delete", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "删除指定镜像实验")
    public ResponseResult removeMirrorJob(@MultiRequestBody long jobId, Principal principal) throws Exception {
        //1.根据job id从数据库中查找指定镜像实验信息
        MirrorJob mirrorJob = mirrorJobDAO.findById(jobId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(jobId)));
        //2.判断当前用户与指定实验的用户是否一致，不一致则无权限删除
        if (!principal.getName().equals(mirrorJob.getUser().getUsername())) {
            return new ResponseResult(ResponseCode.DELETE_OUT_OF_BOUNDS.getCode(), ResponseCode.DELETE_OUT_OF_BOUNDS.getMsg(), null);
        }
        //删除指定镜像实验
        mirrorJobService.removeMirrorJobById(jobId);
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), "删除成功", null);
    }

    /**
     * 指定根目录文件夹，可浏览整个文件夹，及其子文件，显示文件大小、最近修改时间
     *
     * @param type         指定的类型: code/mirrorjob/experimentjob
     * @param id           codeId/mirrorJobId/experimentJobId
     * @param relativePath 相对路径
     * @param principal    用户信息
     *                     mapKey1: isDir: 该文件是否是文件夹
     *                     mapKey2: name: 文件名
     *                     mapKey3: size: 文件大小
     *                     mapKey4: lastModified 最近修改时间
     *                     mapKey5: relativePath: 该文件的相对路径
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/mirror/job/getDirInfo", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "指定根目录及类型，层级浏览整个文件夹")
    public List<Map<String, Object>> getDirInfo(@RequestParam String type,
                                                @RequestParam String id,
                                                @RequestParam String relativePath,
                                                Principal principal) {
        String path = "";
        String userName = principal.getName();
        if (type.equals("code") || type.equals("mirrorjob")) {
            if (relativePath != "") path = Paths.get(filePath, userName, type, id, relativePath).toString();
            else path = Paths.get(filePath, userName, type, id).toString();
            if (type.equals("code")) logger.info("code path: {}", path);
            else logger.info("mirrorJob path: {}", path);
        }
        else if (type.equals("application")) {
            Application application = applicationDAO.findById(Long.parseLong(id)).orElseThrow(() -> new EntityNotFoundException(id));
            path = application.getRootPath();
            if (relativePath != "") path = Paths.get(path, relativePath).toString();
            logger.info("application Path:{}",path);
        }
        else if (type.equals("servicejob")) {
            ServiceJob serviceJob = serviceJobDAO.findById(Long.parseLong(id)).orElseThrow(() -> new EntityNotFoundException(id));
            path = serviceJob.getRootPath();
            if (relativePath != "") path = Paths.get(path, relativePath).toString();
            logger.info("ServicJob Path:{}",path);
        }
        else if (type.equals("experimentjob")) {
            ExperimentJob experimentJob = experimentJobDAO.findById(Long.parseLong(id)).orElseThrow(() -> new EntityNotFoundException(id));
            path = experimentJob.getRootPath();
            if (relativePath != "") path = Paths.get(path, relativePath).toString();
            logger.info("ExperimentJob Path:{}",path);
        }
        return fileService.traverseFiles(path, relativePath);
    }

    /**
     * @param type:         文件的类型
     * @param id:           文件id
     * @param relativePath: 文件的相对路径
     * @param principal:    用户信息
     * @param request:      http请求信息
     * @Description: 处理前端发来的需要编辑的文件信息，在线编辑
     * @return: aizoo.response.ResponseResult
     * @throws:
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/mirror/job/editByNoteBook", method = RequestMethod.POST)
    @WebLog(description = "使用notebook编辑文件")
    public ResponseResult editByNoteBook(@RequestParam String type,
                                         @RequestParam String id,
                                         @RequestParam String relativePath,
                                         Principal principal,
                                         HttpServletRequest request) {
        String path = "";
        String userName = principal.getName();
        if (type.equals("code")) {
            path = Paths.get("notebook", "edit", userName, type, id, relativePath).toString();
            logger.info("Get the path(type==code) to request notebook for editing: {}",path);
        }
        else{
            logger.error("editByNoteBook Failed！PERMISSION_ERROR");
            return new ResponseResult(ResponseCode.PERMISSION_ERROR.getCode(),
                    ResponseCode.PERMISSION_ERROR.getMsg(), null);
        }
        //获取主机号和端口号
        String host = request.getServerName() + ":" + request.getServerPort();
        //url=主机号：端口号/path
        String url = host + File.separator + path;
        logger.info("Get the url for notebook edit: {}",url);
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), url);
    }

    /**
     * @param id           文件的id
     * @param type         文件的类型
     * @param relativePath 文件的相对路径
     * @param principal    用户信息
     * @return 下载信息
     * @throws Exception
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/downloadFiles", method = RequestMethod.GET)
    @WebLog(description = "下载单个文件")
    public ResponseResult downloadCodeFiles(@RequestParam Long id, @RequestParam String type, @RequestParam String relativePath, Principal principal) throws Exception {
        String filePath = fileService.downloadAtomicFiles(id, type, relativePath, principal);
        String urlPath = filePath.replaceAll(downloadDir, "");
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), downloadUrl + urlPath);
    }
}
