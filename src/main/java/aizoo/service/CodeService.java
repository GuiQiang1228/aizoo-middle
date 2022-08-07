package aizoo.service;

import aizoo.domain.Code;
import aizoo.domain.User;
import aizoo.repository.CodeDAO;
import aizoo.repository.UserDAO;
import aizoo.utils.FileUtil;
import aizoo.utils.ZipUtil;
import aizoo.viewObject.mapper.CodeVOEntityMapper;
import aizoo.viewObject.object.CodeVO;
import aizoo.viewObject.object.TFileInfoVO;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static aizoo.utils.AizooConstans.COMPILE_ZIP_FILE_FOLDER_NAME;

@Service("CodeService")
public class CodeService {
    @Autowired
    CodeDAO codeDAO;

    @Autowired
    UserDAO userDAO;

    @Value("${file.path}")
    private String filePath;

    @Value("${download.dir}")
    String downloadDir;

    private final static Logger logger = LoggerFactory.getLogger(CodeService.class);

    /**
     * 根据用户传入的相对路径或id, 联级获取下一级的code路径
     * 输入为/xx的相对路径时, 返回该路径下一级的文件列表信息
     * 返回code相对路径时从id下一层开始
     *
     * @param userIdx   用户提供的相对路径
     * @param id        codeId
     * @param userName 用户名
     * @return List<Map < String, Object>, ...>
     * mapKey1: isDir: 该文件是否是文件夹
     * mapKey2: name: 文件名
     * mapKey3: relativePath: 从id的下一层开始的相对路径
     */
    public List<Map<String, Object>> getNextPath(String userIdx, String id, String userName){
        List<Map<String, Object>> result = new ArrayList<>();
        String curPath = "";
        if (userIdx != "") curPath = Paths.get(filePath, userName, "code", id, userIdx).toString();
        else curPath = Paths.get(filePath, userName, "code", id).toString();
        // 创建该路径的文件对象及文件列表
        File fileObj = new File(curPath);
        File[] fileList = fileObj.listFiles();
        // 如果指定的就是一个文件 则只返回该文件的相对路径
        if (fileObj.isFile()) {
            Map<String, Object> map = new HashMap<>();
            map.put("isDir", fileObj.isDirectory());
            map.put("name", fileObj.getName());
            map.put("relativePath", userIdx);
            result.add(map);
        }
        if (fileList != null) {
            // 遍历所有文件 判断目录/文件 放进map 再放进list
            for (File file : fileList) {
                Map<String, Object> map = new HashMap<>();
                map.put("isDir", file.isDirectory());
                map.put("name", file.getName());
                map.put("relativePath", userIdx + "/" + file.getName());
                result.add(map);
            }
        }
        return result;
    }

    /**
     * 生成代码源文件(单线程进入)
     *
     * @param id 代码id
     * @return 压缩包绝对路径
     * @throws Exception
     */
    public synchronized String downloadAtomicCodeFiles(long id) throws Exception {
        Code code = codeDAO.findById(id).orElseThrow(() -> new EntityNotFoundException());

        //存放路径=downloaderDir/code.username/code/id/
        String dirStr = Paths.get(downloadDir, code.getUser().getUsername(), "code", String.valueOf(id)).toString();
        File dir = new File(dirStr);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        // 清空之前下载的垃圾
        for (File f : dir.listFiles())
            FileUtil.deleteFile(f);
        // 源文件
        String sourceFilePath = code.getPath();
        File zipFile = new File(dir, code.getName() + ".zip");
        File sourceFile = new File(sourceFilePath);
//        if(sourceFile.isFile()){
//            List<File> fileList = new ArrayList<>();
//            fileList.add(sourceFile);
//            FileOutputStream fos2 = new FileOutputStream(zipFile);
//            ZipUtil.toZip(fileList, fos2);
//        }else{
//            ZipUtil.codeZip(sourceFilePath, zipFile.getAbsolutePath());
//        }
        ZipUtil.codeZip(sourceFilePath, zipFile.getAbsolutePath());
        return zipFile.getAbsolutePath();
    }

    /**
     * 删除代码资源方法
     *
     * @param id 根据代码资源ID定位数据库中的，id由前端传参
     * @throws Exception
     */
    @Transactional
    public void deleteCode(Long id) throws Exception {
        logger.info("Start delete Code");
        // 根据ID去数据库中查找对应的代码资源，如果没有找到抛出一个notfound异常
        Code code = codeDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
        logger.info("findById CodeId: {}", id);
        // 获取代码资源在服务器上的存放位置
        String filePath = code.getPath();
        logger.info("Code Path: {}", filePath);
        code.setUser(null);
        // 开始删除code
        codeDAO.delete(code);
        //删除服务器上的代码资源文件
        FileUtil.deleteFile(new File(filePath));
        logger.info("End delete Code");
    }

    /**
     * 检查该code名是否已经注册
     *
     * @param codeName 待检查的code名
     * @param userName 待检查的用户名
     * @return 若已经注册返回false，若未注册返回true
     */
    public boolean checkCodeName(String codeName,String userName) {
        logger.info("Start check codeName");
        logger.info("checkCodeName codeName:{}", codeName);
        Code code = codeDAO.findByNameAndUserUsername(codeName,userName);
        if (code == null) {
            logger.info("【该code名称不存在，可以注册】");
            logger.info("End check codeName");
            return true;
        }
        logger.info("【该code名称已存在，不可以注册】");
        logger.info("End check codeName");
        return false;
    }

    /**
     * 将切片文件合并，若为压缩文件则先解压然后复制到指定文件夹中
     *
     * @param fileInfo 文件信息,前端MD5上传文件
     * @param codeVO   code组织成的VO信息
     * @throws Exception
     */
    @Transactional
    public void uploadCode(TFileInfoVO fileInfo, CodeVO codeVO) throws Exception {
        logger.info("Start upload Code");

        // 将code由VO转为entity，再去更新数据库
        Code code = CodeVOEntityMapper.MAPPER.codeVO2Code(codeVO, userDAO);
        // 先将code存到数据库中，生成code的Id
        codeDAO.save(code);
        // 根据code名返回code便于获取id
        //得到附件名称
        String filename = fileInfo.getName();
        logger.info("fileInfo: {}", fileInfo.toString());
        logger.info("codeVO: {}", codeVO.toString());
        // filePath是服务器上存放用户上传文件的起始地址
        // tempMergeFile是具体到temp文件夹里的具体文件,具体路径为{file.path}/{username}/temp/{identifier}/{filename}
        // 根目录之后按照用户名来存放文件,temp是为了将用户上传的切片文件存放的文件夹, UniqueIdentifier是附件MD5标识
        String tempMergeFile = Paths.get(filePath, codeVO.getUsername(), "temp", fileInfo.getUniqueIdentifier(), filename).toString();
        // tempFolder是temp文件夹的绝对路径，具体路径为{file.path}/{username}/temp/{identifier}
        String tempFolder = Paths.get(filePath, codeVO.getUsername(), "temp", fileInfo.getUniqueIdentifier()).toString();
        //设置目标存放路径（解压前）,targetTempPath为{file.path}/{username}/code/{code.id}/{filename}
        String targetTempPath = Paths.get(filePath, codeVO.getUsername(), "code", code.getId().toString(), filename).toString();
        //设置目标存放路径（最终）,targetTempPath为{file.path}/{username}/code/{code.id}/{filename}
        String targetPath = targetTempPath;
        //目标存放文件夹
        String targetFolderPath = Paths.get(filePath, codeVO.getUsername(), "code", code.getId().toString()).toString();
        try {
            logger.info("Start merge file,tempFolder: {}", tempFolder);
            //进行在临时目录下切片文件的合并操作，tempMergeFile为合并后的目标文件路径，tempFolder为文件夹路径，filename为不参与合并的文件名
            FileUtil.merge(tempMergeFile, tempFolder, filename);
            logger.info("Start copy file,targetFolder: {}", targetFolderPath);
            //先将code从临时目录复制到目标存放路径下
            FileUtil.copyFile(tempMergeFile, targetFolderPath);
            if (filename.endsWith(".zip")) {
                logger.info("Start unzip file,targetTempPath：{},targetFolderPath：{}", targetTempPath, targetFolderPath);
                //更换filename（修改为解压后的文件名）
                int lastIndex = filename.lastIndexOf(".");
                filename = filename.substring(0, lastIndex);
                //将最终目标路径换为解压后的文件
                targetPath = Paths.get(filePath, codeVO.getUsername(), "code", code.getId().toString(), filename).toString();
                //先新建目标文件夹，解决只有一个文件夹的zip文件缺少结构的问题
                File targetDir = new File(targetPath);
                if(!targetDir.exists()){
                    targetDir.mkdirs();
                }
                //需要解压的文件
                File targetZipFile = new File(targetTempPath);
                //解压到目标文件夹下
                ZipUtil.unZip(targetZipFile, targetPath);
                logger.info("unzip file successfully");
                //将复制后的压缩文件删除
                FileUtil.deleteFile(new File(targetTempPath));
            }
            // 放到对应的code文件夹里
            code.setPath(targetPath);
            codeDAO.save(code);
        } catch (Exception e) {
            //删除目录中本次上传的文件
            File f = new File(tempMergeFile);
            FileUtil.deleteFile(f);
            //删除数据库中的信息
            codeDAO.delete(code);
            logger.error("上传code失败，删除目录中本次上传的code");
            logger.error("上传code失败,错误信息:{}", e);
            throw e;
        } finally {
            logger.info("Start delete temp folder and file");
            //将临时目录下的文件和文件夹删除
            FileUtils.deleteDirectory(new File(tempFolder));
        }
        logger.info("End upload code");

    }

    /**
     * 将切片文件合并，并拷贝至指定目录下
     *
     * @param fileInfo 文件信息,前端MD5上传文件
     * @param id 对应代码的id
     * @param relativePath 相对路径
     * @param username 用户名
     * @throws Exception
     */
    @Transactional
    public void uploadSingleFile(TFileInfoVO fileInfo, long id, String relativePath, String username) throws Exception {
        logger.info("Start upload single Code file");

        // 查找对应的code
        Code code = codeDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
        //得到附件名称
        String filename = fileInfo.getName();
        logger.info("fileInfo: {}", fileInfo.toString());
        // filePath是服务器上存放用户上传文件的起始地址
        // tempMergeFile是具体到temp文件夹里的具体文件,具体路径为{file.path}/{username}/temp/{identifier}/{filename}
        // 根目录之后按照用户名来存放文件,temp是为了将用户上传的切片文件存放的文件夹, UniqueIdentifier是附件MD5标识
        String tempMergeFile = Paths.get(filePath, username, "temp", fileInfo.getUniqueIdentifier(), filename).toString();
        // tempFolder是temp文件夹的绝对路径，具体路径为{file.path}/{username}/temp/{identifier}
        String tempFolder = Paths.get(filePath, username, "temp", fileInfo.getUniqueIdentifier()).toString();
        //设置目标存放路径, targetTempPath为{file.path}/{username}/code/{code.id}/relativePath/filename
        String targetPath = "";
        if(relativePath != "")
            targetPath = Paths.get(filePath, username, "code", code.getId().toString(), relativePath, filename).toString();
        else
            targetPath = Paths.get(filePath, username, "code", code.getId().toString(), filename).toString();
        File targetFile = new File(targetPath);
        try {
            logger.info("Start merge file,tempFolder: {}", tempFolder);
            //进行在临时目录下切片文件的合并操作，tempMergeFile为合并后的目标文件路径，tempFolder为文件夹路径，filename为不参与合并的文件名
            FileUtil.merge(tempMergeFile, tempFolder, filename);
            logger.info("Start copy file,targetFolder: {}", targetPath);
            //先将code从临时目录复制到目标存放路径下
            FileUtil.copyFile(tempMergeFile, targetFile.getParent());
        } catch (Exception e) {
            //删除目录中本次上传的文件
            File f = new File(tempMergeFile);
            FileUtil.deleteFile(f);
            //删除数据库中的信息
            logger.error("上传code单个文件失败，删除目录中本次上传的文件");
            logger.error("上传code单个文件失败,错误信息:{}", e);
            throw e;
        } finally {
            logger.info("Start delete temp file");
            //将临时目录下的文件和文件夹删除
            FileUtils.deleteDirectory(new File(tempFolder));
        }
        logger.info("End upload single Code file");

    }
}
