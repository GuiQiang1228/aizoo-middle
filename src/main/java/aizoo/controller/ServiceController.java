package aizoo.controller;

import aizoo.annotation.MultiRequestBody;
import aizoo.aspect.ResourceCheck;
import aizoo.aspect.WebLog;
import aizoo.common.JobStatus;
import aizoo.common.Node;
import aizoo.common.ResourceType;
import aizoo.common.exception.NoSlurmAccountException;
import aizoo.common.exception.ReleaseGraphException;
import aizoo.domain.Component;
import aizoo.domain.Datasource;
import aizoo.domain.MirrorJob;
import aizoo.domain.ServiceJob;
import aizoo.repository.*;
import aizoo.response.BaseResponse;
import aizoo.response.ResponseCode;
import aizoo.response.ResponseResult;
import aizoo.service.ExperimentService;
import aizoo.service.GraphService;
import aizoo.service.ServiceService;
import aizoo.utils.ListEntity2ListVO;
import aizoo.viewObject.mapper.ServiceJobVOEntityMapper;
import aizoo.viewObject.object.GraphVO;
import aizoo.viewObject.object.RunningServiceJobVO;
import aizoo.viewObject.object.ServiceJobVO;
import aizoo.viewObject.object.ServiceVO;
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
import javax.transaction.Transactional;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@BaseResponse
@RestController
public class ServiceController {

    @Autowired
    ServiceDAO serviceDAO;

    @Autowired
    ServiceService serviceService;

    @Autowired
    ExperimentService experimentService;

    @Autowired
    DatatypeDAO datatypeDAO;

    @Autowired
    ServiceJobDAO serviceJobDAO;

    @Autowired
    GraphService graphService;

    @Autowired
    private ComponentDAO componentDAO;

    @Autowired
    DatasourceDAO datasourceDAO;

    private static final Logger logger = LoggerFactory.getLogger(ServiceController.class);

    /**
     * 根据多个条件搜索ServiceJob信息，最后按照分页返回
     *
     * @param serviceName serviceJob名称
     * @param desc serviceJob描述信息
     * @param serviceStatus serviceJob状态
     * @param graphName serviceJob图名称
     * @param startUpdateTime 任务开始更新时间
     * @param endUpdateTime 任务结束更新时间
     * @param pageNum 页号
     * @param pageSize 每页大小
     * @param principal 用户登录信息
     * @return Page<ServiceJobVO>，将搜索到的 ServiceJobVO封装成 Page后返回
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/service/searchPage", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "万能分页ServiceJob搜索")
    public Page<ServiceJobVO> searchPage(@RequestParam(value = "serviceName", required = false, defaultValue = "") String serviceName,
                                         @RequestParam(value = "desc", required = false, defaultValue = "") String desc,
                                         @RequestParam(value = "serviceStatus", required = false, defaultValue = "") String serviceStatus,
                                         @RequestParam(value = "graphName", required = false, defaultValue = "") String graphName,
                                         @RequestParam(value = "startUpdateTime", required = false, defaultValue = "") String startUpdateTime,
                                         @RequestParam(value = "endUpdateTime", required = false, defaultValue = "") String endUpdateTime,
                                         @RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                         @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize, Principal principal) {
        String userName = principal.getName();
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        Page<ServiceJob> servicesPage = serviceJobDAO.searchServiceJob(serviceName, desc, serviceStatus, graphName, startUpdateTime, endUpdateTime, userName, pageable);
        return VO2EntityMapper.mapEntityPage2VOPage(ServiceJobVOEntityMapper.MAPPER::serviceJob2ServiceJobVO, servicesPage);
    }

    /**
     * 获取所有用户已发布的Service组件，并返回
     * 最终会放在用户画图界面目录的“服务”栏下，供用户使用
     *
     * @param principal 用户登录信息
     * @return List<ServiceVO>
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/design/service", method = RequestMethod.GET)
    @WebLog(description = "目录返回所有当前用户已发布的Service")
    public List<ServiceVO> getService(Principal principal) {
        return ListEntity2ListVO.service2ServiceVO(serviceDAO.findByUserUsernameAndReleased(principal.getName(), true));
    }

    /**
     * 根据某个服务组件的id，获取到该Service对应创建的所有 RunningServiceJobVO 列表
     *
     * @param id Service服务组件的id
     * @param principal 用户登录信息
     * @return List<RunningServiceJobVO>，其中RunningServiceJobVO是新定义的一种数据结构，它仅包含了 ServiceJob的名称信息
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/design/getServiceJobId", method = RequestMethod.GET)
    @WebLog(description = "返回该服务对应的所有serviceJob")
    public List<RunningServiceJobVO> getServiceJob(@RequestParam Long id, Principal principal) {
        List<ServiceJob> serviceJobs = serviceJobDAO.findByServiceIdAndUserUsernameAndJobStatusAndIpNotNull(id, principal.getName(), JobStatus.RUNNING);
        return ListEntity2ListVO.serviceJob2RunningServiceJobVO(serviceJobs);
    }

    /**
     * 让运行的ServiceJob任务停止
     * 先通过slurm获取当前job的状态，如果与数据库中的不一致则返回false，并更新数据库
     * 一致则终止任务并查询slurm更新数据库并返回true
     *
     * @param serviceJobId ServiceJob任务的id
     * @param principal 用户登录信息
     * @return ResponseResult，其中 "data"键的值是一个 boolean类型的数据，false表示失败，true表示成功
     * @throws Exception
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/service/stop", method = RequestMethod.POST)
    @WebLog(description = "serviceJob停止")
    public ResponseResult serviceStop(@MultiRequestBody long serviceJobId, Principal principal) throws Exception {
        //从slurm中获取当前job的状态，如果与数据库中的不一致则返回false，并更新数据库；一致则终止任务并查询slurm更新数据库并返回true
        boolean result = false;
        try {
            ServiceJob job = serviceJobDAO.findById(serviceJobId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(serviceJobId)));
            if(!principal.getName().equals(job.getUser().getUsername())){
                return new ResponseResult(ResponseCode.JOBSTOP_OUT_OF_BOUNDS.getCode(), ResponseCode.JOBSTOP_OUT_OF_BOUNDS.getMsg(),result);
            }
            result = serviceService.slurmStopServiceJob(serviceJobId);
        } catch (JsonProcessingException e) {
            logger.error("serviceStop fail, error: {}", e);
        }
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), result);
    }

    /**
     * 删除 serviceJob，包括数据库中记录、out目录下相关的文件、与project的关系、es索引
     *
     * @param serviceJobId ServiceJob任务的id
     * @param principal 用户登录信息
     * @return ResponseResult，data为null，仅返回状态码等信息
     * @throws Exception
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/service/delete", method = RequestMethod.POST)
    @WebLog(description = "serviceJob删除")
    public ResponseResult serviceDelete(@MultiRequestBody long serviceJobId, Principal principal) throws Exception {
        boolean flag = serviceService.deleteServiceJob(serviceJobId, principal.getName());
        if(!flag)
            return new ResponseResult(ResponseCode.DELETE_OUT_OF_BOUNDS.getCode(), ResponseCode.DELETE_OUT_OF_BOUNDS.getMsg(), null);
        else
            return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
    }

    /**
     * 执行服务，包括两个过程
     * 1. 发布服务图
     * 2. 翻译服务图，然后提交到slurm去执行服务任务
     *
     * @param graphVO 服务图信息
     * @param serviceVO 服务信息
     * @param serviceJobVO 服务任务信息
     * @param principal 用户登录信息
     * @return ResponseResult，data为null，仅返回状态码等信息
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/service/execute", method = RequestMethod.POST)
    @WebLog(description = "service执行")
    @Transactional
    @ResourceCheck(resourceTypes = {ResourceType.DISK, ResourceType.CPU, ResourceType.GPU, ResourceType.MEMORY,
            ResourceType.SERVICE_RUNNING_NUMBER, ResourceType.SERVICE_TOTAL_NUMBER})
    public ResponseResult serviceExecute(@MultiRequestBody("graph") GraphVO graphVO,
                                         @MultiRequestBody("service") ServiceVO serviceVO,
                                         @MultiRequestBody("serviceJob") ServiceJobVO serviceJobVO,
                                         @MultiRequestBody("initJobId") long initJobId,
                                         Principal principal) {
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
            // 发布服务图
            graphService.releaseGraphVersion(graphVO);
            // 翻译、执行服务图
            serviceService.executeService(graphVO, serviceVO, serviceJobVO, principal.getName());
            //若job为复用job，删除原来的job
            Optional<ServiceJob> optional = serviceJobDAO.findById(initJobId);
            if(optional.isPresent())
                serviceService.deleteServiceJob(initJobId, principal.getName());
        }
        catch (ReleaseGraphException e){
            logger.error("ReleaseGraphException!! error={}",e);
            return new ResponseResult(ResponseCode.RELEASE_GRAPH_ERROR.getCode(), ResponseCode.RELEASE_GRAPH_ERROR.getMsg(), null);
        }
        catch (NoSlurmAccountException e) {
            logger.error("NoSlurmAccountException!! error={}",e);
            return new ResponseResult(ResponseCode.NO_SLURM_ACCOUNT_ERROR.getCode(), ResponseCode.NO_SLURM_ACCOUNT_ERROR.getMsg(), null);
        }
        catch (EntityNotFoundException e){
            logger.error("EntityNotFoundException!! error= {}",e);
            return new ResponseResult(ResponseCode.NODE_NOT_EXIST_ERROR.getCode(), ResponseCode.NODE_NOT_EXIST_ERROR.getMsg(),null);
        }
        catch (Exception e) {
            logger.error("serviceExecute fail, error: {}",e);
            return new ResponseResult(ResponseCode.RUN_ERROR.getCode(), ResponseCode.RUN_ERROR.getMsg(), null);
        }
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
    }

    /**
     * 获取serviceJob的详细信息，包括serviceJob任务信息和它用到的service服务组件信息
     * @param serviceJobId ServiceJob任务的id
     * @return Map<String, Object>类型，格式为：{"serviceJob":"服务任务的信息：serviceJob","service":"服务组件的信息：serviceVO"}
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/service/serviceDetails", method = RequestMethod.GET)
    @WebLog(description = "service详细信息")
    public Map<String, Object> serviceDetails(@RequestParam Long serviceJobId) {
        return serviceService.getServiceVOAndServiceJobVO(serviceJobId);
    }
}
