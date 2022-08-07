package aizoo.controller;

import aizoo.Client;
import aizoo.annotation.MultiRequestBody;
import aizoo.aspect.WebLog;
import aizoo.domain.Mirror;
import aizoo.repository.MirrorDAO;
import aizoo.response.BaseResponse;
import aizoo.response.ResponseCode;
import aizoo.response.ResponseResult;
import aizoo.service.MirrorService;
import aizoo.utils.FileUtil;
import aizoo.viewObject.mapper.MirrorVOEntityMapper;
import aizoo.viewObject.object.MirrorVO;
import aizoo.viewObject.object.TChunkInfoVO;
import aizoo.viewObject.object.TFileInfoVO;
import aizoo.viewObject.pagebleUtils.VO2EntityMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.List;

@BaseResponse
@RestController
public class MirrorController {

    @Autowired
    MirrorDAO mirrorDAO;

    @Autowired
    Client client;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MirrorService mirrorService;

    @Value("${file.path}")
    private String filePath;

    private static final Logger logger = LoggerFactory.getLogger(MirrorController.class);

    /**
     获取所有Mirror
     @return List<MirrorVO>
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/mirror/getMirror", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "获取所有mirror")
    public List<MirrorVO> getMirror() {
        List<Mirror> mirrorList = mirrorDAO.findAll();
        return VO2EntityMapper.mapEntityList2VOList(MirrorVOEntityMapper.MAPPER::mirror2MirrorVO, mirrorList);
    }

    /**
     * 根据用户名和镜像的私有/公开权限从数据库中获取可以上传的镜像
     * @param principal 该参数无需前端传递，加上不会影响前端定义的接口参数，principal存储了当前登录用户的相关信息，比如username
     * @return 将镜像的VO以List的方式传给前端
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/mirror/getMirrorByPrivacy",method= RequestMethod.GET,produces = "application/json;charset=UTF-8")
    @WebLog(description = "得到所有可以上传的镜像")
    public List<MirrorVO> getMirrorByPrivacy(Principal principal) {
        List<Mirror> mirrorList = mirrorDAO.findByPrivacyOrUserUsername("public", principal.getName());
        // 调用mirrorService文件中的getMirrorByPrivacy方法, 返回的Mirror再转成VO
        return VO2EntityMapper.mapEntityList2VOList(MirrorVOEntityMapper.MAPPER::mirror2MirrorVO, mirrorList);
    }


    /**
     * @param mirrorName: 待检查镜像的名称
     * @Description: 检查镜像名称是否已经被注册
     * @return: aizoo.response.ResponseResult: 包括mirror, message
     * @throws:
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/mirror/checkMirrorName", method = RequestMethod.GET)
    @WebLog(description = "上传前检查mirror的名称是否已经被注册")
    @ResponseBody
    public ResponseResult checkMirrorName(@RequestParam(value = "mirrorName") String mirrorName, Principal principal) {
        if (mirrorName != null) {
            boolean flag = mirrorService.checkMirrorName(mirrorName, principal.getName());
            if (flag) {
                return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), true);
            }
        }
        return new ResponseResult(ResponseCode.HAVE_SAME_MIRRORNAME.getCode(),
                "已存在相同的镜像文件名：" + mirrorName + ",请改名后重试", false);
    }

    /**
     * 文件切片上传, 将切片文件上传至临时目录, 权限不同切片路径不同
     * @param privacy 权限
     * @param chunk     文件切片
     * @param principal 规则
     * @return 无返回值
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/mirror/uploadChunk", method = RequestMethod.POST)
    @WebLog(description = "文件切片上传")
    @ResponseBody
    public void uploadChunk(@RequestParam(value = "privacy") String privacy, TChunkInfoVO chunk, Principal principal) {
        MultipartFile mirrorFile = chunk.getChunkFile();
        logger.info("file originName: {}, chunkNumber: {}", mirrorFile.getOriginalFilename(), chunk.getChunkNumber());
        logger.info("The privacy of the uploading mirror is: {}", privacy);
        try {
            byte[] bytes = mirrorFile.getBytes();
            // 切片文件上传至用户临时目录
            Path path = null;
            if (privacy.equals("public")) {
                path = Paths.get(FileUtil.generatePath(filePath, "", chunk));
            }
            else if (privacy.equals("private")){
                path = Paths.get(FileUtil.generatePath(filePath, principal.getName(), chunk));
            }
            logger.info("upload path: {}", path);
            //文件写入指定路径
            Files.write(path, bytes);
        } catch (IOException e) {
            logger.error("uploadChunk Failed！错误: {}", e.getMessage());
        }
    }


    /**
     * 镜像切片合并后上传
     * @param mirrorVO 镜像
     * @param fileInfo 文件输入流
     * @return 上传成功或者失败信息
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/mirror/upload", method = RequestMethod.POST)
    @WebLog(description = "镜像切片文件的合并并复制到指定文件中")
    public ResponseResult uploadMirror(@MultiRequestBody("mirrorVO") MirrorVO mirrorVO,
                                       @MultiRequestBody("fileInfo") TFileInfoVO fileInfo) {
        try {
            mirrorService.uploadMirror(fileInfo, mirrorVO);
        } catch (Exception e) {
            logger.error("Upload Mirror Failed!");
            logger.error("Failed mirrorName: {}, error: {}", mirrorVO.getName(), e.getMessage());
            return new ResponseResult(ResponseCode.MIRROR_UPLOAD_ERROR.getCode(),
                    ResponseCode.MIRROR_UPLOAD_ERROR.getMsg() + ":" + e.getMessage(), null);
        }
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
    }
}
