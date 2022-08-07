package aizoo.service;

import aizoo.domain.SlurmAccount;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import aizoo.Client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service("SlurmService")
public class SlurmService {
    /**
     * 整合了Slurm常用操作：启动Slurm、停止Slurm、获取node节点信息、获取node节点信息
     */

    private final static Logger logger = LoggerFactory.getLogger(SlurmService.class);

    private final Client client;

    @Autowired
    public SlurmService(Client client) {
        this.client = client;
    }

    @Autowired
    ObjectMapper objectMapper;

    /**
     * 启动Slurm
     * @param runArgs Slurm相关参数
     * @param slurmAccount Slurm账户
     * @return 启动信息
     * @throws Exception
     */
    public String startExperimentJob(String runArgs, SlurmAccount slurmAccount) throws Exception {
        logger.info("start");
        return client.experimentJobStart(runArgs, slurmAccount);
    }

    /**
     * 停止Slurm
     * @param jobKey
     * @param slurmAccount
     * @return 响应信息
     * @throws Exception
     */
    public String stop(String jobKey,SlurmAccount slurmAccount) throws Exception{
        logger.info("stop");
        return client.slurmStop(jobKey, slurmAccount);
    }

    /**
     * 获取node节点信息
     * @param slurmAccount
     * @return node节点相应信息
     * @throws Exception
     */
    public String getNodeList(SlurmAccount slurmAccount) throws Exception{
        logger.info("getNodeList");
        return client.slurmGetNodeList(slurmAccount);
    }

    /**
     * 获取job状态
     * @param jobKey
     * @param slurmAccount
     * @return Map<String, Object>, 内容为 {jobKey: job相关信息}
     * @throws Exception
     */
    public String showJob(String jobKey, SlurmAccount slurmAccount) throws Exception{
        logger.info("showJob");
        return client.slurmShowJob(jobKey, slurmAccount);
    }

    /**
     * 提交MirrorJob
     * @param runArgs Slurm相关参数
     * @param slurmAccount Slurm账户
     * @return 启动信息
     * @throws Exception
     */
    public String startMirrorJob(String runArgs, String userArgs, SlurmAccount slurmAccount) throws Exception {
        logger.info("mirror Job start");
        return client.mirrorJobStart(runArgs, userArgs, slurmAccount);
    }

}
