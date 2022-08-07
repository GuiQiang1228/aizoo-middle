package aizoo.controller;

import aizoo.annotation.MultiRequestBody;
import aizoo.aspect.WebLog;
import aizoo.common.JobStatus;
import aizoo.repository.ResourceUsageDAO;
import aizoo.response.BaseResponse;
import aizoo.response.ResponseCode;
import aizoo.response.ResponseResult;
import aizoo.service.JobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@BaseResponse
public class JobController {

    @Autowired
    private ResourceUsageDAO resourceUsageDAO;

    @Autowired
    private JobService jobService;

    private static final Logger logger = LoggerFactory.getLogger(JobController.class);
    /**
    * @Description: 查询任务的执行状态是否可以加载job的运行数据
    * @param id: 待查询的jobId
    * @return: java.lang.Boolean 是否可以加载job的运行数据
    */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/job/canLoading", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "查询任务的执行状态是否可以加载job的运行数据")
    public Boolean canLoadingRunData(@RequestParam(value = "jobId") long id){
        // job详情页打开时，判断是否可以加载runData
        String jobStatus = resourceUsageDAO.getJobStatusById(id);
        return JobStatus.needUpdate().contains(JobStatus.valueOf(jobStatus));
    }

    /**
    * @Description: 修改任务的描述
    * @param id: 待修改任务的id
    * @param description: 需要修改的描述信息
    * @param jobType: job作业类型（作业的类别分为（EXPERIMENT_JOB，SERVICE_JOB，APPLICATION，MIRROR_JOB四种））
    * @return: aizoo.response.ResponseResult:包含操作反馈code，message
    */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/job/modify/description", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @WebLog(description = "修改任务的描述")
    public ResponseResult modifyJobDesc(@MultiRequestBody(value = "id") long id, @MultiRequestBody(value = "description") String description,
                                        @MultiRequestBody(value = "type") String jobType){
        logger.info("modify the description of the job, jobId = {}", id);
        jobService.modifyJobDesc(jobType, id, description);
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), null);
    }

    /**
     * @Description: 获取任务的VO
     * @param id: 待获取任务的id
     * @param jobType: job作业类型（作业的类别分为（EXPERIMENT_JOB，SERVICE_JOB，APPLICATION，MIRROR_JOB四种））
     * @return: aizoo.response.ResponseResult:包含操作反馈code，message
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/job/getVO", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "获取任务的VO")
    public ResponseResult getJobVO(long id, String jobType){
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), jobService.getJobVO(jobType, id));
    }
}
