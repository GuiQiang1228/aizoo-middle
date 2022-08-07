package aizoo.controller;


import aizoo.common.JobStatus;
import aizoo.common.exception.IpNotMatchException;
import aizoo.common.exception.NoSlurmAccountException;
import aizoo.domain.SlurmAccount;
import aizoo.domain.User;
import aizoo.repository.*;
import aizoo.response.ResponseCode;
import aizoo.response.ResponseResult;
import aizoo.service.HomeResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import aizoo.aspect.WebLog;

import java.security.Principal;
import java.util.*;

@RestController
public class HomeResourceController {
    @Autowired
    private ExperimentJobDAO experimentJobDAO;

    @Autowired
    private GraphDAO graphDAO;

    @Autowired
    private ServiceJobDAO serviceJobDAO;

    @Autowired
    private ApplicationDAO applicationDAO;

    @Autowired
    private ComponentDAO componentDAO;

    @Autowired
    UserDAO userDAO;
    @Autowired
    private HomeResourceService homeResourceService;

    private static final Logger logger = LoggerFactory.getLogger(HomeResourceController.class);


    /**
     * 该方法用于获取首页的资源使用状态信息
     * @param principal Spring自带security组件中的principal接口
     * @return 返回带有处理结果的ResponseResult，资源信息用map存储
     * 该map中，key为data，值为包含坐标图信息的map
     * 坐标图信息map中有两组值
     * 第一组key为x，值为map（有两组值，第一组key为name，值为GPU节点，第二组key为data，值为包含节点的list)
     * 第二组key为y,值为存放了3个map的list,每个map都包含两组数据，key分别为name和data（CPU、GPU、内存信息）
     * @throws Exception
     */
    @RequestMapping(value = "/api/index/resourceUsedStatus", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "获取首页的资源使用状态信息")
    public ResponseResult getResourceUsedStatus(Principal principal) throws Exception {
        //在运行过程中，Spring会将Username、Password、Authentication、Token注入到Principal接口中，可以直接在controller获取使用
        String username = principal.getName();
        logger.info("getResourceUsedStatus```````username={}", principal.getName());
        User user = userDAO.findByUsername(username);
        logger.info("getResourceUsedStatus```````userid={}", user.getId());
        //获取该用户的slurm账户
        SlurmAccount slurmAccount = user.getSlurmAccount();
        try {
            //正常情况下
            Map<String, Object> rst = homeResourceService.getClusterResourceUsedInfo(slurmAccount);
            return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), rst);
        }catch (NoSlurmAccountException e){
            logger.error("getResourceUsedStatus fail, NoSlurmAccountException error={}",e);
            //slurm账户为空的情况
            return new ResponseResult(ResponseCode.SLURM_ACCOUNT_NULL.getCode(), ResponseCode.SLURM_ACCOUNT_NULL.getMsg(), null);
        } catch (IpNotMatchException e) {
            logger.error("getResourceUsedStatus fail, IpNotMatchException error={}",e);
            //IP不匹配的情况
            return new ResponseResult(ResponseCode.IP_NOT_MATCH.getCode(), ResponseCode.IP_NOT_MATCH.getMsg(), null);
        }
    }

    /**
     * 该方法用于获取首页的实验分布的信息
     * @param principal 用户信息
     * @return 近五个月每个月用户的实验成功次数和实验失败次数
     * 返回map，key为data,值为包含坐标轴信息的map
     * map中第一个key是x，值为月份
     * map中第二个key是y,值为包含两个map的list
     * 该list中第一个map有两组数据，第一组key为name，值为“实验成功次数”，第二组key为value，值为具体次数。
     * 该list中第二个map有两组数据，第一组key为name，值为“实验失败次数”，第二组key为value，值为具体次数。
     */
    @RequestMapping(value = "/api/index/taskScheduleStatus", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "获取首页的实验分布的信息")
    public Map<String, Object> getTaskScheduleStatus(Principal principal) {
        Map<String, Object> rst = homeResourceService.getScheduleJobsInfo(principal.getName());
        return rst;
    }

    /**
     * 该方法用于获取集群的实验成功和失败的数量
     * @return key为data，值为包含两个map的list。
     * 返回的list中第一个map存放两组值，一组key为name，value为“存放成功次数”，另一组key为value,值为具体数值
     * 返回的list中第二个map存放两组值，一组key为name，value为“存放失败次数”，另一组key为value,值为具体数值
     */
    @RequestMapping(value = "/api/index/taskScheduleNumber", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "获取集群的实验成功和失败的数量")
    public Map<String, List<Object>> getTaskScheduleNumber() {
        int countSuccess = experimentJobDAO.countByJobStatus(JobStatus.COMPLETED);
        int countFailure = experimentJobDAO.countByJobStatus(JobStatus.FAILED);
        Map<String, List<Object>> rst = new HashMap<String, List<Object>>();
        //data为包含两个map的list
        List<Object> data = new ArrayList<>();
        //m1存放两组值，一组指明是存放成功次数，另一组指名具体值
        Map<String, Object> m1 = new HashMap<String, Object>() {
            {
                put("name", "实验成功次数");
                put("value", countSuccess);
            }
        };
        //m2存放两组值，一组指明是存放失败次数，另一组指名具体值
        Map<String, Object> m2 = new HashMap<String, Object>() {
            {
                put("name", "实验失败次数");
                put("value", countFailure);
            }
        };
        data.add(m1);
        data.add(m2);
        rst.put("data", data);
        return rst;
    }

    /**
     * 该方法用于获取用户的实验成功和失败的数量
     * @param principal 用户信息
     * @return key为data，值为包含两个map的list
     * 返回的list中第一个map存放两组值，一组key为name，value为“实验成功次数”，另一组key为value,值为具体数值
     * 返回的list中第二个map存放两组值，一组key为name，value为“实验失败次数”，另一组key为value,值为具体数值
     */
    @RequestMapping(value = "/api/index/userTaskScheduleNumber", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "获取用户的实验成功和失败的数量")
    public Map<String, List<Object>> getUserTaskScheduleNumber(Principal principal) {
        String username = principal.getName();
        int countSuccess = experimentJobDAO.countByJobStatusAndUserUsername(JobStatus.COMPLETED, username);
        int countFailure = experimentJobDAO.countByJobStatusAndUserUsername(JobStatus.FAILED, username);
        Map<String, List<Object>> rst = new HashMap<>();
        //data为包含两个map的list
        List<Object> data = new ArrayList<>();
        //m1存放两组值，一组指明是实验成功次数，另一组指名具体值
        Map<String, Object> m1 = new HashMap<String, Object>() {
            {
                put("name", "实验成功次数");
                put("value", countSuccess);
            }
        };
        //m2存放两组值，一组指明是实验失败次数，另一组指名具体值
        Map<String, Object> m2 = new HashMap<String, Object>() {
            {
                put("name", "实验失败次数");
                put("value", countFailure);
            }
        };
        data.add(m1);
        data.add(m2);
        rst.put("data", data);
        return rst;
    }

    /**
     * 该方法用于获取我的实验和总实验的数量信息
     * @param principal 用户信息
     * @return 返回值为json类型的字符串，属性名是data,值为包括两个对象的list
     * list中的每个对象有name和value两个属性，name指名是总实验次数还是我的实验次数，value是具体数值
     */
    @RequestMapping(value = "/api/index/taskScheduleRatio", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "获取我的实验和总实验的数量信息")
    public String getTaskScheduleRatio(Principal principal) {
        //根据用户名获取我的实验数量
        int countMyJob = experimentJobDAO.countByUserUsername(principal.getName());
        //获取所有实验数量
        int countAllJob = (int) experimentJobDAO.count();
        return "{\n" +
                "      \"data\":[\n" +
                "          {\n" +
                "              \"value\": " + countAllJob + ",\n" +
                "              \"name\": \"总实验次数\"\n" +
                "          },\n" +
                "          {\n" +
                "              \"value\": " + countMyJob + ",\n" +
                "              \"name\": \"我的实验次数\"\n" +
                "          }\n" +
                "      ]\n" +
                "  }";
    }

    /**
     * 该方法用于获取首页图/实验/服务/应用/资源数量信息
     * @param principal 用户信息
     * @return  返回值为包含所需信息的map，key包含graph/job/jobRun/service/serviceRun/application/applicationRun/resource
     */
    @RequestMapping(value = "/api/index/totalNumber", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "获取首页图/实验/服务/应用/资源数量信息")
    public ResponseResult getTotalNumber(Principal principal) {
        Map<String, Integer> result = new HashMap<>();
        String username = principal.getName();
        //1.通过dao层获取所需数据
        logger.info("通过dao层获取所需数据");
        Integer graphTotalNum = graphDAO.countByUserUsername(username);
        Integer jobTotalNum = experimentJobDAO.countByUserUsername(username);
        Integer runningJobNum = experimentJobDAO.countByJobStatusAndUserUsername(JobStatus.RUNNING, username);
        Integer serviceTotalNUm = serviceJobDAO.countByUserUsername(username);
        Integer runningServiceNum = serviceJobDAO.countByJobStatusAndUserUsername(JobStatus.RUNNING, username);
        Integer appTotalNum = applicationDAO.countByUserUsername(username);
        Integer runningAppNum = Integer.valueOf(0);   //先写死
        Integer componentTotalNum = componentDAO.countByUserUsernameAndReleased(username, false);
        //2.加入返回的map中
        logger.info("加入返回的map中");
        result.put("graph", graphTotalNum);
        result.put("job", jobTotalNum);
        result.put("jobRun", runningJobNum);
        result.put("service", serviceTotalNUm);
        result.put("serviceRun", runningServiceNum);
        result.put("application", appTotalNum);
        result.put("applicationRun", runningAppNum);
        result.put("resource", componentTotalNum);
        return new ResponseResult(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMsg(), result);
    }
}
