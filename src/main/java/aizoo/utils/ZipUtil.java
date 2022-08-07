package aizoo.utils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZipUtil {

    private static final int BUFFER_SIZE = 2 * 1024;

    /**
     *
     * @param srcDir 文件夹路径
     * @param output 文件输出流
     * @throws IOException 失败会抛出运行时异常
     */
    public static void downloadFiles(String srcDir, String output) throws IOException {
        try {
            //方法1-2：IO字符流下载，用于大文件
            OutputStream out = new FileOutputStream(output);
            File file = new File(srcDir);  //创建文件
            FileInputStream fis = new FileInputStream(file);  //创建文件字节输入流
            BufferedInputStream bis = new BufferedInputStream(fis); //创建文件缓冲输入流
            byte[] buffer = new byte[bis.available()];//从输入流中读取不受阻塞
            bis.read(buffer);//读取数据文件
            bis.close();
            out.write(buffer);//输出数据文件
            out.flush();//释放缓存
            out.close();//关闭输出流

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 压缩成ZIP 方法1
     * @param srcDir 压缩文件夹路径
     * @param output 文件输出流
     * @throws RuntimeException 压缩失败会抛出运行时异常
     */
    public static void codeZip(String srcDir, String output)
            throws RuntimeException{

        long start = System.currentTimeMillis();
        ZipOutputStream zos = null ;
        try {
            zos = new ZipOutputStream(new FileOutputStream(output));
            File sourceFile = new File(srcDir);
            compress(sourceFile,zos,sourceFile.getName());
            long end = System.currentTimeMillis();
            System.out.println("压缩完成，耗时：" + (end - start) +" ms");
        } catch (Exception e) {
            throw new RuntimeException("zip error from ZipUtils",e);
        }finally{
            if(zos != null){
                try {
                    zos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * zip格式
     *
     * @param input  输入流
     * @param output 文件输出流
     * @param name   压缩后的名称
     * @throws Exception
     */
    public static void zip(String input, String output, String name) throws Exception {
        // 输出流置空
        ZipOutputStream out = null;
        try {
            // 1、获取文件对象的上级目录
            String outputDirPath = new File(output).getParent();
            File outputDir = new File(outputDirPath);
            // 如果目录不存在，创建目录
            if (!outputDir.exists())
                outputDir.mkdirs();
            // 2、设置文件输出流
            out = new ZipOutputStream(new FileOutputStream(output));
            File inputFile = new File(input);
            // 3、调用compress方法压缩文件
            compress(inputFile, out, name);
        } catch (Exception e) {
            throw e;
        } finally {
            out.close();
        }
    }

    /**
     * 压缩方法
     *
     * @param sourceFile 源文件
     * @param zos        zip输出流
     * @param name       压缩后的名称
     * @throws Exception
     */
    private static void compress(File sourceFile, ZipOutputStream zos, String name) throws Exception {
        byte[] buf = new byte[BUFFER_SIZE];
        if (sourceFile.isFile()) {
            // 1、向zip输出流中添加一个zip实体，构造器中name为zip实体的文件的名字
            zos.putNextEntry(new ZipEntry(name));
            // 2、copy文件到zip输出流中
            int len;
            FileInputStream in = new FileInputStream(sourceFile);
            while ((len = in.read(buf)) != -1) {
                zos.write(buf, 0, len);
            }
            // Complete the entry
            zos.closeEntry();
            in.close();
        } else {
            File[] listFiles = sourceFile.listFiles();
            if (listFiles == null || listFiles.length == 0) {
                // 3、需要保留原来的文件结构时，需要对空文件夹进行处理
                // 空文件夹的处理
                zos.putNextEntry(new ZipEntry(name + File.separator));
                // 没有文件，不需要文件的copy
                zos.closeEntry();
            } else {
                for (File file : listFiles) {
                    // 4、判断是否需要保留原来的文件结构
                    if (file.getName().endsWith(".zip"))
                        continue;
                    // 注意：file.getName()前面需要带上父文件夹的名字加一斜杠,
                    // 不然最后压缩包中就不能保留原来的文件结构,即：所有文件都跑到压缩包根目录下了
                    compress(file, zos, name + File.separator + file.getName());
                }
            }
        }
    }

    /**
     * ZIP解压
     *
     * @param zipFile 需要解压的文件
     * @param descDir 路径名
     * @throws IOException
     */
    public static void unZip(File zipFile, String descDir) throws IOException {
        File pathFile = new File(descDir);
        if (!pathFile.exists()) {
            pathFile.mkdirs();
        }
        // 1、指定编码，否则压缩包里不能有中文目录
        ZipFile zip = new ZipFile(zipFile, Charset.forName("GBK"));
        for (Enumeration entries = zip.entries(); entries.hasMoreElements(); ) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            String zipEntryName = entry.getName();
            InputStream in = zip.getInputStream(entry);
            String outPath = (Paths.get(descDir, zipEntryName).toString()).replaceAll("\\*", "/");
            File outFile=new File(outPath);
            //2、判断路径是否存在,不存在则创建文件路径
            try {
                //父文件
                File parentFile = new File(outPath.substring(0, outPath.lastIndexOf(File.separator)));
                if (!outFile.exists()) {
                    parentFile.mkdirs();
                }
            } catch (StringIndexOutOfBoundsException e) {
                e.printStackTrace();
            }
            //3、判断文件全路径是否为文件夹,如果是上面已经上传,不需要解压
            if (entry.isDirectory()) {
                continue;
            }
            OutputStream out = new FileOutputStream(outPath);
            byte[] buf1 = new byte[1024];
            int len;
            // 4、输出数据
            while ((len = in.read(buf1)) > 0) {
                out.write(buf1, 0, len);
            }
            in.close();
            out.close();
        }
        zip.close();
        System.out.println("******************解压完毕********************");

    }

    /**
     * 压缩成ZIP
     *
     * @param srcFiles 需要压缩的文件列表
     * @param out      压缩文件输出流
     * @throws RuntimeException 压缩失败会抛出运行时异常
     */
    public static void toZip(List<File> srcFiles, OutputStream out) throws RuntimeException {
        // 获取当前时间作为压缩起始时间
        long start = System.currentTimeMillis();
        // 文件输出流置空
        ZipOutputStream zos = null;
        try {
            // 1、文件输出流
            zos = new ZipOutputStream(out);
            // 2、每次循环都将数组中的文件对象赋给srcFile这个变量
            for (File srcFile : srcFiles) {
                byte[] buf = new byte[BUFFER_SIZE];
                zos.putNextEntry(new ZipEntry(srcFile.getName()));
                int len;
                // 3、创建文件字节输入流对象
                FileInputStream in = new FileInputStream(srcFile);
                while ((len = in.read(buf)) != -1) {
                    zos.write(buf, 0, len);
                }
                zos.closeEntry();
                in.close();
            }
            // 获取当前时间作为压缩结束时间
            long end = System.currentTimeMillis();
            System.out.println("压缩完成，耗时：" + (end - start) + " ms");
        } catch (Exception e) {
            // 压缩失败
            throw new RuntimeException("zip error from ZipUtils", e);
        } finally {
            if (zos != null) {
                try {
                    zos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
