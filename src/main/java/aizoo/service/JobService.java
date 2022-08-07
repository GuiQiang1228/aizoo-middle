package aizoo.service;

import aizoo.common.JobType;
import aizoo.common.exception.NoSlurmAccountException;
import aizoo.domain.*;
import aizoo.repository.*;
import aizoo.viewObject.mapper.ApplicationVOEntityMapper;
import aizoo.viewObject.mapper.ExperimentJobVOEntityMapper;
import aizoo.viewObject.mapper.MirrorJobVOEntityMapper;
import aizoo.viewObject.mapper.ServiceJobVOEntityMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.persistence.EntityNotFoundException;
import java.util.HashMap;
import java.util.Map;

@Service
public class JobService {

    @Autowired
    private SlurmService slurmService;

    @Autowired
    private ExperimentJobDAO experimentJobDAO;

    @Autowired
    private ServiceJobDAO serviceJobDAO;

    @Autowired
    private ApplicationDAO applicationDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private MirrorJobDAO mirrorJobDAO;

    @Autowired
    private ObjectMapper objectMapper = new ObjectMapper();

    private static final Logger logger = LoggerFactory.getLogger(JobService.class);

    /**
    * @Description: 根据jobKey获取作业状态
     * 需要先从slurm service获取job信息
     * 若slurm service获取不到，则获取数据库中的job信息
    * @param jobKey:   需要获取的作业的jobkey
    * @param jobType:  作业的类别（分为EXPERIMENT_JOB，SERVICE_JOB，APPLICATION三种）
    * @param user:     作业所属的用户信息
    * @return: java.lang.String 作业的状态信息
    * @throws: Exception
    */
    public String getJobStatus(String jobKey, JobType jobType,User user) throws Exception {
        logger.info("Start get JobStatus ");
        logger.info("jobKey: {},jobTye: {}",jobKey,jobType.toString());
        // 有些特殊的job状态，slurm service的返回值，与数据库里可接受的job状态的返回值对应关系
        // 本map的key是slurm service返回的job状态，value是数据库里存的job状态,所有的状态列表，请参考JobStatus.java
        Map<String, String> jobStateMapping = new HashMap<String, String>() {{
            put("NODE FAILURE", "NODE_FAILURE");
            put("SPECIAL EXIT STATE", "SPECIAL_EXIT_STATE");
        }};

        ExperimentJob experimentJob = null;
        ServiceJob serviceJob = null;
        Application app = null;
        MirrorJob mirrorJob = null;
        //1. 获取用户的slurmAccount信息
        SlurmAccount slurmAccount = user.getSlurmAccount();
        if (slurmAccount == null)
            throw new NoSlurmAccountException();
        //2. 根据job的类别获取不同的实例
        if (jobType == JobType.EXPERIMENT_JOB){
            experimentJob = experimentJobDAO.findByJobKey(jobKey);
        } else if (jobType == JobType.SERVICE_JOB){
            serviceJob = serviceJobDAO.findByJobKey(jobKey);
        } else if (jobType == JobType.APPLICATION){
            app = applicationDAO.findByJobKey(jobKey);
        } else if(jobType == JobType.MIRROR_JOB){
            mirrorJob = mirrorJobDAO.findByJobKey(jobKey);
        }
        //3. 根据jobKey和slurmAccount获取slurm service中job信息
        String jobInfo = slurmService.showJob(jobKey,slurmAccount);
        //4. 将json str（jobInfo）解析成map，slurm service查询到的jobInfo，是只有一个key的map，唯一key就是jobKey
        Map<String, Object> map = objectMapper.readValue(jobInfo, new TypeReference<Map<String, Object>>() {});
        String jobState = null;
        if (!map.isEmpty()) {
            //5. 根据jobKey取出对应的jobinfo
            Map<String, Object> result = (Map<String, Object>) map.get(jobKey);
            jobState = (String) result.get("job_state");
            //6. 若获取到的job状态特殊，则转为数据库里可接受的job状态
            if (jobStateMapping.containsKey(jobState))
                jobState = jobStateMapping.get(jobState);
        } else {
            //7. 若未获取到的slurm service中job信息，则从数据库中获取job的状态
            if(jobType == JobType.EXPERIMENT_JOB){
                jobState = experimentJob.getJobStatus().toString();
            }else if (jobType == JobType.SERVICE_JOB){
                jobState = serviceJob.getJobStatus().toString();
            }else if(jobType == JobType.APPLICATION){
                jobState = app.getAppResults().toString();
            } else if(jobType == JobType.MIRROR_JOB){
                jobState = mirrorJob.getJobStatus().toString();
            }
        }
        logger.info("JobStatus: {}",jobState);
        logger.info("End get JobStatus ");
        return jobState;
    }

    /**
    * @Description: 设置/修改作业的描述信息
    * @param jobType: 作业类型（作业的类别（分为EXPERIMENT_JOB，SERVICE_JOB，APPLICATION三种））
    * @param id:      作业的id
    * @param description: 需要设置的job描述
    * @return: void
    */
    public void modifyJobDesc(String jobType, long id, String description){
        logger.info("Start modify Job Description");
        logger.info("jobType: {},description: {}",jobType,description);
        logger.info("findById id: {}",id);
        //1. 将job类别转为枚举类型
        JobType type = JobType.valueOf(jobType);
        //2. 根据job类型更新相应的表中规定id的job的description
        switch (type){
            case APPLICATION:{
                Application application = applicationDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
                application.setDescription(description);
                applicationDAO.save(application);
                logger.info("update the description of Application Job");
                break;
            }
            case SERVICE_JOB:{
                ServiceJob serviceJob = serviceJobDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
                serviceJob.setDescription(description);
                serviceJobDAO.save(serviceJob);
                logger.info("update the description of Service Job");
                break;
            }
            case EXPERIMENT_JOB:{
                ExperimentJob experimentJob = experimentJobDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
                experimentJob.setDescription(description);
                experimentJobDAO.save(experimentJob);
                logger.info("update the description of Experiment Job");
                break;
            }
            case MIRROR_JOB:{
                MirrorJob mirrorJob = mirrorJobDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
                mirrorJob.setDescription(description);
                mirrorJobDAO.save(mirrorJob);
                logger.info("update the description of Mirror Job");
                break;
            }
        }
        logger.info("End modify Job Description");
    }

    /**
     * @Description: 设置/修改作业的描述信息
     * @param jobType: 作业类型（作业的类别（分为EXPERIMENT_JOB，SERVICE_JOB，APPLICATION三种））
     * @param id:      作业的id
     * @return: void
     */
    public Object getJobVO(String jobType, long id){
        //1. 将job类别转为枚举类型
        JobType type = JobType.valueOf(jobType);
        //2. 根据job类型更新相应的表中规定id的job的description
        switch (type){
            case APPLICATION:{
                Application application = applicationDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
                return ApplicationVOEntityMapper.MAPPER.application2ApplicationVO(application);
            }
            case SERVICE_JOB:{
                ServiceJob serviceJob = serviceJobDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
                return ServiceJobVOEntityMapper.MAPPER.serviceJob2ServiceJobVO(serviceJob);
            }
            case EXPERIMENT_JOB:{
                ExperimentJob experimentJob = experimentJobDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
                return ExperimentJobVOEntityMapper.MAPPER.job2JobVO(experimentJob);
            }
            case MIRROR_JOB:{
                MirrorJob mirrorJob = mirrorJobDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
                return MirrorJobVOEntityMapper.MAPPER.job2JobVO(mirrorJob);
            }
        }
        return null;
    }
}
