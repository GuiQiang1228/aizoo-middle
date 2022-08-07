package aizoo.controller;

import aizoo.annotation.MultiRequestBody;
import aizoo.aspect.ResourceCheck;
import aizoo.aspect.WebLog;
import aizoo.common.Node;
import aizoo.common.ResourceType;
import aizoo.common.exception.NoSlurmAccountException;
import aizoo.common.exception.ReleaseGraphException;
import aizoo.domain.Application;
import aizoo.domain.Component;
import aizoo.domain.Datasource;
import aizoo.domain.MirrorJob;
import aizoo.repository.*;
import aizoo.response.BaseResponse;
import aizoo.response.ResponseCode;
import aizoo.response.ResponseResult;
import aizoo.service.ApplicationService;
import aizoo.service.GraphService;
import aizoo.utils.ListEntity2ListVO;
import aizoo.viewObject.mapper.ApplicationVOEntityMapper;
import aizoo.viewObject.object.ApplicationVO;
import aizoo.viewObject.object.GraphVO;
import aizoo.viewObject.object.VisualContainerVO;
import aizoo.viewObject.pagebleUtils.VO2EntityMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityNotFoundException;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

@BaseResponse
@RestController
public class ApplicationController {

    @Autowired
    VisualContainerDAO visualContainerDAO;

    @Autowired
    ApplicationDAO applicationDAO;

    @Autowired
    ApplicationService applicationService;

    @Autowired
    ApplicationResultDAO applicationResultDAO;

    @Autowired
    GraphService graphService;

    @Autowired
    private ComponentDAO componentDAO;

    @Autowired
    DatasourceDAO datasourceDAO;

    @Autowired
    ServiceDAO serviceDAO;

    private static final Logger logger = LoggerFactory.getLogger(ApplicationController.class);

    /**
     * 该接口功能是获取所有的可视化容器
     *
     * @return List<VisualContainerVO> 所有的可视化容器的VO列表
     */
    @RequestMapping(value = "/api/design/visualContainer", method = RequestMethod.GET)
    @WebLog(description = "获取所有的可视化容器")
    public List<VisualContainerVO> getVisualContainer() {
        return ListEntity2ListVO.container2ContainerVO(visualContainerDAO.findAll());
    }

    /**
     * 根据关键字搜索可视化容器
     *
     * @param keyword 用于可视化容器搜索的关键字
     * @return List<VisualContainerVO> 搜索后命中的可视化容器VO列表
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/design/searchVisualContainer", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "根据关键字搜索可视化容器")
    public List<VisualContainerVO> getDataBYKeyword(@RequestParam String keyword) {
        return ListEntity2ListVO.container2ContainerVO(visualContainerDAO.findByTitleLike("%" + keyword + "%"));
    }

    /**
     * application高级搜索
     *
     * @param appName           对application的名字作搜索限制，非必传参数，默认值为空字符串
     * @param desc              对application的描述作搜索限制，非必传参数，默认值为空字符串
     * @param graphName         对application对应图的名字作搜索限制，非必传参数，默认值为空字符串
     * @param applicationStatus 对application的状态作搜索限制，非必传参数，默认值为空字符串
     * @param startUpdateTime   对application的更新开始时间作搜索限制，非必传参数，默认值为空字符串
     * @param endUpdateTime     对application的更新结束时间作搜索限制，非必传参数，默认值为空字符串
     * @param pageNum           对搜索页数作搜索限制，非必传参数，默认值为0
     * @param pageSize          对每页的数目作搜索限制，非必传参数，默认值为10
     * @param principal         当前登录用户信息（前端无需传入，用户通过 java security 认证后，即可使用）
     * @return Page<ApplicationVO> 根据限制条件搜索到的ApplicationVO分页
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/application/searchPage", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "万能分页App搜索")
    public Page<ApplicationVO> searchPage(@RequestParam(value = "applicationName", required = false, defaultValue = "") String appName,
                                          @RequestParam(value = "desc", required = false, defaultValue = "") String desc,
                                          @RequestParam(value = "graphName", required = false, defaultValue = "") String graphName,
                                          @RequestParam(value = "applicationStatus", required = false, defaultValue = "") String applicationStatus,
                                          @RequestParam(value = "startUpdateTime", required = false, defaultValue = "") String startUpdateTime,
                                          @RequestParam(value = "endUpdateTime", required = false, defaultValue = "") String endUpdateTime,
                                          @RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                          @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize, Principal principal) {
        String userName = principal.getName();
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        Page<Application> appsPage = applicationDAO.searchApplication(appName, desc, graphName, applicationStatus, startUpdateTime, endUpdateTime, userName, pageable);
        //将命中的Application页面转化为对应的VO页面再进行返回
        return VO2EntityMapper.mapEntityPage2VOPage(ApplicationVOEntityMapper.MAPPER::application2ApplicationVO, appsPage);
    }

    /**
     * 删除指定的application
     *
     * @param appId     需删除应用的id
     * @param principal 当前登录用户信息（前端无需传入，用户通过 java security 认证后，即可使用）
     * @return ResponseResult 成功状态：{code: 200, msg: "成功", data: null}，失败状态：{code: 10072, msg: "非本人资源，无法删除", data: null}  {code: 10083, msg: "删除应用失败", data: null}
     * @throws Exception
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/application/delete", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "删除指定app")
    public ResponseResult removeJob(@MultiRequestBody("applicationJobId") long appId, Principal principal) throws Exception {
        Application app = applicationDAO.findById(appId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(appId)));
        if (!principal.getName().equals(app.getUser().getUsername())) {
            return new ResponseResult(ResponseCode.DELETE_OUT_OF_BOUNDS.getCode(), ResponseCode.DELETE_OUT_OF_BOUNDS.getMsg(), null);
        }
        try {
            boolean success = applicationService.removeAppById(appId);
            return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), success);
        } catch (Exception e) {
            logger.error("删除应用失败！id={}, error={}", appId, e);
        }
        return new ResponseResult(ResponseCode.APPLICATION_DELETE_FAILED.getCode(), ResponseCode.APPLICATION_DELETE_FAILED.getMsg(), null);
    }

    /**
     * 停止指定的application job
     *
     * @param applicationJobId 需停止的application job的id
     * @param principal        当前登录用户信息（前端无需传入，用户通过 java security 认证后，即可使用）
     * @return ResponseResult 成功状态：{code: 200, msg: "成功", data: null}
     * 失败状态：{code: 10074, msg: "非本人JOB，无法停止", data: null}  若数据库中的job状态与查到的状态不一样  返回 {code: 12000, msg: "终止失败请重试", data: null}
     * @throws Exception
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/application/stop", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "停止指定app")
    public ResponseResult stopJob(@MultiRequestBody("applicationJobId") long applicationJobId, Principal principal) throws Exception {
        boolean result = false;
        try {
            Application application = applicationDAO.findById(applicationJobId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(applicationJobId)));
            if (!application.getUser().getUsername().equals(principal.getName())) {
                return new ResponseResult(ResponseCode.JOBSTOP_OUT_OF_BOUNDS.getCode(), ResponseCode.JOBSTOP_OUT_OF_BOUNDS.getMsg(), result);
            }
            result = applicationService.slurmStopApplication(applicationJobId);
        } catch (JsonProcessingException e) {
            logger.error("stopJob fail, error: {}",e);
        }
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), result);
    }

    /**
     * 执行application
     *
     * @param graphVO   前端传递的graphVO
     * @param appVO     前端传递的applicationVO
     * @param principal 当前登录用户信息（前端无需传入，用户通过 java security 认证后，即可使用）
     * @return ResponseResult 成功状态：{code: 200, msg: "成功", data: null}
     * 失败状态：{code: 30009, msg: "发布图失败！", data: null} {code: 10006, msg: "无slurm账号，无法执行！", data: null} {code: 10005, msg: "执行不成功，请重试！", data: null}
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/application/execute", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "执行application")
    @ResourceCheck(resourceTypes = {ResourceType.DISK, ResourceType.CPU, ResourceType.GPU, ResourceType.MEMORY,
            ResourceType.APPLICATION_RUNNING_NUMBER, ResourceType.APPLICATION_TOTAL_NUMBER})
    public ResponseResult applicationExecute(@MultiRequestBody("graph") GraphVO graphVO,
                                             @MultiRequestBody("application") ApplicationVO appVO,
                                             @MultiRequestBody("initJobId") long initJobId, Principal principal) {
        try {
            //检查图是否存在
            if(!graphService.checkGraphExist(graphVO.getId()))
                return new ResponseResult(ResponseCode.GRAPH_NOT_EXIST_ERROR.getCode(), ResponseCode.GRAPH_NOT_EXIST_ERROR.getMsg(), null);
            //检查图中节点是否有缺失
            for (Node node : graphVO.getNodeList()) {
                if(node.getComponentVO() !=null){
                    Component component = componentDAO.findById(node.getComponentVO().getId()).
                            orElseThrow(() -> new EntityNotFoundException("Missing component,id:"+String.valueOf(node.getComponentVO().getId())));
                }
                if(node.getServiceVO() !=null){
                    aizoo.domain.Service service=serviceDAO.findById(node.getServiceVO().getId()).
                            orElseThrow(() -> new EntityNotFoundException("Missing service,id:"+String.valueOf(node.getServiceVO().getId())));
                }
                if(node.getDatasourceVO() !=null){
                    Datasource datasource = datasourceDAO.findById(node.getDatasourceVO().getId()).
                            orElseThrow(() -> new EntityNotFoundException("Missing datasource,id:"+String.valueOf(node.getDatasourceVO().getId())));
                }
            }
            graphService.releaseGraphVersion(graphVO);
            applicationService.executeApplication(graphVO, appVO, principal.getName());
            //若job为复用job，删除原来的job
            Optional<Application> optional = applicationDAO.findById(initJobId);
            if(optional.isPresent())
                applicationService.removeAppById(initJobId);
        } catch (ReleaseGraphException e) {
            logger.error("applicationExecute fail, ReleaseGraphException error={}",e);
            return new ResponseResult(ResponseCode.RELEASE_GRAPH_ERROR.getCode(), ResponseCode.RELEASE_GRAPH_ERROR.getMsg(), null);
        } catch (NoSlurmAccountException e) {
            logger.error("applicationExecute fail, NoSlurmAccountException error={}",e);
            return new ResponseResult(ResponseCode.NO_SLURM_ACCOUNT_ERROR.getCode(), ResponseCode.NO_SLURM_ACCOUNT_ERROR.getMsg(), null);
        } catch (EntityNotFoundException e){
            logger.error("EntityNotFoundException!! error= {}",e);
            return new ResponseResult(ResponseCode.NODE_NOT_EXIST_ERROR.getCode(), ResponseCode.NODE_NOT_EXIST_ERROR.getMsg(),null);
        } catch (Exception e) {
            logger.error("applicationExecute fail, error: {}", e);
            return new ResponseResult(ResponseCode.RUN_ERROR.getCode(), ResponseCode.RUN_ERROR.getMsg(), null);
        }
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
    }
}
