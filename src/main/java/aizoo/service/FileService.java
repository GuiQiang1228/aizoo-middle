package aizoo.service;


import aizoo.common.DownloadStatus;
import aizoo.common.JobStatus;
import aizoo.common.JobType;
import aizoo.controller.GraphController;
import aizoo.domain.*;
import aizoo.repository.*;
import aizoo.viewObject.mapper.ComponentVOEntityMapper;
import aizoo.viewObject.object.ComponentVO;
import aizoo.viewObject.object.PictureVO;
import aizoo.viewObject.object.StructureFileVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.apache.commons.lang3.EnumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import aizoo.utils.FileUtil;
import org.apache.commons.io.FileUtils;
import aizoo.utils.ZipUtil;


import javax.persistence.EntityNotFoundException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.*;

import static aizoo.utils.AizooConstans.COMPILE_ZIP_FILE_FOLDER_NAME;


@Service("FileService")
public class FileService {

    @Value("${file.path}")
    String filePath;


    @Value("${download.dir}")
    String downloadDir;

    @Value("${save.path}")
    String savePath;

    @Autowired
    ComponentDAO componentDAO;

    @Autowired
    ServiceJobDAO serviceJobDAO;

    @Autowired
    JobService jobService;

    @Autowired
    ExperimentJobDAO experimentJobDAO;

    @Autowired
    MirrorJobDAO mirrorJobDAO;

    @Autowired
    UserDAO userDAO;

    @Autowired
    CodeDAO codeDAO;

    @Autowired
    ApplicationDAO applicationDAO;

    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

    /**
     * 将图片保存到用户图片文件夹下
     *
     * @param pictureVO 图片信息
     * @param username  用户名
     * @return 无返回值
     */
    public void uploadPicture(PictureVO pictureVO, String username) throws IOException {
//        截图的上传地址为filePath/user/picture/graphType/graphId_graphName.png
        MultipartFile picture = pictureVO.getPictureFile();
        Path path = Paths.get(filePath, username, "picture", pictureVO.getPictureType().getValue());
        FileUtil.uploadFile(picture, path.toString());
    }

    /**
     * 检查文件是否存在于命名空间中
     *
     * @param namespace 命名空间
     * @param fileName  文件名字，多个文件以逗号相隔
     * @return 若存在，返回文件名字；否则返回 "SUCCESS"
     */
    public String fileCheck(String namespace, String fileName) {
        String[] fileList = fileName.split(",");
        for (String fileNames : fileList) {
            String file = Paths.get(filePath, namespace.replace(".", "/"), fileNames).toString();
            if (FileUtil.fileExists(file)) {
                return fileNames;
            }
        }
        return "SUCCESS";
    }

    /**
     * 检查用户剩余硬盘容量是否足够支持本次上传
     * 以byte为单位进行比较
     *
     * @param size 待上传文件大小
     * @param username  用户名
     * @return 若剩余空间足够上传，返回true，否则返回false
     */
    public Boolean fileDiskCheck(double size, String username) {
        User user = userDAO.findByUsername(username);
        Level level = user.getLevel();
        double totalDisk = level.getDisk()*1024*1024;
        double usedDisk = FileUtil.getSize(new File(Paths.get(filePath, username).toString()));
        if(totalDisk - usedDisk < size)
            return false;
        return true;
    }

    /**
     * 获得实验结果
     * 先确定当前任务的状态，若不是有效的job status中的一种，则返回null；
     * 若当前任务已经结束，但是无法在路径中找到任务生成的文件，则返回null；
     * 否则将运行结果、代码文件下载至用户目录下（只拷贝save.path下的结果）
     * 在任务执行时，已将代码文件从file.path拷到save.path中）
     * 并返回下载文件的路径
     * 此任务必须单线程执行，且synchronized无法与transactional共同使用，要自己处理请求中所有的报错与回滚，
     *
     * @param id       实验id
     * @param username 用户名
     * @return 先确定当前任务的状态，若不是有效的job status中的一种，则返回null；
     * 若当前任务已经结束，但是无法在路径中找到任务生成的文件，则返回null；
     * 否则返回下载文件的路径
     */
    public synchronized String getExperimentDownResult(Long id, String username) throws Exception {
        ExperimentJob experimentJob = experimentJobDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
        String jobKey = experimentJob.getJobKey();
        String jobStatus = null;
        try {
            jobStatus = jobService.getJobStatus(jobKey, JobType.EXPERIMENT_JOB, experimentJob.getUser());
        } catch (JsonProcessingException e) {
            logger.error("getExperimentDownResult failed! And get job status failed! error=", e);
        }

        if (!EnumUtils.isValidEnum(JobStatus.class, jobStatus)) {
            experimentJob.setDownloadStatus(DownloadStatus.DOWNLOAD_ERROR);
            experimentJobDAO.save(experimentJob);
            return null;
        }

        //下载地址，因为每一个job都只会 执行一次
        // 如果删掉重拷，下次拷过来的文件，也可能是不同版本，所以不用删掉重拷
        File targetPath = new File(Paths.get(downloadDir, username, "job_result", String.valueOf(experimentJob.getId())).toString());
        File zipFile = new File(targetPath, experimentJob.getId() + ".zip");

        try {
            if (!targetPath.exists() || !zipFile.exists()) {
                experimentJob.setDownloadStatus(DownloadStatus.DOWNLOADING);
                experimentJobDAO.save(experimentJob);
                targetPath.mkdirs();
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> map = objectMapper.readValue(experimentJob.getEnvironment(), new TypeReference<Map<String, Object>>() {
                });
                Map<String, Object> jobEnv = (Map<String, Object>) map.get(jobKey);

                // 这个路径是从job的stdOut取的，为的是避免改了save path后，原来的文件找不到,导致下载报错
                String stdOut = (String) jobEnv.get("std_out");
                String savePath = stdOut.substring(0, stdOut.indexOf("runtime_log"));
                File sourceDir = new File(savePath);
                // 若sourceDir不存在，则说明任务执行并未生成结果，因此下载失败
                if (!sourceDir.exists()) {
                    experimentJob.setDownloadStatus(DownloadStatus.DOWNLOAD_ERROR);
                    experimentJobDAO.save(experimentJob);
                    return null;
                }

                // 将sourceDir中的job运行结果复制到static目录下，供用户下载
                for (File f : Objects.requireNonNull(sourceDir.listFiles())) {
                    if (f.isDirectory() && (f.getName().equals("user_saved") || f.getName().equals("ml_model")))
                        continue;
                    if (f.isDirectory())
                        FileUtils.copyDirectoryToDirectory(f, targetPath);
                    else if (f.isFile())
                        FileUtils.copyFileToDirectory(f, targetPath);
                }
                ZipUtil.zip(targetPath.getAbsolutePath(), zipFile.getAbsolutePath(), "job_result");
            }
            experimentJob.setDownloadStatus(DownloadStatus.COMPLETED);
            experimentJobDAO.save(experimentJob);
            return zipFile.getAbsolutePath();
        } catch (Exception e) {
            logger.error("getExperimentDownResult failed! error=", e);
            try {
                FileUtil.deleteFile(targetPath);
            } catch (Exception exception) {

                logger.error("getExperimentDownResult failed! And target file delete failed! error=", e);
            }
            experimentJob.setDownloadStatus(DownloadStatus.DOWNLOAD_ERROR);
            experimentJobDAO.save(experimentJob);
            return null;
        }
    }

    /**
     * 获得服务结果
     * 先确定当前任务的状态，若不是有效的job status中的一种，则返回null；
     * 若当前任务已经结束，但是无法在路径中找到任务生成的文件，则返回null；
     * 否则将运行结果、代码文件下载至用户目录下（只拷贝save.path下的结果）
     * 在任务执行时，已将代码文件从file.path拷到save.path中）
     * 并返回下载文件的路径
     * 此任务必须单线程执行，且synchronized无法与transactional共同使用，要自己处理请求中所有的报错与回滚
     * <p>
     * 由于service是可以边执行边下载结果的，所以每次下载都要清空文件夹，避免log还是旧log！！！
     *
     * @param id       服务id
     * @param username 用户名
     * @return 先确定当前任务的状态，若不是有效的job status中的一种，则返回null；
     * 若当前任务已经结束，但是无法在路径中找到任务生成的文件，则返回null；
     * 否则返回下载文件的路径
     */
    public synchronized String getServiceDownResult(Long id, String username) throws Exception {
        ServiceJob serviceJob = serviceJobDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
        String jobKey = serviceJob.getJobKey();
        String jobStatus = null;
        try {
            jobStatus = jobService.getJobStatus(jobKey, JobType.SERVICE_JOB, serviceJob.getUser());
        } catch (JsonProcessingException e) {
            logger.error("getServiceDownResult failed! And get job status failed! error=", e);
        }
        if (!EnumUtils.isValidEnum(JobStatus.class, jobStatus)) {
            serviceJob.setDownloadStatus(DownloadStatus.DOWNLOAD_ERROR);
            serviceJobDAO.save(serviceJob);
            return null;
        }
        File targetPath = new File(Paths.get(downloadDir, username, "job_result", String.valueOf(serviceJob.getId())).toString());
        File zipFile = new File(targetPath, serviceJob.getId() + ".zip");
        try {
            // 清空之前下载的全部内容
            if (targetPath.exists())
                FileUtil.deleteFile(targetPath);
            targetPath.mkdirs();
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> map = objectMapper.readValue(serviceJob.getEnvironment(), new TypeReference<Map<String, Object>>() {
            });
            Map<String, Object> jobEnv = (Map<String, Object>) map.get(jobKey);
            String stdOut = (String) jobEnv.get("std_out");
            String savePath = stdOut.substring(0, stdOut.indexOf("runtime_log"));
            System.out.println(savePath);
            File copyDir = new File(savePath);
            if (!copyDir.exists()) {
                serviceJob.setDownloadStatus(DownloadStatus.DOWNLOAD_ERROR);
                serviceJobDAO.save(serviceJob);
                return null;
            }
            // 将sourceDir中的job运行结果复制到static目录下，供用户下载
            for (File f : Objects.requireNonNull(copyDir.listFiles())) {
                if (f.isDirectory() && (f.getName().equals("user_saved") || f.getName().equals("ml_model")))
                    continue;
                if (f.isDirectory())
                    FileUtils.copyDirectoryToDirectory(f, targetPath);
                else if (f.isFile())
                    FileUtils.copyFileToDirectory(f, targetPath);
            }
            ZipUtil.zip(targetPath.getAbsolutePath(), zipFile.getAbsolutePath(), "job_result");
            serviceJob.setDownloadStatus(DownloadStatus.COMPLETED);
            serviceJobDAO.save(serviceJob);
            return zipFile.getAbsolutePath();

        } catch (Exception e) {
            logger.error("getServiceDownResult failed! error=", e);
            try {
                FileUtil.deleteFile(targetPath);
            } catch (Exception exception) {

                logger.error("getServiceDownResult failed! And target file delete failed! error=", e);
            }
            serviceJob.setDownloadStatus(DownloadStatus.DOWNLOAD_ERROR);
            serviceJobDAO.save(serviceJob);
            return null;
        }

    }

    /**
     * 获得镜像实验结果
     * 先确定当前任务的状态，若不是有效的job status中的一种，则返回null；
     * 若当前任务已经结束，但是无法在路径中找到任务生成的文件，则返回null；
     * 否则将运行结果、代码文件下载至用户目录下（只拷贝save.path下的结果）
     * 在任务执行时，已将代码文件从file.path拷到save.path中）
     * 并返回下载文件的路径
     * 此任务必须单线程执行，且synchronized无法与transactional共同使用，要自己处理请求中所有的报错与回滚，
     *
     * @param id       镜像实验id
     * @param username 用户名
     * @return 先确定当前任务的状态，若不是有效的job status中的一种，则返回null；
     * 若当前任务已经结束，但是无法在路径中找到任务生成的文件，则返回null；
     * 若当前任务已经结束，但需要拷贝下载的文件夹大于1GB，返回null；
     * 否则返回下载文件的路径
     */
    public synchronized String getMirrorJobDownResult(Long id, String username) throws Exception {
        MirrorJob mirrorJob = mirrorJobDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
        String jobKey = mirrorJob.getJobKey();
        String jobStatus = null;
        try {
            jobStatus = jobService.getJobStatus(jobKey, JobType.EXPERIMENT_JOB, mirrorJob.getUser());
        } catch (JsonProcessingException e) {
            logger.error("getExperimentDownResult failed! And get job status failed! error=", e);
        }

        if (!EnumUtils.isValidEnum(JobStatus.class, jobStatus)) {
            mirrorJob.setDownloadStatus(DownloadStatus.DOWNLOAD_ERROR);
            mirrorJobDAO.save(mirrorJob);
            return null;
        }

        //下载地址，因为每一个job都只会 执行一次
        // 如果删掉重拷，下次拷过来的文件，也可能是不同版本，所以不用删掉重拷
        File targetPath = new File(Paths.get(downloadDir, username, "job_result", String.valueOf(mirrorJob.getId())).toString());
        File zipFile = new File(targetPath, mirrorJob.getId() + ".zip");

        try {
            if (!targetPath.exists() || !zipFile.exists()) {
                mirrorJob.setDownloadStatus(DownloadStatus.DOWNLOADING);
                mirrorJobDAO.save(mirrorJob);
                targetPath.mkdirs();
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> map = objectMapper.readValue(mirrorJob.getEnvironment(), new TypeReference<Map<String, Object>>() {
                });
                Map<String, Object> jobEnv = (Map<String, Object>) map.get(jobKey);

                // 这个路径是从job的stdOut取的，为的是避免改了save path后，原来的文件找不到,导致下载报错
                String stdOut = (String) jobEnv.get("std_out");
                String savePath = stdOut.substring(0, stdOut.indexOf("runtime_log"));
                File sourceDir = new File(savePath);
                // 若sourceDir不存在，则说明任务执行并未生成结果，因此下载失败
                if (!sourceDir.exists()) {
                    mirrorJob.setDownloadStatus(DownloadStatus.DOWNLOAD_ERROR);
                    mirrorJobDAO.save(mirrorJob);
                    return null;
                }

                //判断文件夹的size大小，若超过1GB，拒绝下载
                if (FileUtil.getSize(sourceDir) > 1073741824) {
                    logger.info("MirrorJob file size too large! download failed!");
                    mirrorJob.setDownloadStatus(DownloadStatus.DOWNLOAD_ERROR);
                    return null;
                }

                // 将sourceDir中的job运行结果复制到static目录下，供用户下载
                for (File f : Objects.requireNonNull(sourceDir.listFiles())) {
                    if (f.isDirectory() && (f.getName().equals("user_saved") || f.getName().equals("ml_model")))
                        continue;
                    if (f.isDirectory())
                        FileUtils.copyDirectoryToDirectory(f, targetPath);
                    else if (f.isFile())
                        FileUtils.copyFileToDirectory(f, targetPath);
                }
                ZipUtil.zip(targetPath.getAbsolutePath(), zipFile.getAbsolutePath(), "job_result");
            }
            mirrorJob.setDownloadStatus(DownloadStatus.COMPLETED);
            mirrorJobDAO.save(mirrorJob);
            return zipFile.getAbsolutePath();
        } catch (Exception e) {
            logger.error("getMirrorJobDownResult failed! error=", e);
            try {
                FileUtil.deleteFile(targetPath);
            } catch (Exception exception) {

                logger.error("getMirrorJobDownResult failed! And target file delete failed! error=", e);
            }
            mirrorJob.setDownloadStatus(DownloadStatus.DOWNLOAD_ERROR);
            mirrorJobDAO.save(mirrorJob);
            return null;
        }
    }


    /**
     * 编译下载代码，需要将原来的代码整个复制到下载目录下
     * 如果下载目录已存在改路径，则删掉重新拷贝（避免出现有代码不是最新的情况）
     * 需要单线程进入
     *
     * @param sourceDir 代码路径
     * @param filename  压缩包名称
     * @return 压缩包路径
     * @throws Exception
     */
    public synchronized String downloadTempCode(String sourceDir, String filename) throws Exception {
        File targetPath = new File(downloadDir, sourceDir.replace(filePath, ""));
        if (targetPath.exists())
            FileUtil.deleteFile(targetPath);
        targetPath.mkdirs();
        File zipFile = new File(targetPath, filename + ".zip");
        for (File f : new File(sourceDir).listFiles()) {
            if (f.isDirectory())
                FileUtils.copyDirectoryToDirectory(f, targetPath);
            else if (f.isFile())
                FileUtils.copyFileToDirectory(f, targetPath);
        }
        // graph_codes为压缩包内文件夹的名字
        ZipUtil.zip(sourceDir, zipFile.getAbsolutePath(), COMPILE_ZIP_FILE_FOLDER_NAME);
        return zipFile.getAbsolutePath();
    }

    /**
     * 生成算子的结构文件和源文件(单线程进入)
     *
     * @param id 算子id
     * @return 压缩包绝对路径
     * @throws Exception
     */
    public synchronized String downloadAtomicComponentFiles(long id) throws Exception {
        Component component = componentDAO.findById(id).orElseThrow(() -> new EntityNotFoundException());
        ComponentVO componentVO = ComponentVOEntityMapper.MAPPER.component2ComponentVO(component);
        StructureFileVO sfVO = new StructureFileVO(componentVO.getName(), componentVO.getDescription(), componentVO.getExample(), componentVO.getFramework(), componentVO.getFrameworkVersion(), componentVO.getComponentVersion());

        //VO格式化为可导出后直接上传的形式，且各字段信息与上传前一样
        List<Map<String, Object>> inputs = componentVO.getInputs();
        List<Map<String, Object>> outputs = componentVO.getOutputs();
        for (Map<String, Object> input : inputs) {
            input.remove("id");
            input.remove("title");
            input.remove("originName");
        }
        for (Map<String, Object> output : outputs) {
            if (output.get("name").toString().equals("self")) {
                outputs.remove(output);
                break;
            }
        }
        for (Map<String, Object> output : outputs) {
            output.remove("id");
            output.remove("title");
            output.remove("originName");
            output.remove("isSelf");
        }
        Map<String, Object> properties = componentVO.getProperties();
        for (String key : properties.keySet()) {
            if (!properties.get(key).toString().contains("{"))
                continue;
            LinkedHashMap<String, String> objectMap = (LinkedHashMap<String, String>) properties.get(key);
            if (objectMap.get("default") != null)
                objectMap.remove("default");
            if (!objectMap.get("defaultType").equals("None") && (objectMap.get("defaultType").length() < 5 || !objectMap.get("defaultType").toString().substring(0, 5).equals("aizoo")))
                objectMap.replace("defaultType", "aizoo." + objectMap.get("defaultType"));
            if (!objectMap.get("type").equals("None") && (objectMap.get("type").toString().length() < 5 || !objectMap.get("type").toString().substring(0, 5).equals("aizoo")))
                objectMap.replace("type", "aizoo." + objectMap.get("type"));
            properties.replace(key, objectMap);
        }

        sfVO.setInputs(inputs);
        sfVO.setOutputs(outputs);
        sfVO.setProperties(properties);

        // 存放路径=downloaderDir/componentV0.username/atomic_components/component.name/id
        String dirStr = Paths.get(downloadDir, componentVO.getUsername(), "atomic_components", component.getName(), String.valueOf(id)).toString();

        File dir = new File(dirStr);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        // 清空之前下载的垃圾
        for (File f : dir.listFiles())
            FileUtil.deleteFile(f);


        // 生成结构文件
        File structFile = new File(dir, component.getName() + ".json");
        String pretty = JSON.toJSONString(sfVO, SerializerFeature.PrettyFormat, SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteDateUseDateFormat);
        Writer write = new OutputStreamWriter(new FileOutputStream(structFile), StandardCharsets.UTF_8);
        write.write(pretty);
        write.flush();
        write.close();

        // 源文件，由于这个是算子，它没有引用文件，所以只用拷这一个文件即可
        String sourceFilePath = component.getPath();
        File sourceFile = new File(sourceFilePath);


        // 压缩成zip包
        List<File> fileList = new ArrayList<>();
        fileList.add(structFile);
        fileList.add(sourceFile);
        File zipFile = new File(dir, component.getName() + ".zip");
        FileOutputStream fos2 = new FileOutputStream(zipFile);
        ZipUtil.toZip(fileList, fos2);
        return zipFile.getAbsolutePath();
    }

    /**
     指定根目录文件夹，可浏览整个文件夹，及其子文件，显示文件大小、最近修改时间
     * @param path 指定的根目录
     * @param relativePath 指定的相对路径
     * @return List<Map < String, Object>, ...>
     * mapKey1: isDir: 该文件是否是文件夹
     * mapKey2: name: 文件名
     * mapKey3: size: 文件大小
     * mapKey4: lastModified 最近修改时间
     * mapKey5: relativePath: 该文件的相对路径
     */
    public List<Map<String, Object>> traverseFiles(String path, String relativePath) {
        logger.info("Begin traverseFiles");
        logger.info("path: {}, relativePath: {}", path, relativePath);
        List<Map<String, Object>> result = new ArrayList<>();
        File fileObj = new File(path);
        File[] fileList = fileObj.listFiles();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String fileSizeString = "";
        // 如果指定的就是一个文件 则只返回该文件的相对路径
        if (!fileObj.isDirectory()) {
            Map<String, Object> map = new HashMap<>();
            map.put("isDir", fileObj.isDirectory());
            map.put("name", fileObj.getName());
            map.put("lastModified", sdf.format(fileObj.lastModified()));
            map.put("size", FileUtil.getFormatSize(fileObj));
            map.put("relativePath", relativePath);
            result.add(map);
        }
        if (fileList != null) {
            // 遍历所有文件 判断目录/文件 放进map 再放进list
            for (File file : fileList) {
                Map<String, Object> map = new HashMap<>();
                if (!file.isDirectory()) {
                    fileSizeString = FileUtil.getFormatSize(file);
                }
                else fileSizeString = "";
                map.put("isDir", file.isDirectory());
                map.put("name", file.getName());
                map.put("lastModified", sdf.format(file.lastModified()));
                map.put("size", fileSizeString);
                map.put("relativePath", relativePath + "/" + file.getName());
                result.add(map);
            }
        }
        logger.info("End traverseFiles");
        return result;
    }

    /**
     * 生成文件(单线程进入)
     *
     * @param id 代码id
     * @return 文件绝对路径
     * @throws Exception
     */

    public synchronized String downloadAtomicFiles(Long id, String type, String relativePath, Principal principal) throws Exception {
        String sourceFilePath = "";
        //String fileName = "";
        String userName = principal.getName();
        if (type.equals("code") || type.equals("mirrorjob")) {
            if (relativePath != "")
                sourceFilePath = Paths.get(filePath, userName, type, String.valueOf(id), relativePath).toString();
            else sourceFilePath = Paths.get(filePath, userName, type, String.valueOf(id)).toString();
        } else if (type.equals("experimentjob")) {
            ExperimentJob experimentJob = experimentJobDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
            sourceFilePath = Paths.get(experimentJob.getRootPath(), relativePath).toString();
        }
        //存放路径=downloaderDir/userName/type/filename_uuid
        String toFilePath = Paths.get(downloadDir, userName, type).toString();
        File dir = new File(toFilePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return FileUtil.copySingleFile(sourceFilePath, toFilePath);
        //return toFilePath;
    }

    /**
    * @Description: 组织访问notebook的url
     * @param type:          文件类型
     * @param id:            文件id
     * @param relativePath:  文件相对路径
     * @param userName:      用户名
    * @return: java.lang.String
    */
    public String getReadFileUrlPath(String type,String id,String relativePath,String userName){
        String path = "";
        logger.info("start getReadFileUrlPath");
        logger.info("type: {},id: {},relativePath: {},userName: {}",type,id,relativePath,userName);
        String noteBookApi = "api/contents";
        if (type.equals("code") || type.equals("mirrorjob")) {
            path = Paths.get(noteBookApi, userName, type, id, relativePath).toString();
        }
        else if (type.equals("experimentjob")) {
            ExperimentJob experimentJob = experimentJobDAO.findById(Long.parseLong(id)).orElseThrow(() -> new EntityNotFoundException(id));
            path = experimentJob.getRootPath().replace(savePath, "out");
            path = Paths.get(noteBookApi, path, relativePath).toString();
        }
        else if(type.equals("servicejob")){
            ServiceJob serviceJob = serviceJobDAO.findById(Long.parseLong(id)).orElseThrow(() -> new EntityNotFoundException(id));
            path = serviceJob.getRootPath().replace(savePath, "out");
            path = Paths.get(noteBookApi, path, relativePath).toString();
        }
        else if(type.equals("application")){
            Application application = applicationDAO.findById(Long.parseLong(id)).orElseThrow(() -> new EntityNotFoundException(id));
            path = application.getRootPath().replace(savePath, "out");
            path = Paths.get(noteBookApi, path, relativePath).toString();
        }
        logger.info("path: {}",path);
        logger.info("End getReadFileUrlPath");
        return path;
    }
}
