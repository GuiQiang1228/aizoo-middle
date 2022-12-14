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
     * ????????????????????????ServiceJob?????????????????????????????????
     *
     * @param serviceName serviceJob??????
     * @param desc serviceJob????????????
     * @param serviceStatus serviceJob??????
     * @param graphName serviceJob?????????
     * @param startUpdateTime ????????????????????????
     * @param endUpdateTime ????????????????????????
     * @param pageNum ??????
     * @param pageSize ????????????
     * @param principal ??????????????????
     * @return Page<ServiceJobVO>?????????????????? ServiceJobVO????????? Page?????????
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/service/searchPage", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "????????????ServiceJob??????")
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
     * ??????????????????????????????Service??????????????????
     * ??????????????????????????????????????????????????????????????????????????????
     *
     * @param principal ??????????????????
     * @return List<ServiceVO>
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/design/service", method = RequestMethod.GET)
    @WebLog(description = "??????????????????????????????????????????Service")
    public List<ServiceVO> getService(Principal principal) {
        return ListEntity2ListVO.service2ServiceVO(serviceDAO.findByUserUsernameAndReleased(principal.getName(), true));
    }

    /**
     * ???????????????????????????id???????????????Service????????????????????? RunningServiceJobVO ??????
     *
     * @param id Service???????????????id
     * @param principal ??????????????????
     * @return List<RunningServiceJobVO>?????????RunningServiceJobVO??????????????????????????????????????????????????? ServiceJob???????????????
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/design/getServiceJobId", method = RequestMethod.GET)
    @WebLog(description = "??????????????????????????????serviceJob")
    public List<RunningServiceJobVO> getServiceJob(@RequestParam Long id, Principal principal) {
        List<ServiceJob> serviceJobs = serviceJobDAO.findByServiceIdAndUserUsernameAndJobStatusAndIpNotNull(id, principal.getName(), JobStatus.RUNNING);
        return ListEntity2ListVO.serviceJob2RunningServiceJobVO(serviceJobs);
    }

    /**
     * ????????????ServiceJob????????????
     * ?????????slurm????????????job??????????????????????????????????????????????????????false?????????????????????
     * ??????????????????????????????slurm????????????????????????true
     *
     * @param serviceJobId ServiceJob?????????id
     * @param principal ??????????????????
     * @return ResponseResult????????? "data"?????????????????? boolean??????????????????false???????????????true????????????
     * @throws Exception
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/service/stop", method = RequestMethod.POST)
    @WebLog(description = "serviceJob??????")
    public ResponseResult serviceStop(@MultiRequestBody long serviceJobId, Principal principal) throws Exception {
        //???slurm???????????????job??????????????????????????????????????????????????????false??????????????????????????????????????????????????????slurm????????????????????????true
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
     * ?????? serviceJob??????????????????????????????out??????????????????????????????project????????????es??????
     *
     * @param serviceJobId ServiceJob?????????id
     * @param principal ??????????????????
     * @return ResponseResult???data???null??????????????????????????????
     * @throws Exception
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/service/delete", method = RequestMethod.POST)
    @WebLog(description = "serviceJob??????")
    public ResponseResult serviceDelete(@MultiRequestBody long serviceJobId, Principal principal) throws Exception {
        boolean flag = serviceService.deleteServiceJob(serviceJobId, principal.getName());
        if(!flag)
            return new ResponseResult(ResponseCode.DELETE_OUT_OF_BOUNDS.getCode(), ResponseCode.DELETE_OUT_OF_BOUNDS.getMsg(), null);
        else
            return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
    }

    /**
     * ?????????????????????????????????
     * 1. ???????????????
     * 2. ?????????????????????????????????slurm?????????????????????
     *
     * @param graphVO ???????????????
     * @param serviceVO ????????????
     * @param serviceJobVO ??????????????????
     * @param principal ??????????????????
     * @return ResponseResult???data???null??????????????????????????????
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/service/execute", method = RequestMethod.POST)
    @WebLog(description = "service??????")
    @Transactional
    @ResourceCheck(resourceTypes = {ResourceType.DISK, ResourceType.CPU, ResourceType.GPU, ResourceType.MEMORY,
            ResourceType.SERVICE_RUNNING_NUMBER, ResourceType.SERVICE_TOTAL_NUMBER})
    public ResponseResult serviceExecute(@MultiRequestBody("graph") GraphVO graphVO,
                                         @MultiRequestBody("service") ServiceVO serviceVO,
                                         @MultiRequestBody("serviceJob") ServiceJobVO serviceJobVO,
                                         @MultiRequestBody("initJobId") long initJobId,
                                         Principal principal) {
        try {
            //?????????????????????
            if(!graphService.checkGraphExist(graphVO.getId()))
                return new ResponseResult(ResponseCode.GRAPH_NOT_EXIST_ERROR.getCode(), ResponseCode.GRAPH_NOT_EXIST_ERROR.getMsg(), null);
            //?????????????????????????????????
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
            // ???????????????
            graphService.releaseGraphVersion(graphVO);
            // ????????????????????????
            serviceService.executeService(graphVO, serviceVO, serviceJobVO, principal.getName());
            //???job?????????job??????????????????job
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
     * ??????serviceJob????????????????????????serviceJob???????????????????????????service??????????????????
     * @param serviceJobId ServiceJob?????????id
     * @return Map<String, Object>?????????????????????{"serviceJob":"????????????????????????serviceJob","service":"????????????????????????serviceVO"}
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/service/serviceDetails", method = RequestMethod.GET)
    @WebLog(description = "service????????????")
    public Map<String, Object> serviceDetails(@RequestParam Long serviceJobId) {
        return serviceService.getServiceVOAndServiceJobVO(serviceJobId);
    }
}
