package aizoo.aspect;

import aizoo.common.ResourceType;
import aizoo.domain.*;
import aizoo.repository.*;
import aizoo.response.ResponseCode;
import aizoo.response.ResponseResult;
import aizoo.service.ResourceUsageService;
import aizoo.viewObject.object.ApplicationVO;
import aizoo.viewObject.object.ExperimentJobVO;
import aizoo.viewObject.object.MirrorJobVO;
import aizoo.viewObject.object.ServiceJobVO;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.security.Principal;
import java.text.DecimalFormat;
import java.util.*;

@Aspect
@Component
public class ResourceRequestAspect {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private ResourceUsageDAO resourceUsageDAO;

    @Autowired
    private ExperimentJobDAO experimentJobDAO;

    @Autowired
    private ServiceJobDAO serviceJobDAO;

    @Autowired
    private ApplicationDAO applicationDAO;

    @Autowired
    ResourceUsageService resourceUsageService;

    @Autowired
    MirrorJobDAO mirrorJobDAO;

    private final static Logger logger = LoggerFactory.getLogger(ResourceRequestAspect.class);

    /** 以自定义 @ResourceCheck 注解为切点 */
    @Pointcut("@annotation(aizoo.aspect.ResourceCheck)")
    public void ResourceCheck() {}

    @Around("ResourceCheck()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        logger.info("Start ResourceCheck");
        logger.info("joinPoint: {}",joinPoint.toString());
        Object[] args = joinPoint.getArgs();
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Parameter[] parameters = method.getParameters();
        ResourceType[] resourceTypes = method.getAnnotation(ResourceCheck.class).resourceTypes();

        Object checkResult = getCheckResult(Arrays.asList(resourceTypes), parameters, args);
        if(checkResult != null){
            logger.info("结果: {}",checkResult.toString());
            logger.info("End do Around");
            return checkResult;
        }

        Object result = joinPoint.proceed();
        logger.info("结果: {}",result.toString());
        logger.info("End ResourceCheck");
        return result;
    }

    public Object getCheckResult(List<ResourceType> resourceTypes, Parameter[] parameters, Object[] args) {
        logger.info("Start get Check Result");
        logger.info("resourceTypes: {},parameters: {},args: {}",resourceTypes.toString(),parameters.toString(),args.toString());
        String username = null;
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getName().equals("principal")) {
                Principal principal = (Principal) args[i];
                username = principal.getName();
            }
        }
        //处理参数没传principal的情况
        if(username == null){
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if(!(authentication instanceof AnonymousAuthenticationToken)){
                username = authentication.getName();
            }
        }
        logger.info(username);

        if(username == null){
            logger.error("无法获取用户信息，资源检查失败");
            return null;
        }
        User user = userDAO.findByUsername(username);
        Level level = user.getLevel();
        logger.info("成功获取用户的level:{}",level);

        StringBuilder message = new StringBuilder("");

        if (resourceTypes.contains(ResourceType.DISK)) {
            //更新硬盘使用情况
            logger.info("开始更新硬盘的使用情况");
//            resourceUsageService.updateDiskCapacity(username);

            DecimalFormat df = new DecimalFormat("#.00");
            double fileSize = 0;
            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i].getName().equals("fileSize")) {
                    fileSize = Double.parseDouble(df.format((double) args[i]));  //仅保留两位小数
                    logger.info("fileSize: {}",fileSize);
                    break;
                }
            }
            Double diskUsage = resourceUsageDAO.getAllTypeResourceUsage(ResourceType.DISK.toString(), username);
            logger.info("diskUsage: {}",diskUsage);
            if (level.getDisk() < diskUsage + fileSize)
                message.append("、硬盘空间");
        }

        if (resourceTypes.contains(ResourceType.CPU) || resourceTypes.contains(ResourceType.GPU) || resourceTypes.contains(ResourceType.MEMORY)) {
            Map<String, Object> slurmArgs = getSlurmArgsOfJob(parameters, args);
            if (resourceTypes.contains(ResourceType.CPU)) {
                int cpuNums = Integer.valueOf((String) slurmArgs.get("cpuspertask"));
                double cpuUsage = resourceUsageDAO.getAllTypeResourceUsage(ResourceType.CPU.toString(), username);
                if (level.getCPU() < cpuNums + cpuUsage)
                    message.append("、CPU数量");
            }

            if (resourceTypes.contains(ResourceType.GPU)) {
                String gres = (String) slurmArgs.get("gres");  // 得到的结果如: "gpu:4"
                int gpuNums = Integer.valueOf(gres.substring(gres.lastIndexOf(':')+1));
                double gpuUsage = resourceUsageDAO.getAllTypeResourceUsage(ResourceType.GPU.toString(), username);
                if (level.getCPU() < gpuNums + gpuUsage)
                    message.append("、GPU节点数量");
            }

            if (resourceTypes.contains(ResourceType.MEMORY)) {
                double mem = Double.valueOf((String) slurmArgs.get("mem"));
                double memUsage = resourceUsageDAO.getAllTypeResourceUsage(ResourceType.MEMORY.toString(), username);
                if (level.getMemory() < mem + memUsage)
                    message.append("、内存使用容量");
            }
        }

        if (resourceTypes.containsAll(Arrays.asList(ResourceType.EXPERIMENT_RUNNING_NUMBER, ResourceType.EXPERIMENT_TOTAL_NUMBER))) {
            // 验证正在运行的实验数量是否超出限制
            double runningExpNum = resourceUsageDAO.getAllTypeResourceUsage(ResourceType.EXPERIMENT_RUNNING_NUMBER.toString(), username);
            if (level.getExperimentMaxRunningNum() < runningExpNum + 1)
                message.append("、正在运行实验数量");
            int totalExpNum = experimentJobDAO.countByUserUsername(username);
            if (level.getExperimentTotalNum() < totalExpNum + 1)
                message.append("、总实验数量");
        }

        if (resourceTypes.containsAll(Arrays.asList(ResourceType.SERVICE_RUNNING_NUMBER, ResourceType.SERVICE_TOTAL_NUMBER))) {
            // 验证正在运行的实验数量是否超出限制
            double runningServiceNum = resourceUsageDAO.getAllTypeResourceUsage(ResourceType.SERVICE_RUNNING_NUMBER.toString(), username);
            if (level.getServiceMaxRunningNum() < runningServiceNum + 1)
                message.append("、正在运行服务数量");
            int totalServiceNum = serviceJobDAO.countByUserUsername(username);
            if (level.getServiceTotalNum() < totalServiceNum + 1)
                message.append("、总服务数量");
        }

        if (resourceTypes.containsAll(Arrays.asList(ResourceType.APPLICATION_RUNNING_NUMBER, ResourceType.APPLICATION_TOTAL_NUMBER))) {
            // 验证正在运行的实验数量是否超出限制
            double runningAppNum = resourceUsageDAO.getAllTypeResourceUsage(ResourceType.APPLICATION_RUNNING_NUMBER.toString(), username);
            if (level.getAppMaxRunningNum() < runningAppNum + 1)
                message.append("、正在运行应用数量");
            int totalAppNum = applicationDAO.countByUserUsername(username);
            if (level.getAppTotalNum() < totalAppNum + 1)
                message.append("、总应用数量");
        }

        if (resourceTypes.containsAll(Arrays.asList(ResourceType.MIRROR_JOB_RUNNING_NUMBER, ResourceType.MIRROR_JOB_TOTAL_NUMBER))) {
            // 验证正在运行的镜像数量是否超出限制
            double runningMirrorJobNum = resourceUsageDAO.getAllTypeResourceUsage(ResourceType.MIRROR_JOB_RUNNING_NUMBER.toString(), username);
            if (level.getMirrorMaxRunningNum() < runningMirrorJobNum + 1)
                message.append("、正在运行镜像实验数量");
            int totalMirrorJobNum = mirrorJobDAO.countByUserUsername(username);
            if (level.getMirrorTotalNum() < totalMirrorJobNum + 1)
                message.append("、总镜像实验数量");
        }

        if (!message.toString().equals("")) {
            message.deleteCharAt(0);
            message.append("将超出限制，请扩容");
            logger.info("验证资源反馈code: {},反馈消息: {}",ResponseCode.RESOURCE_CHECK_ERROR.getCode(),message.toString());
            logger.info("End get Check Result");
            return new ResponseResult(ResponseCode.RESOURCE_CHECK_ERROR.getCode(), message.toString(), null);
        }
        logger.info("反馈结果: null");
        logger.info("End get Check Result");
        return null;
    }

    public Map<String, Object> getSlurmArgsOfJob( Parameter[] parameters, Object[] args){
        logger.info("Start get SlurmArgs Of Job");
        logger.info("parameters: {},args: {}",parameters.toString(),args.toString());
        Map<String, Map<String,Object>> environment = new HashMap<>();
        for (int i = 0; i < parameters.length; i++) {
            String parameterName = parameters[i].getName();
            if(parameterName.equals("experimentJobVO")){
                ExperimentJobVO job = (ExperimentJobVO)args[i];
                environment = job.getEnvironment();
                break;
            }
            else if(parameterName.equals("serviceJobVO")){
                ServiceJobVO job = (ServiceJobVO)args[i];
                environment = job.getEnvironment();
                break;
            }
            else if(parameterName.equals("appVO")){
                ApplicationVO job = (ApplicationVO)args[i];
                environment = job.getEnvironment();
                break;
            }
            else if(parameterName.equals("mirrorJobVO")){
                MirrorJobVO job = (MirrorJobVO) args[i];
                environment = job.getEnvironment();
                break;
            }
        }
        logger.info("得到的SlurmArgsOfJob是: {}",environment.get("slurmKwargs").toString());
        logger.info("End get SlurmArgs Of Job");
        return environment.get("slurmKwargs");
    }
}
