package aizoo.scheduler;

import aizoo.common.JobStatus;
import aizoo.domain.Application;
import aizoo.domain.ApplicationResult;
import aizoo.repository.ApplicationDAO;
import aizoo.repository.ApplicationResultDAO;
import aizoo.repository.VisualContainerDAO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ApplicationResultScheduler {

    @Value("${download.dir}")
    String downloadDir;

    @Value("${download.url}")
    String downloadUrl;

    @Autowired
    ApplicationDAO applicationDAO;


    @Autowired
    VisualContainerDAO visualContainerDAO;

    @Autowired
    ApplicationResultDAO applicationResultDAO;

    private final static Logger logger = LoggerFactory.getLogger(ApplicationResultScheduler.class);

    private final static List<String> jobs = new CopyOnWriteArrayList<>();

    private ObjectMapper objectMapper = new ObjectMapper();

    private volatile boolean allUnFinishedJobKeyHasAdd = false;

    /**
     * 定时查询是否有已完成但未保存的appResult
     * 将appResult保存至static目录下供用户访问和下载。并保存到数据库中
     */
    @Transactional
    @Scheduled(cron = "*/3 * * * * *")
    public void appResultCheck() throws IOException {
        logger.info("Start check appResult");
        if (jobs != null && allUnFinishedJobKeyHasAdd) {
            List<String> job2Remove = new ArrayList<>();
            for (String key : jobs) {
                try {
                    logger.info("开始: jobKey:{}", key);
                    Application app = applicationDAO.findByJobKey(key);
                    if (app == null) {
                        continue;
                    }
                    if (app.getJobStatus() != JobStatus.RUNNING
                            && app.getJobStatus() != JobStatus.COMPLETING
                            && app.getJobStatus() != JobStatus.CONFIGURING
                            && app.getJobStatus() != JobStatus.PENDING
                            && app.getJobStatus() != JobStatus.RESIZING
                    ) {
                        String targetPath = Paths.get(downloadDir, app.getUser().getUsername(), "app_result", String.valueOf(app.getId())).toString();
                        File targetDir = new File(targetPath);

                        boolean resultExists = resultExistsInDatabase(app);
                        boolean fileExists = fileExistsInFileSystem(app);
                        // 结果和文件都存在，直接跳过
                        if (resultExists && fileExists)
                            continue;
                        // 结果不存在，文件存在，删掉全部文件
                        if (!resultExists && fileExists)
                            deleteRecursive(targetDir);

                        // 如下的操作，只会出现，结果存在，文件不存在，或二者都不存在的情况
                        // 结果存在文件不存在，则只下载文件，产生结果记录
                        // 获取slurm生成job结果的保存目录
                        Map<String, Object> map = objectMapper.readValue(app.getEnvironment(), new TypeReference<Map<String, Object>>() {
                        });
                        Map<String, Object> jobEnv = (Map<String, Object>) map.get(key);

                        // .out文件的目录 {save.path}/{uuid}/runtime_log/xxx.out
                        // xxx/aizoo-back-interpreter/files/out/{uuid}/runtime_log/xxx.out
                        String stdOut = (String) jobEnv.get("std_out");

                        // 复制日志文件到下载目录下
                        // xxx/aizoo-back-interpreter/files/out/{uuid}/runtime_log整个目录到下载目录（连带runtime_log文件夹）
                        copyLogFiles(new File(stdOut.substring(0, stdOut.lastIndexOf("/"))), new File(targetPath));

                        // 把日志文件的绝对路径，添加到app表里去，地址为转换完的url
                        // 这个path会被转换成url，存放根目录在downloadDir下，所以不需要存文件的绝对路径
                        String logDir = Paths.get(targetPath, "runtime_log").toString();
                        for (File logFile : new File(logDir).listFiles()) {
                            String url = downloadUrl + logFile.getAbsolutePath().replace(downloadDir, "");
                            if (logFile.getName().endsWith("out"))
                                app.setOutLogUrl(url);
                            else if (logFile.getName().endsWith("err"))
                                app.setErrorLogUrl(url);
                        }
                        applicationDAO.save(app);


                        // xxx/aizoo-back-interpreter/files/out/{uuid}/ 所有结果、文件、日志等保存的目录
                        // 注意，由于没提供app整个下载，所以目前代码文件没拷到out目录下
                        String savePath = stdOut.substring(0, stdOut.indexOf("runtime_log"));
                        logger.info("savePath: {}", savePath);

                        // xxx/aizoo-back-interpreter/files/out/{uuid}/results 运行结果的保存目录
                        File copyDir = new File(Paths.get(savePath, "results").toString());

                        // 开始拷贝执行结果，即拷贝xxx/aizoo-back-interpreter/files/out/{uuid}/results 里的全部文件
                        logger.info("复制appResult到静态资源路径下,并写入到数据库中");
                        for (File sourceFile : Objects.requireNonNull(copyDir.listFiles())) {
                            if (sourceFile.isFile()) {
                                File targetFile = new File(targetDir.getAbsolutePath() + File.separator + sourceFile.getName());
                                if (targetFile.exists())// 文件已存在，则不再拷第二遍
                                    continue;
                                FileUtils.copyFileToDirectory(sourceFile, targetDir);
                                // 如果结果不存在，则保存到数据库中
                                if (!resultExists) {
                                    String containerName = "FileWriter";  //默认使用的可视化容器是FileWriter
                                    ApplicationResult appResult = new ApplicationResult();
                                    appResult.setApplication(app);
                                    appResult.setName(sourceFile.getName());

                                    appResult.setInputFile(sourceFile.getName());
                                    // 可以通过url获取绝对路径，为避免本地启动时，数据库的path被本地路径污染，故不再存path
//                                    appResult.setPath(targetFile.getAbsolutePath());
                                    appResult.setUrl(downloadUrl + targetFile.getAbsolutePath().replace(downloadDir, ""));
                                    appResult.setVisualContainer(visualContainerDAO.findByName(containerName));
                                    applicationResultDAO.save(appResult);
                                }

                            }
                        }
                        logger.info("已经处理完当前应用的所有appResult");
                        job2Remove.add(key);
                    }
                } catch (Exception exception) {
                    logger.error("appResultCheck出错！key={}, 错误： {}", key, exception);
                    continue;
                }

            }
            jobs.removeAll(job2Remove);
            logger.info("removed application jobs：{}", job2Remove);
            logger.info("current application waiting for result jobs：{}", jobs);
        }
        logger.info("End check appResult");
    }


    /**
     * 复制app执行后的log文件到用户下载目录下
     *
     * @param srcDir    初始目录
     * @param targetDir 目标目录
     * @throws IOException
     */
    private void copyLogFiles(File srcDir, File targetDir) throws IOException {
        if (srcDir.isDirectory())
            FileUtils.copyDirectoryToDirectory(srcDir, targetDir);
    }

    /**
     * 数据库里是否存在某个application的result的记录
     *
     * @param application 应用
     * @return boolean类型, 表示是否包含该应用结果记录
     */
    private boolean resultExistsInDatabase(Application application) {
        return application.getAppResults() != null && application.getAppResults().size() > 0;
    }

    /**
     * 文件系统里是否存在这个application的结果文件夹（不判断每一个结果内容）
     *
     * @param application 应用
     * @return boolean类型, 表示是否存在该应用的结果文件夹
     */
    private boolean fileExistsInFileSystem(Application application) {
        String targetPath = Paths.get(downloadDir, application.getUser().getUsername(), "app_result", String.valueOf(application.getId())).toString();
        File targetFile = new File(targetPath);
        return targetFile.exists();
    }

    /**
     * 递归地删掉整个文件夹以及全部文件
     *
     * @param file 目录或文件
     */
    private void deleteRecursive(File file) {
        if (!file.exists())
            return;
        if (!file.isDirectory())
            file.delete();
        else {
            for (File f : file.listFiles()) {
                deleteRecursive(f);
            }
        }
    }


    /**
     * 宕机重启后，遍历全表状态为complete的application，查询是否将appResult复制完成，未完成加job
     * 可能会出现部分文件已存在但是并不完整，暂不考虑这种情况
     */
    @PostConstruct
    public void checkInit() {
        Set<Application> appList = applicationDAO.findByJobStatus(JobStatus.COMPLETED);
        for (Application app : appList) {
            String targetPath = Paths.get(downloadDir, app.getUser().getUsername(), "app_result", String.valueOf(app.getId())).toString();
            File targetFile = new File(targetPath);
            if (!targetFile.exists()) {
                jobs.add(app.getJobKey());
            }
        }
        allUnFinishedJobKeyHasAdd = true;
    }


    /**
     * 加入到等待更新状态、保存结果的jobs队列中
     *
     * @param jobKey 任务的jobKey
     */
    public static void addJob(String jobKey) {
        logger.info("Start add Job");
        jobs.add(jobKey);
        logger.info("jobKey: {}", jobKey);
        logger.info("End add Job");
    }


    /**
     * 把jobKey从等待复制应用结果的job队列中移出
     *
     * @param jobkey 任务的jobKey
     */
    public static void removeJobIfExited(String jobkey) {
        logger.info("Start remove Job If Exited");
        if (jobs.contains(jobkey))
            jobs.remove(jobkey);
        logger.info("jobkey to remove: {}", jobkey);
        logger.info("End remove Job If Exited");
    }
}
