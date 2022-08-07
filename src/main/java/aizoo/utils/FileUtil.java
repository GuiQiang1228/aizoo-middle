package aizoo.utils;


import aizoo.response.BaseException;
import aizoo.response.ResponseCode;
import aizoo.viewObject.object.ComponentVO;
import aizoo.viewObject.object.TChunkInfoVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;


public class FileUtil {
    private final static Logger logger = LoggerFactory.getLogger(FileUtil.class);

    /**
     * 将某个文件保存到特定路径中
     * 文件不存在则新建文件，文件存在则覆盖
     *
     * @param file 要上传的文件
     * @param path 存放文件的目录路径
     * @return 文件路径
     */
    public static String uploadFile(MultipartFile file, String path) throws IOException {
        File dir = new File(path);  //目录的路径
        //文件的路径dest=dir/filename
        File dest = new File(Paths.get(path, file.getOriginalFilename()).toString());
        if (!dir.exists())
            dir.mkdirs();
        file.transferTo(dest);
        //返回绝对路径 dir/filename
        return dest.getAbsolutePath();
    }

    /**
     * 删除文件或者文件夹
     * 若是文件夹会删除所有子文件和文件夹本身
     *
     * @param directory 要删除的文件或者文件夹
     * @return 无返回值
     */
    public static void deleteFile(File directory) throws Exception {
        if (!directory.isDirectory()) {
            directory.delete();
        } else {
            File[] files = directory.listFiles();

            // 空文件夹
            assert files != null;
            if (files.length == 0) {
                directory.delete();
                System.out.println("删除" + directory.getAbsolutePath());
                return;
            }

            // 删除子文件夹和子文件
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteFile(file);
                } else {
                    file.delete();
                    System.out.println("删除" + file.getAbsolutePath());
                }
            }

            // 删除文件夹本身
            directory.delete();
            System.out.println("删除" + directory.getAbsolutePath());
        }
    }

    /**
     * 复制特定路径的文件到新的路径
     * 若新的路径存在同名文件，会首先删除文件然后建立新的文件
     *
     * @param fromFilePath 复制的文件路径
     * @param toPath       存放文件的目录
     * @return 文件路径
     */
    public static String copyOrOverwriteFile(String fromFilePath, String toPath) throws IOException {
        File fromFile = new File(fromFilePath);
        File toFilePath = new File(toPath);
        //    检查目标路径是否存在
        if (!toFilePath.exists()) {
            toFilePath.mkdirs();
        }
        // 文件的目标路径为 toPath/fromFileName
        File toFile = new File(toPath + File.separator + fromFile.getName());
        //        检查目标路径下是否有同名文件，有则删除
        if (toFile.exists()) {
            logger.info("toFile exists,delete then copy file {}", toPath);
            toFile.delete();
        }
        Files.copy(fromFile.toPath(), toFile.toPath());
        //返回文件绝对路径，将路径中“\\”替换为“/”
        return toFile.getAbsolutePath().replace("\\", "/");
    }

    /**
     * 复制特定路径的文件到新的路径
     * 若新路径存在同名文件，不再进行复制操作
     *
     * @param fromFilePath 要复制的文件路径
     * @param toPath       目标文件路径
     * @return 文件路径
     */
    public static String copyFile(String fromFilePath, String toPath) throws IOException {
        File fromFile = new File(fromFilePath);
        File toFilePath = new File(toPath);
        //    检查目标路径是否存在
        if (!toFilePath.exists()) {
            toFilePath.mkdirs();
        }
        // 文件的目标路径为 toPath/fromFileName
        File toFile = new File(toPath + File.separator + fromFile.getName());
        //        检查目标路径下是否有同名文件
        if (toFile.exists()) {
            throw new BaseException(ResponseCode.File_NAME_EXISTS);
        }
        Files.copy(fromFile.toPath(), toFile.toPath());
        //返回文件绝对路径，将路径中“\\”替换为“/”
        return toFile.getAbsolutePath().replace("\\", "/");
    }

    /**
     * 将fork文件夹下的文件批量复制到目标文件夹中
     * 若新路径存在同名文件，会覆盖文件
     *
     * @param forkFromFilePaths fork文件夹路径
     * @param targetFilePath    目标文件夹路径
     * @return 文件夹中的原文件路径以及复制后文件路径
     */
    public static String forkFileCopy(String forkFromFilePaths, String targetFilePath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> forkFromFilePathMap = objectMapper.readValue(forkFromFilePaths, new TypeReference<Map<String, String>>() {
        });
        Map<String, String> myForkFilePaths = new HashMap<>();
        //依次将文件夹中的每个文件进行复制
        for (String forkFromFileName : forkFromFilePathMap.keySet()) {
            String filePath = forkFromFilePathMap.get(forkFromFileName);
            // 这里直接拷贝或者覆盖
            String myForkFilePath = copyOrOverwriteFile(filePath, targetFilePath);
            myForkFilePaths.put(forkFromFileName, myForkFilePath);
        }
        return objectMapper.writeValueAsString(myForkFilePaths);
    }

    /**
     * 生成文件切片地址，并将文件切片数据写入地址中
     *
     * @param uploadFolder 文件夹
     * @param username     用户名
     * @param chunk        文件切片信息
     * @return 文件切片地址
     */
    public static String generatePath(String uploadFolder, String username, TChunkInfoVO chunk) {
        //path=uploadFolder/username/temp/chunk.identifier
        String path = Paths.get(uploadFolder, username, "temp", chunk.getIdentifier()).toString();
        StringBuilder sb = new StringBuilder(path);
        //判断uploadFolder/identifier 路径是否存在，不存在则创建
        if (!Files.isWritable(Paths.get(sb.toString()))) {
            logger.info("path not exist,create path: {}", sb.toString());
            try {
                Files.createDirectories(Paths.get(sb.toString()));
            } catch (IOException e) {
                logger.error("generatePath Failed！create directories Failed, directory path={}", sb.toString());
                logger.error("generatePath fail, error: {}", e);
            }
        }
        //返回路径=uploadFolder/username/temp/chunk.identifier/chunkfilename-chunknumber
        return sb.append("/")
                .append(chunk.getFilename())
                .append("-")
                .append(chunk.getChunkNumber()).toString();
    }

    /**
     * 合并文件夹中的文件
     * 若目标文件已存在，抛出文件已存在异常
     * 不存在的话，过滤掉特定文件名进行排序，然后合并
     *
     * @param file     合并后的目标文件路径
     * @param folder   文件夹路径
     * @param filename 不参与合并的文件名
     * @return 无返回值
     */
    public static void merge(String file, String folder, String filename) {
        try {
            //先判断文件是否存在
            if (fileExists(file)) {
                //文件已存在
                throw new BaseException(ResponseCode.File_NAME_EXISTS);
            } else {
                //不存在的话，进行合并
                Files.createFile(Paths.get(file));

                Files.list(Paths.get(folder))
                        .filter(path -> !path.getFileName().toString().equals(filename))
                        .sorted((o1, o2) -> {
                            String p1 = o1.getFileName().toString();
                            String p2 = o2.getFileName().toString();
                            int i1 = p1.lastIndexOf("-");
                            int i2 = p2.lastIndexOf("-");
                            return Integer.valueOf(p2.substring(i2)).compareTo(Integer.valueOf(p1.substring(i1)));
                        })
                        .forEach(path -> {
                            try {
                                //以追加的形式写入文件
                                Files.write(Paths.get(file), Files.readAllBytes(path), StandardOpenOption.APPEND);
                                //合并后删除该块
                                Files.delete(path);
                            } catch (IOException e) {
                                logger.error("merge failed! 出错文件路径={}", path.getFileName().toString());
                                logger.error("merge file fail, error: {}", e);
                            }
                        });
            }
        } catch (IOException e) {
            logger.error("merge failed! ");
            //合并失败
            logger.error("merge file fail, error: {}", e);
        }
    }

    /**
     * 判断文件是否存在
     *
     * @param file 文件路径
     * @return 文件存在返回true，不存在返回false
     */
    public static boolean fileExists(String file) {
        Path path = Paths.get(file);
        return Files.exists(path, LinkOption.NOFOLLOW_LINKS);
    }

    /**
     * 解压文件，并获取所有组件文件的保存路径，压缩包本身除外
     *
     * @param commonPath 通用路径
     * @param filePath   压缩文件路径
     * @param fileFolder 存放解压后文件的文件夹
     * @param namespace  命名空间
     * @return 组件文件的保存路径map
     */
    public static Map<String, String> unZipFile(String commonPath, String filePath, String fileFolder, String namespace) throws IOException, FileUploadException {
        //解压压缩包到文件所在目录下
        ZipUtil.unZip(new File(filePath), fileFolder);
        //命名空间所在目录
        String nsPath = namespace.replaceAll("\\.", Matcher.quoteReplacement(File.separator));
        String targetPath = Paths.get(commonPath, nsPath).toString();
        List<String> fileNameList = new ArrayList<>();   //用于保存命名空间目录下所有文件的文件名
        Map<String, String> filePathMap = new HashMap<>();
        File nsDirectory = new File(targetPath);  //命名空间目录
        if (!(nsDirectory).exists())
            nsDirectory.mkdirs();
        for (File f : Objects.requireNonNull(nsDirectory.listFiles())) {
            if (f.isFile() && (!f.getName().endsWith(".zip")))
                fileNameList.add(f.getName());
        }
//        若命名空间下存在相同文件
        for (File f : Objects.requireNonNull(new File(fileFolder).listFiles())) {
            if (fileNameList.contains(f.getName())) {
                throw new FileUploadException("已存在相同的文件名：" + f.getName() + ",请改名后重试");
            } else {
                //获取所有组件文件的保存路径，压缩包本身除外
                if (!f.getName().endsWith(".zip")) {
                    String uploadFilePath = FileUtil.copyFile(f.getAbsolutePath(), targetPath);
                    filePathMap.put(f.getName(), uploadFilePath);
                }
            }
        }
        return filePathMap;
    }

    /**
     * 指定文件夹下的所有文件复制到指定目录下
     * 如果存在同名文件则跳过
     *
     * @param source 原文件夹
     * @param target 复制到的目标目录
     * @return checkPointList存放checkPoint的name和真实路径
     */
    public static Map<String, String> copyCheckPoint(File source, File target) throws IOException {
        Map<String, String> checkPointList = new HashMap<>();
        // 判断源目录是不是一个目录
        if (!source.isDirectory()) {
            //如果不是目录就不复制
            throw new BaseException(ResponseCode.File_NOT_DIR);
        }
        //创建目标目录的file对象
        if (!target.exists()) {
            //不存在就创建文件夹
            target.mkdir();
        }
        //如果源文件存在就复制
        if (source.exists()) {
            // 获取源目录下的File对象列表
            File[] files = source.listFiles();
            assert files != null;
            for (File sourceFile : files) {
                //新文件夹的路径
                File file4 = new File(target + File.separator + sourceFile.getName());
//                目标路径存在同名文件则跳过
                if (file4.exists()) {
                    System.out.println("checkPoint:" + file4.getAbsolutePath() + "已存在，跳过");
                } else if (sourceFile.isFile()) {
                    Files.copy(sourceFile.toPath(), file4.toPath());
                    checkPointList.put(file4.getName(), file4.getAbsolutePath());
                }
            }
        }
        return checkPointList;
    }

    /**
     * 判断源目录中的文件是否都存在于目标目录，只比较一层，没有递归
     * 如果出现子文件夹（正常情况下不会出现），则直接跳过比较下一个
     *
     * @param source 源目录
     * @param target 目标目录
     * @return 若源目录中的文件都存在于目标目录，返回true，否则返回false
     */
    public static boolean copyCheckPointCheck(File source, File target) {
        if (!target.exists()) {
            //不存在就证明未保存过checkPoint
            return false;
        }
        //如果源文件存在
        if (source.exists()) {
            // 获取源目录下的File对象列表
            File[] files = source.listFiles();
            assert files != null;
            for (File file2 : files) {
                //新文件夹的路径
                File file4 = new File(target + File.separator + file2.getName());
//                目标路径存在同名文件则跳过
                if (file4.exists()) {
                    System.out.println("checkPoint:" + file4.getAbsolutePath() + "已存在，跳过");
                } else if (file2.isFile()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 将组件文件集合中的文件复制到 targetDir 目录下
     *
     * @param targetDir          目标目录
     * @param componentFilePaths 组件文件路径集合
     * @param filePath           项目默认的文件存放路径
     * @return 无返回值
     */
    public static void copyComponentFiles(String targetDir, Collection componentFilePaths, String filePath) throws Exception {
//        复制实验所需文件到指定下载结果路径下
        for (Object path : componentFilePaths) {
            String componentFilePath = (String) path;
            File fromCodeFile = new File(componentFilePath);
            //去掉原有的文件目录路径，新的文件路径=targetDir/filename
            File toCodeFile = new File(targetDir, componentFilePath.replace(filePath, ""));
            FileUtils.copyFile(fromCodeFile, toCodeFile);
        }
    }

    /**
     * 在目标文件夹中创建新文件
     *
     * @param targetPath 目标文件夹路径
     * @param filename   文件名
     * @return 文件路径
     */
    public static String buildFile(String targetPath, String filename) throws IOException {
        File toFilePath = new File(targetPath);
        //    检查目标路径是否存在
        if (!toFilePath.exists()) {
            toFilePath.mkdirs();
        }
        //文件路径=targetPath/filename
        File toFile = new File(targetPath + File.separator + filename);
        //        检查目标路径下是否有同名文件
        if (toFile.exists()) {
            throw new BaseException(ResponseCode.File_NAME_EXISTS);
        }
        toFile.createNewFile();
        //返回绝对路径，并将路径中的“\\”替换为"/"
        return toFile.getAbsolutePath().replace("\\", "/");
    }

    /**
     * 下载文件，将文件从特定输出流输出
     *
     * @param codeFile     文件路径
     * @param outputStream 输出流
     * @return 无返回值
     */
    public static void downloadFile(File codeFile, OutputStream outputStream) throws IOException {
        byte[] buff = new byte[1024];
        BufferedInputStream bis = null;
        // 读取文件
        bis = new BufferedInputStream(new FileInputStream(codeFile));
        int i = bis.read(buff);
        // 只要能读到，则一直读取
        while (i != -1) {
            // 将文件写出
            outputStream.write(buff, 0, buff.length);
            // 刷出
            outputStream.flush();
            i = bis.read(buff);
        }
    }

    /**
     * 解析JOSN文件中的组件信息
     *
     * @param file JOSN文件
     * @return 解析后的组件
     */
    public static ComponentVO parseJsonFile(MultipartFile file) throws IOException {
        InputStream is = file.getInputStream();
        int size = is.available();
        byte[] buffer = new byte[size];
        is.read(buffer);
        is.close();
        String str = new String(buffer, "UTF-8");

        ObjectMapper objectMapper = new ObjectMapper();
        ComponentVO componentVO = objectMapper.readValue(str, ComponentVO.class);
        return componentVO;
    }

    /**
     * 递归获取文件夹的size
     *
     * @param targetFile
     * @return
     */
    public static double getSize(File targetFile) {
        if (!targetFile.exists())
            return 0;
        long fileSize = 0;
        if (targetFile.isDirectory()) {
            File[] files = targetFile.listFiles();
            for (File file : files)
                fileSize += getSize(file);
        } else {
            fileSize = targetFile.length();
        }
        return fileSize;
    }

    /**
     * 复制特定路径的文件/文件夹到新的路径
     * 若新路径存在同名文件，不再进行复制操作
     *
     * @param fromFilePath 要复制的文件路径
     * @param toPath       目标文件路径
     */
    public static void copyFileOrDir(String fromFilePath, String toPath) throws Exception {
        File fromFile = new File(fromFilePath);
        File toFile = new File(toPath);
        //    检查目标路径是否存在
        if (fromFile.isDirectory()) {
            //在目的地下创建和数据源File名称一样的目录
            String srcFileName = fromFile.getName();
            File newFolder = new File(toFile, srcFileName);
            if (!newFolder.exists()) {
                newFolder.mkdir();
            }
            else{
                throw new BaseException(ResponseCode.File_NAME_EXISTS);
            }
            //获取数据源File下所有文件或者目录的File数组
            File[] listFiles = fromFile.listFiles();
            //遍历该File数组，得到每一个File对象
            for (File file : listFiles) {
                //把该File作为数据源File对象，递归调用复制文件夹的方法
                copyFileOrDir(file.getPath(), newFolder.getPath());
            }
        } else {
            // 文件的目标路径为 toPath/fromFileName
            toFile = new File(toPath + File.separator + fromFile.getName());
            if(!toFile.exists())
                Files.copy(fromFile.toPath(), toFile.toPath());
            else
                throw new BaseException(ResponseCode.File_NAME_EXISTS);
        }
    }

    /**
     * 复制特定路径的文件到新的路径
     * 若新路径存在同名文件，加上uuid之后再复制
     *
     * @param fromFilePath 要复制的文件路径
     * @param toPath       目标文件路径
     */
    public static String copySingleFile(String fromFilePath, String toPath) throws Exception {
        File fromFile = new File(fromFilePath);
        //如果目标路径存在，加上uuid
        File toFile = new File(toPath + File.separator + fromFile.getName());
        String fileName = toFile.getName();
        String initName = fileName.substring(0, fileName.lastIndexOf("."));
        String extension = fileName.substring(fileName.lastIndexOf("."));
        while(toFile.exists())
            toFile = new File(toPath + File.separator + initName  + "_" + UUID.randomUUID().toString() + extension);
        Files.copy(fromFile.toPath(), toFile.toPath());
        return toFile.getAbsolutePath();
    }

    /**
     * 获取文件大小 返回具体的size
     */
    public static String getFormatSize(File file) {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        String wrongSize = "0B";
        long fileSize = FileUtils.sizeOf(file);
        if (fileSize == 0) {
            fileSizeString = wrongSize;
        } else if (fileSize < 1024) {
            fileSizeString = df.format((double) fileSize) + "B";
        } else if (fileSize < 1048576) {
            fileSizeString = df.format((double) fileSize / 1024) + "KB";
        } else if (fileSize < 1073741824) {
            fileSizeString = df.format((double) fileSize / 1048576) + "MB";
        } else {
            fileSizeString = df.format((double) fileSize / 1073741824) + "GB";
        }
        return fileSizeString;
    }
}
