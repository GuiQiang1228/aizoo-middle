package aizoo.service;

import aizoo.Client;
import aizoo.domain.*;
import aizoo.elasticObject.ElasticDatasource;
import aizoo.elasticRepository.DatasourceRepository;
import aizoo.repository.*;
import aizoo.utils.*;
import aizoo.viewObject.mapper.DatasourceVOEntityMapper;
import aizoo.viewObject.object.DatasourceVO;
import aizoo.viewObject.object.TFileInfoVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;

@Service("DatasourceService")
public class DatasourceService {

    @Value("${file.path}")
    String file_path;

    @Autowired
    DatasourceDAO datasourceDAO;

    @Autowired
    NamespaceDAO namespaceDAO;

    @Autowired
    UserDAO userDAO;

    @Autowired
    DatatypeDAO datatypeDAO;

    @Autowired
    DatasourceOutputParameterDAO datasourceOutputParameterDAO;

    @Autowired
    Client client;

    @Autowired
    ProjectService projectService;

    @Autowired
    DatasourceRepository datasourceRepository;

    @Autowired
    NamespaceService namespaceService;

    private static final Logger logger = LoggerFactory.getLogger(DatasourceService.class);

    /**
     * 数据资源的上传方法
     * @param fileInfo 文件信息,前端MD5上传文件
     * @param datasourceVO  DataSource组织成VO的信息
     * @throws Exception
     */
    @Transactional
    public void uploadDatasource(TFileInfoVO fileInfo, DatasourceVO datasourceVO) throws Exception {
        logger.info("Start upload Datasource");

        // 将数据资源由VO转为entity，再去更新数据库
        Datasource datasource = DatasourceVOEntityMapper.MAPPER.datasourceVO2Datasource(datasourceVO,
                datasourceDAO, namespaceDAO, userDAO, datatypeDAO);

        String filename = fileInfo.getName();
        logger.info("fileInfo: {}",fileInfo.toString());
        logger.info("datasourceVO: {}",datasourceVO.toString());
//      该数据资源临时文件夹下的具体路径, file_path是服务器上存放用户上传文件的起始地址，以69为例，此时path为 /data/aizoo-slurm/project/aizoo/aizoo-back-interpreter/files/
        // file是具体到temp文件夹里的具体文件,具体路径为{file.path}/{username}/temp/{identifier}/{filename}
        // tempFolder是temp文件夹的路径，具体路径为{file.path}/{username}/temp/{identifier}
//      根目录之后按照用户名来存放文件，temp是为了将用户上传的切片文件存放的文件夹，文件合并完之后再放到对应的DataSource文件夹里, UniqueIdentifier是附件MD5标识
        String file = Paths.get(file_path, datasourceVO.getUsername(), "temp", fileInfo.getUniqueIdentifier(), filename).toString();
        String tempFolder = Paths.get(file_path, datasourceVO.getUsername(), "temp", fileInfo.getUniqueIdentifier()).toString();
//        由命名空间得到目标存放路径,namespacePath是用户选择的存放地址,target将namespacePath前加上服务器根目录路径
        String namespacePath = datasourceVO.getNamespace().replaceAll("\\.", Matcher.quoteReplacement(File.separator));
        String targetPath = Paths.get(file_path, namespacePath).toString();
        // 保存改数据资源的绝对路径
        datasource.setPath(Paths.get(targetPath, filename).toString().replace("\\", "/"));
        try {
            logger.info("Start merge file,tempFolder: {}",tempFolder);
//            进行在临时目录下切片文件的合并操作
            FileUtil.merge(file, tempFolder, filename);
//            将数据资源从临时目录复制到目标存放路径下
            FileUtil.copyFile(file, targetPath);
//            数据资源设置输出
            for (DatasourceOutputParameter datasourceOutputParameter : datasource.getDatasourceOutputParameters()) {
                datasourceOutputParameter.setDatasource(datasource);
            }
            datasourceDAO.save(datasource);
        } catch (Exception e) {
            //删除目录中本次上传的文件
            File f = new File(file);
            FileUtil.deleteFile(f);
            logger.error("上传文件失败，删除目录中本次上传的文件");
            logger.error("上传文件失败,错误信息: ",e);
            throw e;
        } finally {
            logger.info("Start delete temp folder and file");
            //将临时目录下的文件和文件夹删除
            String tempPath = Paths.get(file_path, datasourceVO.getUsername(), "temp", fileInfo.getUniqueIdentifier()).toString();
            FileUtils.deleteDirectory(new File(tempPath));
        }
        logger.info("End upload Datasource");

    }

    /**
     * 数据资源的云上传方法
     * *
     * @param datasourceVO  DataSource组织成VO的信息
     * @throws Exception
     */
    @Transactional
    public void uploadDatasource(DatasourceVO datasourceVO) throws Exception {

        // 将数据资源由VO转为entity，再去更新数据库
        Datasource datasource = DatasourceVOEntityMapper.MAPPER.datasourceVO2Datasource(datasourceVO,
                datasourceDAO, namespaceDAO, userDAO, datatypeDAO);
        for (DatasourceOutputParameter datasourceOutputParameter : datasource.getDatasourceOutputParameters()) {
                datasourceOutputParameter.setDatasource(datasource);
        }
        datasourceDAO.save(datasource);

    }

    /**
     * 根据用户名和数据资源的私有/公开属性从数据库中查找改用户拥有的数据资源
     * @param username 用户名
     * @param privacy  私有/公开
     * @return DataSource类型的List，DataSource类型结构可见DataSource.java
     */
    public List<Datasource> getDatasource(String username, String privacy) {
        logger.info("Start get Datasource");
        List<Datasource> datasourceList;

        //根据传进来的privacy是public还是private返回对应的List
        if (privacy.equals("public")) {
            datasourceList = datasourceDAO.findByUserUsernameNotAndPrivacyAndPathIsNotNull(username, privacy);
        } else // 即private
            datasourceList = datasourceDAO.findByUserUsernameAndPathIsNotNull(username);
        logger.info("findByUser... username: {}",username);
        logger.info("findByUser... privacy: {}",privacy);
        logger.info("获取到的datasourceList: {}",datasourceList.toString());
        logger.info("End get Datasource");
        return datasourceList;
    }

    /**
     * 根据用户名和数据资源的私有/公开属性和输入的搜索关键字从数据库中查找数据资源
     * @param username  用户名
     * @param privacy   private/public
     * @param keyword   搜索关键字
     * @return  DataSource类型的List
     */
    public List<Datasource> getDatasourceByKeyword(String username, String privacy, String keyword) {
        logger.info("Start get Datasource By Keyword");
        List<Datasource> datasourceList;
        if (privacy.equals("public")) {
            datasourceList = datasourceDAO.findByUserUsernameNotAndPrivacyAndTitleLikeAndPathIsNotNull(username, privacy, "%" + keyword + "%");
        } else
            datasourceList = datasourceDAO.findByUserUsernameAndTitleLikeAndPathIsNotNull(username, "%" + keyword + "%");
        logger.info("findByUser... username: {}",username);
        logger.info("findByUser... privacy: {}",privacy);
        logger.info("findByUser... keyword: {}",keyword);
        logger.info("获取到的datasourceList By Keyword: {}",datasourceList.toString());
        logger.info("End get Datasource By Keyword");
        return datasourceList;
    }

    /**
     * 删除数据资源方法
     * @param id 根据数据资源ID定位数据库中的，id由前端传参
     * @throws Exception
     */
    @Transactional
    public void deleteDatasource(Long id) throws Exception {
        logger.info("Start delete Datasource");
        // 根据ID去数据库中查找对应的数据资源，如果没有找到抛出一个notfound异常
        Datasource datasource = datasourceDAO.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
        logger.info("findById DatasourceId: {}",id);
        datasource.setNamespace(null);
        datasource.setUser(null);
//        输出的datasource置空
        for (DatasourceOutputParameter datasourceOutputParameter : datasource.getDatasourceOutputParameters()) {
            datasourceOutputParameter.setDatasource(null);
            datasourceOutputParameterDAO.delete(datasourceOutputParameter);
        }

        // 对使用了该DataSource的project做一个关联删除操作
        List<Project> projects = datasource.getProjects();
        List<Long[]> removeList = new ArrayList<>();

        // 将使用了该资源的project以list方式存储
        for (Project project : projects) {
            removeList.add(new Long[]{project.getId(), datasource.getId()});
        }

        projectService.removeProjectDatasourceRelation(removeList);
        // 删除完project相关使用后开始删除DataSource
        datasourceDAO.delete(datasource);
        logger.info("End delete Datasource");
        //删除es对应索引
        Optional<ElasticDatasource> optional = datasourceRepository.findById(datasource.getId().toString());
        if(optional.isPresent())
            datasourceRepository.delete(optional.get());
    }

    /**
     * 修改上传管理页面用户拥有的数据资源的描述信息，也包括了输出参数的描述
     * @param datasourceVO  DataSourceVO类型参数，其中desc描述信息为用户修改后的新信息
     * @throws JsonProcessingException
     */
    @Transactional
    public void updateDesc(DatasourceVO datasourceVO) throws JsonProcessingException {
        logger.info("Start update Desc");
        logger.info("datasourceVO: {}",datasourceVO.toString());
        // 通过DataSource的id来修改数据库中的描述和样例
        datasourceDAO.updateDesc(datasourceVO.getId(), datasourceVO.getDescription());
        datasourceDAO.updateExample(datasourceVO.getId(), datasourceVO.getExample());

        //通过for循环依次更新DataSource的输出参数的描述信息
        for (Map<String, Object> datasourceOutput : datasourceVO.getOutputs()) {
            Object id = datasourceOutput.get("id");
            String desc = (String) datasourceOutput.get("description");
            datasourceOutputParameterDAO.updateDesc(Long.valueOf(String.valueOf(id)), desc);
        }
        logger.info("End update Desc");
    }

    /**
     * forkDatasource（该操作暂时只在fork整张图的时候进行）
     *
     * @param targetUser 当前用户
     * @param sourceDatasource 被fork的数据集
     * @return
     * @throws Exception
     */
    @Transactional
    public Datasource forkDatasource(User targetUser, Datasource sourceDatasource) throws Exception {
        Datasource targetDatasource = new Datasource();
        logger.info("buildNewDatasource start,sourceDatasourceId={},  finalTargetDatasourceId={}", sourceDatasource.getId(), targetDatasource.getId());
        Long sourceDatasourceId = sourceDatasource.getId();
        String targetNamespace = "";
//          新建命名空间 username.type.version(fork+forkFromUser+number).forkfrom的命名空间
        targetNamespace = namespaceService.forkRegisterNamespace(sourceDatasource.getId(), false, targetUser, "DATASOURCE");
        Namespace namespace = namespaceDAO.findByNamespace(targetNamespace);

        logger.info("buildNewDatasource forkRegisterNamespace complete,sourceDatasourceId={},  finalTargetDatasourceId={}", sourceDatasource.getId(), targetDatasource.getId());
//        set 目标数据集的一系列信息
        targetDatasource.setNamespace(namespace);
        targetDatasource.setDatabaseName(sourceDatasource.getDatabaseName());
        targetDatasource.setExample(sourceDatasource.getExample());
        targetDatasource.setHost(sourceDatasource.getHost());
        targetDatasource.setPassword(sourceDatasource.getPassword());
        targetDatasource.setPort(sourceDatasource.getPort());
        targetDatasource.setSqlUsername(sourceDatasource.getSqlUsername());
        targetDatasource.setProjects(new ArrayList<>());
        targetDatasource.setName(sourceDatasource.getName());
        targetDatasource.setTitle(sourceDatasource.getTitle());
        targetDatasource.setDescription(sourceDatasource.getDescription());
        targetDatasource.setUser(targetUser);
        targetDatasource.setPath(sourceDatasource.getPath());
        targetDatasource.setPrivacy("private");
        logger.info("buildNewDatasource setValues complete,sourceDatasourceId={},  finalTargetDatasourceId={}", sourceDatasource.getId(), targetDatasource.getId());

//      set它的output
        targetDatasource.setDatasourceOutputParameters(buildNewOutputList(sourceDatasource, targetDatasource));
        logger.info("buildNewDatasource setOutputs complete,sourceDatasourceId={},  finalTargetDatasourceId={}", sourceDatasource.getId(), targetDatasource.getId());

        datasourceDAO.save(targetDatasource);

        //设置fork信息
        targetDatasource.setForkFrom(sourceDatasource);
        if(sourceDatasource.getForkBy() == null)
            sourceDatasource.setForkBy(new ArrayList<>());
        sourceDatasource.getForkBy().add(targetDatasource);
        datasourceDAO.save(sourceDatasource);
        datasourceDAO.save(targetDatasource);

        return targetDatasource;
    }

    /**
     * 重新组织forkDatasource的output
     *
     * @param sourceDatasource 待fork的datasource
     * @param finalTargetDatasource fork之后的新datasource
     * @return 组织之后的outputList
     */
    public List<DatasourceOutputParameter> buildNewOutputList(Datasource sourceDatasource, Datasource finalTargetDatasource) {
        // 新的outputs
        List<DatasourceOutputParameter> newDatasourceOutputParameters = new ArrayList<>();
        // 旧的outputs
        List<DatasourceOutputParameter> datasourceOutputParameters = sourceDatasource.getDatasourceOutputParameters();
        for (DatasourceOutputParameter datasourceOutputParameter : datasourceOutputParameters) {
            DatasourceOutputParameter newDatasourceOutputParameter = new DatasourceOutputParameter(
                    datasourceOutputParameter.getParameter(), finalTargetDatasource, datasourceOutputParameter.getParameterIoType());
            datasourceOutputParameterDAO.save(newDatasourceOutputParameter);
            newDatasourceOutputParameters.add(newDatasourceOutputParameter);
        }
        return newDatasourceOutputParameters;
    }

}
