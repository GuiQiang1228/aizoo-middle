package aizoo.controller;

import aizoo.annotation.MultiRequestBody;
import aizoo.common.ComponentType;
import aizoo.common.GraphType;
import aizoo.domain.*;
import aizoo.repository.*;
import aizoo.response.BaseException;
import aizoo.response.BaseResponse;
import aizoo.response.ResponseCode;
import aizoo.response.ResponseResult;
import aizoo.utils.DAOUtil;
import aizoo.utils.FileUtil;
import aizoo.utils.ListEntity2ListVO;
import aizoo.viewObject.mapper.ExperimentJobVOEntityMapper;
import aizoo.viewObject.object.*;
import aizoo.viewObject.mapper.GraphVOEntityMapper;
import aizoo.viewObject.pagebleUtils.VO2EntityMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.EnumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import aizoo.aspect.WebLog;
import aizoo.service.*;


import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.*;

@BaseResponse
@RestController
public class GraphController {

    @Value("${file.path}")
    String filePath;

    @Value("${download.url}")
    String downloadUrl;

    @Value("${download.dir}")
    String downloadDir;

    @Autowired
    private GraphDAO graphDAO;

    @Autowired
    ComponentDAO componentDAO;

    @Autowired
    DAOUtil daoUtil;

    @Autowired
    ServiceDAO serviceDAO;

    @Autowired
    DatatypeDAO datatypeDAO;

    @Autowired
    NamespaceDAO namespaceDAO;

    @Autowired
    ServiceJobDAO serviceJobDAO;

    @Autowired
    UserDAO userDAO;

    @Autowired
    DatasourceDAO datasourceDAO;

    @Autowired
    ComponentInputParameterDAO componentInputParameterDAO;

    @Autowired
    ComponentOutputParameterDAO componentOutputParameterDAO;

    @Autowired
    private GraphService graphService;

    @Autowired
    private ComponentService componentService;

    @Autowired
    private FileService fileService;

    @Autowired
    private TranslationService translationService;

    @Autowired
    private ExperimentJobDAO experimentJobDAO;

    @Autowired
    ApplicationDAO applicationDAO;

    private static final Logger logger = LoggerFactory.getLogger(GraphController.class);

    /**
     * 保存图数据
     * @param graphVO  前端需要传入的图数据结构，具体见 aizoo\viewObject\object\GraphVO.java
     * @param principal 当前登录用户信息（前端无需传入，用户通过 java security 认证后，即可使用）
     * @return ResponseResult 成功状态：{code: 200, msg: "成功", data: null} 失败状态：{code: 10073, msg: "非本人资源，无法修改", data: null}
     * 异常状态：{code: 10069, msg: "保存图失败:图中部分节点的数据信息已删除", data: null}
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "api/design/save", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "保存图")
    public ResponseResult saveGraph(@RequestBody GraphVO graphVO, Principal principal) {
        // 此处可用vo的mapper转换
        try {
            // 将 GraphVO 转换为 Graph 类型，以便存入数据库
            Graph graph = GraphVOEntityMapper.MAPPER.graphVO2Graph(graphVO, graphDAO, componentDAO, daoUtil, datatypeDAO);

            // 若不是当前用户的图资源，则返回 CHANGE_OUT_OF_BOUNDS(10073, "非本人资源，无法修改")
            if (!(graph.getUser().getUsername().equals(principal.getName()))) {
                return new ResponseResult(ResponseCode.CHANGE_OUT_OF_BOUNDS.getCode(), ResponseCode.CHANGE_OUT_OF_BOUNDS.getMsg(), null);
            }
            // 将 graph 存入数据库
            graphDAO.saveAndFlush(graph);
            //重新组织并保存对应的fileList
            graphService.saveFileList(graphVO);
        } catch (EntityNotFoundException | JsonProcessingException e) {
            logger.error("Fail to saveGraph,graphVO: {},错误信息:", graphVO.toString(),e);
            e.printStackTrace();
            return new ResponseResult(ResponseCode.GRAPH_SAVA_ERROR.getCode(), ResponseCode.GRAPH_SAVA_ERROR.getMsg() + ":图中部分节点的数据信息已删除", null);
        }
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
    }

    /**
     * 发布复合组件
     * @param graphVO 前端需要传入的图数据结构，具体见 aizoo\viewObject\object\GraphVO.java
     * @param componentVO 前端需要传入的组件数据结构，具体见 aizoo\viewObject\object\ComponentVO.java
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "api/design/releaseComponent", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "发布为复合组件")
    public void releaseComponent(@MultiRequestBody("graph") GraphVO graphVO, @MultiRequestBody("component") ComponentVO componentVO) {
        // 保存图以及对应的component
        graphService.release2ComponentAndGraph(graphVO, componentVO);
    }

    /**
     * 通过类型获取图列表分页信息
     * @param pageNum 前端请求当前页码，默认为0
     * @param pageSize 前端请求每页大小，默认为10
     * @param type 前端请求图类型，包括JOB、DATALOADER、MODULE、MODEL、LOSS、SERVICE、PROCESS、METRIC、APPLICATION
     * @param principal 当前登录用户信息（前端无需传入，用户通过 java security 认证后，即可使用）
     * @return Page<GraphVO> 所请求图列表的分页信息
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/design/openGraph", method = RequestMethod.GET)
    @WebLog(description = "分页返回JOB/DATALOADER/MODULE/MODEL/LOSS/SERVICE/PROCESS/METRIC/APPLICATION列表")
    public Page<GraphVO> getGraphListByType(@RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize, @RequestParam String type, Principal principal) {
        return graphService.getGraphListByType(pageNum, pageSize, principal.getName(), type);
    }

    /**
     * 通过图id获取图数据，以便打开一张图
     * @param graphId 前端传入的图id
     * @return ResponseResult 成功状态：{code: 200, msg: "成功", data: map对象} 失败状态：无
     * 如果图类型为component，map的key为 graph、namespace、componentId
     * 如果图类型为service，map的key为 graph、namespace、serviceId
     * 如果图类型为job或者application，map的key为 graph、namespace、componentId
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "api/design/open/{graphId}", method = RequestMethod.GET)
    @WebLog(description = "打开一张图")
    public ResponseResult getGraphByGraphId(@PathVariable Long graphId) {
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), graphService.openGraph(graphId));
    }

    /**
     * 创建一张图
     * @param name 前端传入的图名称
     * @param type 前端传入的图类型
     * @param namespace 前端传入的命名空间
     * @param privacy 前端传入的权限
     * @param desc 前端传入的图描述
     * @param principal 当前登录用户信息（前端无需传入，用户通过 java security 认证后，即可使用）
     * @return GraphVO 具体数据结构见aizoo.viewObject.object.GraphVO
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/design/create", method = RequestMethod.POST)
    @WebLog(description = "新建图")
    public GraphVO createGraph(@MultiRequestBody String name, @MultiRequestBody String type,
                               @MultiRequestBody String namespace, @MultiRequestBody String privacy,
                               @MultiRequestBody(value = "description") String desc, Principal principal) {
        // 验证 type 是否属于 ComponentType or GraphType ，若不属于抛出GRAPH_TYPE_ERROR(30003, "图类型不存在")
        if (!graphService.isValidGraph(type)) {
            throw new BaseException(ResponseCode.GRAPH_TYPE_ERROR);
        }
        // 创建图
        return graphService.createGraph(name, type, namespace, principal.getName(), privacy, desc);
    }

    /**
     * 判断两节点是否相连
     * 前端可以实现，暂时注释返回200
     *
     * @return ResponseResult 成功状态：{code: 200, msg: "成功", data: null} 失败状态：无
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/design/canLink", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "判断能否相连")
    public ResponseResult getCanLink() {
//    public ResponseResult getCanLink(@RequestBody ConnectVO connectVO) {
//        canLink前端可以实现，暂时注释返回200
//        Map<String, String> result = validationService.getCanLink(connectVO);
//        if(!result.get("result").equals("success")){
//            return new ResponseResult(ResponseCode.CAN_LINK_ERROR.getCode(),result.get("reason"),null);
//        }
        // =====
        // canLink前端可以实现，暂时注释返回200
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
        // =====
    }

    /**
     * 上传截图
     *
     * @param pictureVO 前端传入的截图数据
     * @param principal 当前登录用户信息（前端无需传入，用户通过 java security 认证后，即可使用）
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/design/savePicture", method = RequestMethod.POST)
    @WebLog(description = "上传图的截图")
    public void uploadGraphPicture(PictureVO pictureVO, Principal principal) {
        // 此处前端已将截图文件的名称改为graphId_graphName.png
        try {
            fileService.uploadPicture(pictureVO, principal.getName());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            System.out.println(e.getMessage());
            throw new BaseException(ResponseCode.GRAPH_PICTURE_UPLOAD_ERROR);
        }
    }

    /**
     * 根据图id下载截图
     *
     * @param graphId 前端传入的图id
     * @return ResponseEntity<byte[]> 图片字节流信息
     * @throws IOException
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/design/graphPicture", method = RequestMethod.GET)
    @WebLog(description = "根据graphId下载graph截图")
    @ResponseBody
    public ResponseEntity<byte[]> downloadAllGraphPicStream(Long graphId) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return graphService.downloadAllGraphPicStream(graphId, authentication.getName());
    }

    /**
     * elasticsearch查询时，根据graphId和name查看缩略图
     * name可以是其它人的用户名
     *
     * @param graphId 前端传入的图id
     * @param name 前端传入的用户名
     * @return ResponseEntity<byte[]> 图片字节流信息
     * @throws IOException
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/design/graphPicture/elasticsearch", method = RequestMethod.GET)
    @WebLog(description = "根据graphId下载graph截图-> elasticsearch查询时可以查看他人缩略图")
    @ResponseBody
    public ResponseEntity<byte[]> downloadAllGraphPicStreamInElasticSearch(Long graphId, String name) throws IOException {
        return graphService.downloadAllGraphPicStream(graphId, name);
    }

    /**
     * job/service/application图根据jobId/serviceJobId/applicationId下载job截图
     *
     * @param id 前端传入的id
     * @param type 前端传入的type，包括 job、service、application
     * @return ResponseEntity<byte[]> 图片字节流信息
     * @throws IOException
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/job/jobPicture", method = RequestMethod.GET)
    @WebLog(description = "job/service/application图根据jobId/serviceJobId/applicationId下载job截图")
    @ResponseBody
    public ResponseEntity<byte[]> downloadJobPicStream(Long id, String type) throws IOException {

        long graphId = 0;
        if (type.equals("job")) {
            Graph graph = graphDAO.findByExperimentJobsId(id);
            graphId = graph.getId();
        } else if (type.equals("service")) {
            ServiceJob serviceJob = serviceJobDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
            graphId = serviceJob.getGraph().getId();
        } else if (type.equals("application")) {
            Application application = applicationDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
            graphId = application.getGraph().getId();
        }
        return downloadAllGraphPicStream(graphId);
    }

    /**
     * 删除design页面的任务 or 组件 or 模型
     *
     * @param id 前端传入的id
     * @param principal 当前登录用户信息（前端无需传入，用户通过 java security 认证后，即可使用）
     * @return ResponseResult 成功状态：{code: 200, msg: "成功", data: null}，失败状态：{code: 10072, msg: "非本人资源，无法删除", data: null}
     * @throws Exception
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/design/delete", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "删除design页面的任务/组件/模型")
    public ResponseResult deleteDesignGraph(@MultiRequestBody long id, Principal principal) throws Exception {
        Graph graph = graphDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
        if (!(graph.getUser().getUsername().equals(principal.getName()))) {
            return new ResponseResult(ResponseCode.DELETE_OUT_OF_BOUNDS.getCode(), ResponseCode.DELETE_OUT_OF_BOUNDS.getMsg(), null);
        }
        if(graph.getJobs().size()>0)
            return new ResponseResult(ResponseCode.GRAPH_DELETE_FAILED.getCode(), ResponseCode.GRAPH_DELETE_FAILED.getMsg(), null);
        // 通过graphId删除design页面的任务 or 组件 or 模型
        graphService.deleteGraphById(id, principal.getName());
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
    }

    /**
     * 返回当前用户所有graphType为实验、服务、应用的图数据信息
     *
     * @param graphType 前端传入的类型
     * @param principal 当前登录用户信息（前端无需传入，用户通过 java security 认证后，即可使用）
     * @return List<GraphVO> 图数据列表
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/design/getAllJobGraph", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "返回当前用户所有graphType为实验、服务、应用的图数据信息")
    public List<GraphVO> getAllJobGraph(GraphType graphType, Principal principal) {
        return ListEntity2ListVO.graph2GraphVO(graphDAO.findByUserUsernameAndGraphType(principal.getName(), graphType));
    }

    /**
     * 复用当前graph，并新建一个graph
     *
     * @param graphId 前端传入的id
     * @param principal 当前登录用户信息（前端无需传入，用户通过 java security 认证后，即可使用）
     * @return Long类型，新建graph的id
     * @throws Exception
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/design/reuse", method = RequestMethod.POST)
    @WebLog(description = "复用当前graph，新建一个graph")
    public Long reuseGraph(@MultiRequestBody(value = "graphId") String graphId, Principal principal) throws Exception {
        String username = principal.getName();
        return graphService.reuseGraph(graphId, username);
    }

    /**
     * 获取组件已存在的所有版本号
     *
     * @param name 组件名称
     * @param namespace 命名空间
     * @param type 组件类型
     * @param principal 当前登录用户信息（前端无需传入，用户通过 java security 认证后，即可使用）
     * @return List<String> 已存在版本号的列表
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/design/getVersionList", method = RequestMethod.GET)
    @WebLog(description = "获取某个组件已经有的版本号")
    public List<String> getVersionList(@RequestParam(value = "name") String name, @RequestParam(value = "namespace") String namespace,
                                       @RequestParam(value = "type") String type, Principal principal) {
        List<String> versionList = new ArrayList<>();
        // 类型为：COMPONENT("component", "组件")
        if (GraphType.valueOf(type) == GraphType.COMPONENT) {
            List<Component> componentList = componentDAO.findByNameAndNamespaceNamespace(name, namespace);
            for (Component component1 : componentList) {
                versionList.add(component1.getComponentVersion());
            }
        } else { // 类型为：JOB("job", "实验") or SERVICE("service","服务") or APPLICATION("application","应用")
            GraphType graphType = GraphType.valueOf(type);
            List<Graph> graphList = graphDAO.findByUserUsernameAndGraphTypeAndNameAndReleased(principal.getName(), graphType, name, true);
            for (Graph g : graphList) {
                versionList.add(g.getGraphVersion());
            }
        }
        return versionList;
    }

    /**
     * 根据关键字搜索图数据，支持单一关键字搜索和组合关键字搜索
     *
     * @param type 图类型
     * @param version 图版本号
     * @param releaseStatus 发布状态
     * @param graphName 图名称
     * @param desc 图描述
     * @param startUpdateTime 搜索起始时间
     * @param endUpdateTime 搜索结束时间
     * @param pageNum 当前页码
     * @param pageSize 每页大小
     * @param principal 当前登录用户信息（前端无需传入，用户通过 java security 认证后，即可使用）
     * @return Page<GraphVO> 搜索到的分页图数据
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/design/search/searchPage", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "万能分页工作台graph搜索")
    public Page<GraphVO> graphSearchPage(@RequestParam(value = "type", required = false, defaultValue = "") String type,
                                         @RequestParam(value = "version", required = false, defaultValue = "") String version,
                                         @RequestParam(value = "releaseStatus", required = false, defaultValue = "") String releaseStatus,
                                         @RequestParam(value = "graphName", required = false, defaultValue = "") String graphName,
                                         @RequestParam(value = "desc", required = false, defaultValue = "") String desc,
                                         @RequestParam(value = "startUpdateTime", required = false, defaultValue = "") String startUpdateTime,
                                         @RequestParam(value = "endUpdateTime", required = false, defaultValue = "") String endUpdateTime,
                                         @RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                         @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize, Principal principal) {
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        String userName = principal.getName();
        // 转换releaseStatus类型
        if (releaseStatus.equals("false"))
            releaseStatus = "0";
        if (releaseStatus.equals("true"))
            releaseStatus = "1";
        // 根据关键字从数据库中查询匹配的分页图数据
        Page<Graph> graphPage = null;
        //如果type是组件图的类型，与component联查
        if (EnumUtils.isValidEnum(ComponentType.class, type)) {
            graphPage = graphDAO.searchComponent(version, releaseStatus, graphName, desc, startUpdateTime, endUpdateTime, type, userName, pageable);
        } else {  //否则只在graph中查
            graphPage = graphDAO.searchGraph(type, graphName, desc, startUpdateTime, endUpdateTime, userName, releaseStatus, version, pageable);
        }
        return VO2EntityMapper.mapEntityPage2VOPage(GraphVOEntityMapper.MAPPER::graph2GraphVO, graphPage);
    }


    // ==========
    // request、serviceVO参数暂未用到
    /**
     * 翻译、下载图数据
     * 包括组件图、实验图、服务图和应用图
     *
     * @param graphVO 前端传入的 GraphVO 数据结构
     * @param request 暂时没有用到
     * @param experimentJobVO 前端传入的 ExperimentJobVO 数据结构
     * @param componentVO 前端传入的 ComponentVO 数据结构
     * @param serviceVO 暂时没有用到
     * @param principal 当前登录用户信息（前端无需传入，用户通过 java security 认证后，即可使用）
     * @return ResponseResult 成功状态：{code: 200, msg: "成功", data: 文件下载地址 } 失败状态：{code: 10051, msg: "编译下载图失败!", data: null}
     * @throws Exception
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/design/compile", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "翻译和下载图代码")
    public ResponseResult compileGraph(@MultiRequestBody(value = "graph") GraphVO graphVO, HttpServletRequest request,
                                       @MultiRequestBody(value = "experimentJob", required = false) ExperimentJobVO experimentJobVO,
                                       @MultiRequestBody(value = "component", required = false) ComponentVO componentVO,
                                       @MultiRequestBody(value = "service", required = false) ServiceVO serviceVO, Principal principal) throws Exception {
        try {
            Graph graph = GraphVOEntityMapper.MAPPER.graphVO2Graph(graphVO, graphDAO, componentDAO, daoUtil, datatypeDAO);

            ExperimentJob job = null;
            if (experimentJobVO != null)
                job = ExperimentJobVOEntityMapper.MAPPER.jobVO2Job(experimentJobVO, experimentJobDAO);

            if (componentVO != null) {
                if (!componentVO.getReleased()) {
                    // 将 componentVO 调整组织为 component
                    Component component = graphService.componentCompilePrepare(componentVO);
                    graph.setComponent(component);
                    component.setGraph(graph);
                }
            }
            // 获取编译的临时图文件对象
            File codeFile = translationService.compileGraphTemp(graph, job, principal.getName());
            // 获取图中所有节点的代码路径
            Set componentFilePaths = componentService.getComponentFilePaths(graph);
            // 复制所有组件的文件到保存路径下，这个路径里只保存代码，不保存runtime log等文件
            FileUtil.copyComponentFiles(codeFile.getParent(), componentFilePaths, filePath);
            // 构造翻译、下载文件地址
            String filePath = fileService.downloadTempCode(codeFile.getParent(), graph.getId().toString());
            String urlPath = filePath.replaceAll(downloadDir, "");
            //删除编译下载产生的 /username/compiled/service(job/application)/jobId/乱码  文件夹
            logger.info("deletePath: {}", codeFile.getPath().replace("/" + codeFile.getName(), ""));
            FileUtil.deleteFile(new File(codeFile.getPath().replace("/" + codeFile.getName(), "")));
            return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), downloadUrl + urlPath);
        } catch (EntityNotFoundException e){
            logger.error("EntityNotFoundException!! error= {}",e.getMessage());
            return new ResponseResult(ResponseCode.NODE_NOT_EXIST_ERROR.getCode(), ResponseCode.NODE_NOT_EXIST_ERROR.getMsg()+e.getMessage(),null);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new ResponseResult(ResponseCode.GRAPH_COMPILE_ERROR.getCode(), ResponseCode.GRAPH_COMPILE_ERROR.getMsg() + e.getMessage(), null);
        }
    }

    /**
     * 修改图描述
     *
     * @param id 前端传入的 graphId
     * @param description 前端传入的图描述
     * @return ResponseResult 成功状态：{code: 200, msg: "成功", data: null} 失败状态：无
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/graph/modify/description", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "修改图的描述")
    public ResponseResult modifyGraphDesc(@MultiRequestBody(value = "id") long id, @MultiRequestBody(value = "description") String description) {
        graphService.modifyDescription(id, description);
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
    }

    /**
     * 修改图的命名空间
     *
     * @param id 前端传入的 graphId
     * @param namespace 前端传入的图命名空间
     * @return ResponseResult 成功状态：{code: 200, msg: "成功", data: null} 失败状态：无
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/graph/modify/namespace", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "修改图的namespace")
    public ResponseResult modifyGraphNamespace(@MultiRequestBody(value = "id") long id, @MultiRequestBody(value = "namespace") String namespace) {
        graphService.modifyGraphNamespace(id, namespace);
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
    }

    /**
     * 修改图名称
     *
     * @param id 前端传入的 graphId
     * @param graphName 待修改图名称
     * @return ResponseResult 成功状态：{code: 200, msg: "成功", data: null} 失败状态：无
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/graph/modify/name", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "修改图的名字")
    public ResponseResult modifyGraphName(@MultiRequestBody(value = "id") long id, @MultiRequestBody(value = "graphName") String graphName) {
        graphService.modifyGraphName(id, graphName);
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
    }

    /**
     * 检查图的子节点是否都存在
     *
     * @param id 前端传入的图id
     * @return ResponseResult 成功状态：{code: 200, msg: "成功", data: object} 失败状态：无
     * object为双层Map，第一层的string共有三种类型：component, service, datasource，前两个可能会有嵌套情况出现，第二层为<objectId, object>形式，
     * 将所有不存在的object添加进来返回给前端，采用这种形式主要是为了去重
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/graph/check/node", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "检查图的nodeList是否都存在")
    public ResponseResult checkGraph(@RequestParam(value = "graphId") long id) {
        HashMap<String, HashMap<Long, Object>> object = graphService.checkGraph(id);
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), object);
    }
}
