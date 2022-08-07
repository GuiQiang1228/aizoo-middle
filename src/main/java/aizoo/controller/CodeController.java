package aizoo.controller;

import aizoo.annotation.MultiRequestBody;
import aizoo.aspect.WebLog;
import aizoo.domain.Code;
import aizoo.repository.CodeDAO;
import aizoo.repository.MirrorJobDAO;
import aizoo.repository.UserDAO;
import aizoo.response.BaseResponse;
import aizoo.response.ResponseCode;
import aizoo.response.ResponseResult;
import aizoo.service.CodeService;
import aizoo.service.ResourceUsageService;
import aizoo.utils.FileUtil;
import aizoo.viewObject.mapper.CodeVOEntityMapper;
import aizoo.viewObject.object.CodeVO;
import aizoo.viewObject.object.TChunkInfoVO;
import aizoo.viewObject.object.TFileInfoVO;
import aizoo.viewObject.pagebleUtils.VO2EntityMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.List;
import java.util.Map;

@BaseResponse
@RestController
public class CodeController {

    @Autowired
    CodeDAO codeDAO;

    @Autowired
    MirrorJobDAO mirrorJobDAO;

    @Autowired
    UserDAO userDAO;

    @Autowired
    CodeService codeService;

    @Autowired
    private ResourceUsageService resourceUsageService;

    @Value("${download.url}")
    String downloadUrl;
    @Value("${download.dir}")
    String downloadDir;

    @Value("${file.path}")
    String filePath;

    private final static Logger logger = LoggerFactory.getLogger(CodeController.class);

    /**
     * 利用springboot自带分页功能进行分页搜索
     * 参数为搜索需要的信息
     *
     * @param name            code名称
     * @param desc            code描述
     * @param startUpdateTime 开始更新时间
     * @param endUpdateTime   结束更新时间
     * @param pageNum         当前页号
     * @param pageSize        每页有几条记录
     * @param principal       用户信息
     * @return 返回查询到的转换格式后的Page对象
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/code/searchPage", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "万能分页code搜索")
    public Page<CodeVO> searchPage(@RequestParam(value = "name", required = false, defaultValue = "") String name,
                                   @RequestParam(value = "desc", required = false, defaultValue = "") String desc,
                                   @RequestParam(value = "startUpdateTime", required = false, defaultValue = "") String startUpdateTime,
                                   @RequestParam(value = "endUpdateTime", required = false, defaultValue = "") String endUpdateTime,
                                   @RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                   @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize, Principal principal) {
        String userName = principal.getName();
        //不带排序的pageable对象
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        //加入了pageable对象的查询语句，通过该pageable对象分析生成一个带分页查询的sql语句，返回存储JPA查询数据库的结果集（codePage)
        Page<Code> codePage = codeDAO.searchCode(name, desc, startUpdateTime, endUpdateTime, userName, pageable);
        return VO2EntityMapper.mapEntityPage2VOPage(CodeVOEntityMapper.MAPPER::code2CodeVO, codePage);
    }

    /**
     * 根据principal里面的username
     * 查数据库中用户所有的code 拼接路径得到一个路径地址
     * 遍历该路径下的所有文件 判断是否时目录并放进map
     * 最后将所有map放进list并返回
     *
     * @param userIdx   用户提供的相对路径
     * @param id        code下层的id
     * @param principal 用户信息
     * @return List<Map < String, Object>, ...>
     * mapKey: 一个相对路径 mapValue: 该相对路径下的文件是否是一个目录
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/code/getCodeList", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "获取用户的code列表")
    public List<Map<String, Object>> getCodeList(@RequestParam String userIdx, @RequestParam String id, Principal principal) {
        String userName = principal.getName();
        // 调用service的方法获取下一级路径
        return codeService.getNextPath(userIdx, id, userName);
    }

    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/code/getCode", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "获取用户的所有code")
    public List<CodeVO> getCode(Principal principal) {
        String username = principal.getName();
        List<Code> codeList = codeDAO.findByUserUsername(username);
        return VO2EntityMapper.mapEntityList2VOList(CodeVOEntityMapper.MAPPER::code2CodeVO, codeList);
    }

    /**
     * 下载代码源文件
     *
     * @param id 代码id
     * @return 代码源文件信息
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/code/downloadCode", method = RequestMethod.GET)
    @WebLog(description = "下载代码源文件")
    public ResponseResult downloadCodeFiles(@RequestParam Long id) throws Exception {
        String filePath = codeService.downloadAtomicCodeFiles(id);
        String urlPath = filePath.replaceAll(downloadDir, "");
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), downloadUrl + urlPath);
    }

    /**
     * 删除资源管理页面用户拥有的代码资源
     *
     * @param id        组件或数据资源id
     * @param principal 规则
     * @return 删除信息
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/code/deleteCode", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "删除资源管理页面用户拥有的代码资源")
    public ResponseResult deleteCode(@MultiRequestBody("id") long id, Principal principal) throws Exception {
        //根据id查找代码资源
        Code code = codeDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
        System.out.println("----用户名是" + code.getUser().getUsername());
        //判断用户名是否相同
        if (!code.getUser().getUsername().equals(principal.getName())) {
            return new ResponseResult(ResponseCode.DELETE_OUT_OF_BOUNDS.getCode(), ResponseCode.DELETE_OUT_OF_BOUNDS.getMsg(), null);
        }
        //删除数据资源
        codeService.deleteCode(id);
        resourceUsageService.updateDiskCapacity(principal.getName());
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
    }

    /**
     * 修改code的描述
     *
     * @param id          前端传入的codeID
     * @param description 前端传入的code的描述
     * @return ResponseResult 成功状态：{code: 200, msg: "成功", data: null} 失败状态：无
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/code/modifyCodeDesc", method = RequestMethod.POST)
    @WebLog(description = "修改description")
    public ResponseResult modifyCodeDesc(@MultiRequestBody(value = "id") long id, @MultiRequestBody(value = "description") String description) {
        Code code = codeDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
        code.setDescription(description);
        //保存修改描述后的code
        codeDAO.save(code);
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
    }

    /**
     * code切片上传，将切片的code文件上传至用户的临时目录下
     *
     * @param chunk     code文件切片
     * @param principal 当前用户信息
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/code/uploadCodeChunk", method = RequestMethod.POST)
    @WebLog(description = "code切片上传")
    public void uploadChunk(TChunkInfoVO chunk, Principal principal) {
        MultipartFile codeFile = chunk.getChunkFile();
        logger.info("code originName:{},chunkNumber:{}", codeFile.getOriginalFilename(), chunk.getChunkNumber());
        try {
            byte[] bytes = codeFile.getBytes();
            //切片code上传至用户临时目录(generatePath作用是生成文件切片地址，并将文件切片数据写入地址中)
            //临时路径为{file.path}/{username}/temp/{chunk.identifier}
            Path tempPath = Paths.get(FileUtil.generatePath(filePath, principal.getName(), chunk));
            //文件写入指定路径
            Files.write(tempPath, bytes);
        } catch (IOException e) {
            logger.error("uploadChunk Failed!");
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * @param codeName: 待检查的code名称
     * @Description: 检查code名称是否已经被注册
     * @return: aizoo.response.ResponseResult：包括code，message和true/false
     * @throws:
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/code/checkCodeName", method = RequestMethod.GET)
    @WebLog(description = "检查code的名称是否已经被注册")
    @ResponseBody
    public ResponseResult checkCodeName(@RequestParam(value = "codeName") String codeName, Principal principal) {
        if (codeName != null) {
            boolean flag = codeService.checkCodeName(codeName, principal.getName());
            if (flag) {
                return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), true);
            }
        }
        return new ResponseResult(ResponseCode.HAVE_SAME_CODENAME.getCode(),
                "已存在相同的代码文件名：" + codeName + ",请改名后重试", false);
    }

    /**
     * 上传code,实现切片文件的合并并复制到指定文件中
     *
     * @param codeVO   code组织成VO的信息
     * @param fileInfo code信息，前端MD5上传
     * @return ResponseResult, 是否上传成功
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/code/uploadCode", method = RequestMethod.POST)
    @WebLog(description = "实现切片文件的合并并复制到指定文件中")
    public ResponseResult uploadCodeFile(@MultiRequestBody("code") CodeVO codeVO,
                                         @MultiRequestBody("fileInfo") TFileInfoVO fileInfo) {
        try {
            //实现代码上传
            codeService.uploadCode(fileInfo, codeVO);
        } catch (Exception e) {
            logger.error("uploadCode Failed!");
            logger.error(e.getMessage(), e);
            return new ResponseResult(ResponseCode.CODE_UPLOAD_FAILED.getCode(),
                    ResponseCode.CODE_UPLOAD_FAILED.getMsg() + ":" + e.getMessage(), null);
        }
        return new ResponseResult(ResponseCode.SUCCESS.getCode(),
                ResponseCode.SUCCESS.getMsg(), null);
    }

    /**
     * @param id           代码的id
     * @param relativePath 文件的相对路径
     * @param fileInfo 文件信息，前端MD5上传
     * @return ResponseResult, 是否上传成功
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/code/uploadSingle", method = RequestMethod.POST)
    @WebLog(description = "上传单个文件")
    public ResponseResult uploadSingleCodeFile(@MultiRequestBody("id") long id,
                                               @MultiRequestBody("relativePath") String relativePath,
                                               @MultiRequestBody("fileInfo") TFileInfoVO fileInfo, Principal principal) throws Exception {
        try {
            //实现代码上传
            codeService.uploadSingleFile(fileInfo, id, relativePath, principal.getName());
        } catch (Exception e) {
            logger.error("uploadCode Failed!");
            logger.error(e.getMessage(), e);
            return new ResponseResult(ResponseCode.SINGLE_FILE_UPLOAD_FAILED.getCode(),
                    ResponseCode.SINGLE_FILE_UPLOAD_FAILED.getMsg() + ":" + e.getMessage(), null);
        }
        return new ResponseResult(ResponseCode.SUCCESS.getCode(),
                ResponseCode.SUCCESS.getMsg(), null);
    }
}

