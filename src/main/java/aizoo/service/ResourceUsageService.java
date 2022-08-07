package aizoo.service;

import aizoo.common.ResourceType;
import aizoo.domain.ResourceUsage;
import aizoo.domain.User;
import aizoo.repository.ResourceUsageDAO;
import aizoo.repository.UserDAO;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ResourceUsageService {

    private final static Logger logger = LoggerFactory.getLogger(ResourceUsageService.class);

    @Value("${download.dir}")
    String downloadDir;

    @Autowired
    ResourceUsageDAO resourceUsageDAO;

    @Autowired
    UserDAO userDAO;

    @Value("${file.path}")
    private String filePath;

    /**
     * 资源使用情况
     * @param slurmArgs slurm集群
     * @param jobId 任务id
     * @param user 用户
     */
    public void saveResourceUsageOfJob(Map<String, Object> slurmArgs, Long jobId, User user) {
        logger.info("Start save Resource Usage Of Job");
        logger.info("saveResourceUsageOfJob slurmArgs:{},jobId:{},user:{}", slurmArgs, jobId, user);
        //1、获取CPU,GPU的数量
        double cpuNum = Double.valueOf((String) slurmArgs.get("cpuspertask"));
        String gres = (String) slurmArgs.get("gres");
        double gpuNum = Double.valueOf(gres.substring(gres.lastIndexOf(':') + 1));
        double mem = Double.valueOf((String) slurmArgs.get("mem"));
        //2、获取CPU,GPU的使用情况
        ResourceUsage cpuUsage = new ResourceUsage(ResourceType.CPU, cpuNum, jobId, user);
        resourceUsageDAO.save(cpuUsage);
        ResourceUsage gpuUsage = new ResourceUsage(ResourceType.GPU, gpuNum, jobId, user);
        resourceUsageDAO.save(gpuUsage);
        ResourceUsage memUsage = new ResourceUsage(ResourceType.MEMORY, mem, jobId, user);
        resourceUsageDAO.save(memUsage);
        logger.info("End save Resource Usage Of Job");
    }

    /**
     * 更新磁盘容量
     * @param username 用户名
     * @throws URISyntaxException
     */
    public void updateDiskCapacity(String username) throws URISyntaxException {
        logger.info("Start update Disk Capacity");
        logger.info("updateDiskCapacity username:{}", username);
        synchronized (this) {
            DecimalFormat df = new DecimalFormat("#.00");
            // 1、用户目录下，查看目录是否存在，不存在就创建目录
            File userDir = new File(Paths.get(filePath, username).toString());
            if (!userDir.exists())
                userDir.mkdirs();
            double userDirSize = FileUtils.sizeOfDirectory(userDir) / (1024 * 1024);
            userDirSize = Double.parseDouble(df.format(userDirSize));     //保留两位小数

            // 2、下载目录
            File staticDir = new File(Paths.get(downloadDir, username).toString());
            if (!staticDir.exists())
                staticDir.mkdirs();
            double staticDirSize = FileUtils.sizeOfDirectory(staticDir) / (1024 * 1024);
            staticDirSize = Double.parseDouble(df.format(staticDirSize));  //保留两位小数

            // 3、更新数据库
            ResourceUsage diskUsage = resourceUsageDAO.findByResourceTypeAndUserUsername(ResourceType.DISK, username);
            if (diskUsage == null)
                diskUsage = new ResourceUsage(ResourceType.DISK, userDAO.findByUsername(username));
            diskUsage.setUsedAmount(userDirSize + staticDirSize);
            resourceUsageDAO.save(diskUsage);
        }
        logger.info("End update Disk Capacity");
    }
}
