package aizoo.controller;

import aizoo.Client;
import aizoo.annotation.MultiRequestBody;
import aizoo.aspect.ResourceCheck;
import aizoo.common.JobStatus;
import aizoo.common.Node;
import aizoo.common.ResourceType;
import aizoo.common.exception.NoSlurmAccountException;
import aizoo.common.exception.ReleaseGraphException;
import aizoo.domain.Component;
import aizoo.domain.Datasource;
import aizoo.domain.ExperimentJob;
import aizoo.repository.*;
import aizoo.response.BaseResponse;
import aizoo.response.ResponseCode;
import aizoo.response.ResponseResult;
import aizoo.service.GraphService;
import aizoo.utils.ListEntity2ListVO;
import aizoo.viewObject.mapper.ExperimentJobVOEntityMapper;
import aizoo.viewObject.object.ExperimentJobCheckpointVO;
import aizoo.viewObject.object.GraphVO;
import aizoo.viewObject.object.ExperimentJobVO;
import aizoo.viewObject.pagebleUtils.VO2EntityMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import aizoo.aspect.WebLog;
import aizoo.service.ExperimentService;
import aizoo.service.SlurmService;

import javax.persistence.EntityNotFoundException;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

@BaseResponse
@RestController
public class ExperimentController {

    @Autowired
    ExperimentService experimentService;

    @Autowired
    SlurmService slurmService;

    @Autowired
    GraphService graphService;

    @Autowired
    ExperimentJobDAO experimentJobDAO;

    @Autowired
    GraphDAO graphDAO;

    @Autowired
    Client client;

    @Autowired
    private ComponentDAO componentDAO;

    @Autowired
    DatasourceDAO datasourceDAO;

    @Autowired
    ServiceDAO serviceDAO;

    private static final Logger logger = LoggerFactory.getLogger(ExperimentController.class);

    /**
     * 从slurm中获取当前job的状态
     * 如果与数据库中的不一致则返回false，并更新数据库
     * 一致则终止任务并查询slurm更新数据库并返回true
     *
     * @param jobId     指定实验的id
     * @param principal 当前用户的信息
     * @return 返回带有处理结果的ResponseResult, 具体信息为布尔类型的变量
     * @throws Exception
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/job/stop", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "终止指定实验")
    public ResponseResult stopJob(@MultiRequestBody long jobId, Principal principal) throws Exception {
        boolean result = false;
        try {
            //1.根据jobid找到数据库中对应实验的信息
            ExperimentJob experimentJob = experimentJobDAO.findById(jobId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(jobId)));
            //2.实验对应用户名称与当前用户名是否一致，不一致则无权限终止
            if (!principal.getName().equals(experimentJob.getUser().getUsername())) {
                return new ResponseResult(ResponseCode.JOBSTOP_OUT_OF_BOUNDS.getCode(), ResponseCode.JOBSTOP_OUT_OF_BOUNDS.getMsg(), result);
            }
            logger.info("stop job successfully, jobId = {}", jobId);
            //一致则终止指定实验
            result = experimentService.slurmStopExperimentJob(jobId);
        } catch (JsonProcessingException e) {
            logger.error("终止实验失败: jobId = {}, 错误: {}", jobId, e.getMessage());
            logger.error("End stop Job");
        }
        //3.返回带有处理结果的ResponseResult
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), result);
    }

    /**
     * 删除指定的实验
     *
     * @param jobId     指定实验的id
     * @param principal 当前用户信息
     * @return 返回带有处理结果的ResponseResult
     * @throws Exception
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/job/delete", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "删除指定实验")
    public ResponseResult removeJob(@MultiRequestBody long jobId, Principal principal) throws Exception {
        //1.根据job id从数据库中查找指定实验信息
        ExperimentJob experimentJob = experimentJobDAO.findById(jobId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(jobId)));
        //2.判断当前用户与指定实验的用户是否一致，不一致则无权限删除
        if (!principal.getName().equals(experimentJob.getUser().getUsername())) {
            return new ResponseResult(ResponseCode.DELETE_OUT_OF_BOUNDS.getCode(), ResponseCode.DELETE_OUT_OF_BOUNDS.getMsg(), null);
        }
        //删除指定实验
        experimentService.removeExperimentJobById(jobId);
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), "删除成功", null);
    }

    /**
     * 利用springboot自带分页功能进行分页搜索
     * 参数为搜索需要的信息
     *
     * @param jobName         实验名称
     * @param desc            实验描述
     * @param jobStatus       实验状态
     * @param graphName       图像名称
     * @param startUpdateTime 开始更新时间
     * @param endUpdateTime   结束更新时间
     * @param pageNum         当前页号
     * @param pageSize        每页有几条记录
     * @param principal       用户信息
     * @return 返回查询到的转换格式后的Page对象
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/job/searchPage", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "万能分页experiment搜索")
    public Page<ExperimentJobVO> searchPage(@RequestParam(value = "jobName", required = false, defaultValue = "") String jobName,
                                            @RequestParam(value = "desc", required = false, defaultValue = "") String desc,
                                            @RequestParam(value = "jobStatus", required = false, defaultValue = "") String jobStatus,
                                            @RequestParam(value = "graphName", required = false, defaultValue = "") String graphName,
                                            @RequestParam(value = "startUpdateTime", required = false, defaultValue = "") String startUpdateTime,
                                            @RequestParam(value = "endUpdateTime", required = false, defaultValue = "") String endUpdateTime,
                                            @RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize, Principal principal) {
        String userName = principal.getName();
        //不带排序的pageable对象
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        //加入了pageable对象的查询语句，通过该pageable对象分析生成一个带分页查询的sql语句，返回存储JPA查询数据库的结果集（jobsPage)
        Page<ExperimentJob> jobsPage = experimentJobDAO.searchJob(jobName, desc, jobStatus, graphName, startUpdateTime, endUpdateTime, userName, pageable);
        //利用jpa中的page.map方法转换jobsPage的内部对象(转换为JobVO)
        //第一个参数利用双冒号::简化方法引用，实际是调用job2JobVO方法
        return VO2EntityMapper.mapEntityPage2VOPage(ExperimentJobVOEntityMapper.MAPPER::job2JobVO, jobsPage);
    }

    /**
     * 该方法用于返回执行中的实验信息
     *
     * @param graphVO         前端传来的图片信息
     * @param experimentJobVO 前端传来的实验信息
     * @param principal       用户信息
     * @return 返回包含执行结果的ResponseResult
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/design/execute", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "执行experiment")
    @ResourceCheck(resourceTypes = {ResourceType.DISK, ResourceType.CPU, ResourceType.GPU, ResourceType.MEMORY,
            ResourceType.EXPERIMENT_RUNNING_NUMBER, ResourceType.EXPERIMENT_TOTAL_NUMBER})
    public ResponseResult experimentExecute(@MultiRequestBody("graph") GraphVO graphVO,
                                            @MultiRequestBody("experimentJob") ExperimentJobVO experimentJobVO,
                                            @MultiRequestBody("initJobId") long initJobId, Principal principal) {
        try {
            //检查图是否存在
            if (!graphService.checkGraphExist(graphVO.getId()))
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
            //发布graph
            graphService.releaseGraphVersion(graphVO);
            //根据graph、实验、用户信息执行实验
            experimentService.executeExperiment(graphVO, experimentJobVO, principal.getName());
            logger.info("发布graph，并根据graph执行实验");
            //若job为复用job，删除原来的job
            Optional<ExperimentJob> optional = experimentJobDAO.findById(initJobId);
            if(optional.isPresent())
                experimentService.removeExperimentJobById(initJobId);
        } catch (ReleaseGraphException e) {
            //发布图像失败
            logger.error("release graph failed, GraphType = {}, GraphKey = {}, 错误:{}", graphVO.getGraphType(), graphVO.getGraphKey(), e.getMessage());
            return new ResponseResult(ResponseCode.RELEASE_GRAPH_ERROR.getCode(), ResponseCode.RELEASE_GRAPH_ERROR.getMsg(), null);
        } catch (NoSlurmAccountException e) {
            //查找slurm账户失败
            logger.error("查询项目中gpu服务器失败, 没有Slurm信息。username = {}, 错误：{}", principal.getName(), e.getMessage());
            return new ResponseResult(ResponseCode.NO_SLURM_ACCOUNT_ERROR.getCode(), ResponseCode.NO_SLURM_ACCOUNT_ERROR.getMsg(), null);
        } catch (EntityNotFoundException e){
            logger.error("EntityNotFoundException!! error= {}",e);
            return new ResponseResult(ResponseCode.NODE_NOT_EXIST_ERROR.getCode(), ResponseCode.NODE_NOT_EXIST_ERROR.getMsg(),null);
        } catch (Exception e) {
            //运行失败
            logger.error("运行失败, 错误:{}", e.getMessage());
            logger.error("运行失败, 异常", e);
            return new ResponseResult(ResponseCode.RUN_ERROR.getCode(), ResponseCode.RUN_ERROR.getMsg(), null);
        }
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
    }

    /**
     * 该方法用于返回该模型对应的运行完成的job的相关信息
     *
     * @param id        模型的id
     * @param principal 用户信息
     * @return 包含ExperimentJobCheckpointVO的list
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/design/getModelCheckpoint", method = RequestMethod.GET)
    @WebLog(description = "返回该模型对应的运行完成的job的相关信息")
    public List<ExperimentJobCheckpointVO> getCheckPoint(@RequestParam Long id, Principal principal) {
        return ListEntity2ListVO.jobCheckpoint2JobCheckpointVO(experimentJobDAO.findByComponentIdAndUserUsernameAndJobStatus(id, principal.getName(), JobStatus.COMPLETED));
    }
}
