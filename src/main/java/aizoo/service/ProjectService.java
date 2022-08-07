package aizoo.service;

import aizoo.domain.*;
import aizoo.elasticObject.ElasticProject;
import aizoo.elasticRepository.ProjectRepository;
import aizoo.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.*;

@Service("ProjectService")
public class ProjectService {
    @Autowired
    ProjectDAO projectDAO;

    @Autowired
    UserDAO userDAO;

    @Autowired
    ApplicationDAO applicationDAO;

    @Autowired
    ServiceDAO serviceDAO;

    @Autowired
    ServiceJobDAO serviceJobDAO;

    @Autowired
    GraphDAO graphDAO;

    @Autowired
    ComponentDAO componentDAO;

    @Autowired
    ExperimentJobDAO experimentJobDAO;

    @Autowired
    DatasourceDAO datasourceDAO;

    @Autowired
    CodeDAO codeDAO;

    @Autowired
    MirrorDAO mirrorDAO;

    @Autowired
    MirrorJobDAO mirrorJobDAO;

    @Autowired
    ProjectFileService projectFileService;

    @Autowired
    ProjectRepository projectRepository;

    private static final Logger logger = LoggerFactory.getLogger(DatasourceService.class);

    /**
     *新建project方法
     * @param projectName  项目名称
     * @param username  用户名
     * @param privacy   private/public
     * @param desc      项目描述
     * @return  返回新建后的project
     */
    public Project createProject(String projectName, String username, String privacy, String desc) {
        logger.info("Start Create Project,username: {}, projectName: {}", username, projectName);
        // 根据username找到对应用户的相关信息
        User user = userDAO.findByUsername(username);
        // 新建一个project类型的变量，并建立application等属性初始化
        Project project = new Project(projectName, user, privacy, desc);
        project.setApplications(new ArrayList<>());
        project.setServiceJobs(new ArrayList<>());
        project.setComponents(new ArrayList<>());
        project.setDatasourceList(new ArrayList<>());
        project.setExperimentJobs(new ArrayList<>());
        project.setGraphs(new ArrayList<>());
        project.setServiceJobs(new ArrayList<>());
        project.setCodes(new ArrayList<>());
        project.setMirrors(new ArrayList<>());
        project.setMirrorJobs(new ArrayList<>());
        // 对新建的项目进行保存进数据库
        projectDAO.save(project);
        logger.info("End Create Project");
        return project;
    }

    /**
     * 删除project方法
     * @param id  前端传的project ID
     * @throws Exception
     */
    @Transactional
    public void deleteProject(long id) throws Exception {
        logger.info("Start Delete Project,ProjectId: {}", id);
        // 根据id找到对应的project，若没有找到抛出一个异常
        Project project = projectDAO.findById(id).orElseThrow(() -> new EntityNotFoundException());

        // 将project中的application，serviceJob等以list的形式循环删除
        List<Application> applicationList = project.getApplications();
        List<aizoo.domain.Service> serviceList = project.getServices();
        List<ServiceJob> serviceJobList = project.getServiceJobs();
        List<Graph> graphList = project.getGraphs();
        List<ExperimentJob> experimentJobList = project.getExperimentJobs();
        List<Datasource> datasourceList = project.getDatasourceList();
        List<Component> componentList = project.getComponents();
        List<Code> codeList = project.getCodes();
        List<Mirror> mirrorList = project.getMirrors();
        List<MirrorJob> mirrorJobList = project.getMirrorJobs();
        List<ProjectFile> projectFileList = project.getProjectFiles();
        for (Application application : applicationList)
            projectDAO.deleteApplicationFromProject(id, application.getId());
        for (aizoo.domain.Service service : serviceList)
            projectDAO.deleteServiceFromProject(id, service.getId());
        for (ServiceJob serviceJob : serviceJobList)
            projectDAO.deleteServiceJobFromProject(id, serviceJob.getId());
        for (Graph graph : graphList)
            projectDAO.deleteGraphFromProject(id, graph.getId());
        for (ExperimentJob experimentJob : experimentJobList)
            projectDAO.deleteExperimentJobFromProject(id, experimentJob.getId());
        for (Datasource datasource : datasourceList)
            projectDAO.deleteDatasourceFromProject(id, datasource.getId());
        for (Component component : componentList)
            projectDAO.deleteComponentFromProject(id, component.getId());
        for (Code code : codeList)
            projectDAO.deleteCodeFromProject(id, code.getId());
        for (Mirror mirror : mirrorList)
            projectDAO.deleteMirrorFromProject(id, mirror.getId());
        for (MirrorJob mirrorJob : mirrorJobList)
            projectDAO.deleteMirrorJobFromProject(id, mirrorJob.getId());
        while (projectFileList.size() > 0) {
            ProjectFile projectFile = projectFileList.get(0);
            projectFileService.deleteFile(projectFile.getId());
        }
        projectDAO.delete(project);
        logger.info("End Delete Project");
        //删除对应es索引
        Optional<ElasticProject> optional = projectRepository.findById(project.getId().toString());
        if(optional.isPresent())
            projectRepository.delete(optional.get());
    }

    /**
     * 修改项目信息
     * @param username  用户名
     * @param id        需要修改的project ID
     * @param name      修改后的项目名字
     * @param privacy   私有性
     * @param desc      修改后的项目描述
     * @return          project信息是否修改成功的bool变量
     */
    public boolean modifyProject(String username, long id, String name, String privacy, String desc) {
        logger.info("Start Modify Project,username: {}, projectId: {}", username, id);
        // 根据项目ID查找数据库中对应实体，若是查找失败，则抛出异常
        Project project = projectDAO.findById(id).orElseThrow(() -> new EntityNotFoundException());

        //判断传进来的username用户名是否等于该project的拥有者
        if (!username.equals(project.getUser().getUsername()))
            return false;
        //修改相关信息并保存到数据库中
        project.setName(name);
        project.setPrivacy(privacy);
        project.setDescription(desc);
        projectDAO.save(project);
        logger.info("End Modify Project");
        return true;
    }

    /**
     * 向项目中拉取资源
     * @param username 用户名
     * @param project   要拉取资源的项目
     * @param type  拉取资源类型
     * @param contentId 拉取资源ID
     * @return      project拉取资源是否成功的bool变量
     */
    @Transactional
    public boolean projectAddContent(String username, Project project, String type, long contentId) {
        logger.info("Start Add Content into Project,username: {}, type: {}, contentId: {}", username, type, contentId);
        //  根据传进来的不同type拉取不同的资源
        if (type.equals("application")) {
            Application application = applicationDAO.findById(contentId).orElseThrow(() -> new EntityNotFoundException());
            if (!username.equals(application.getUser().getUsername()))
                return false;
            projectDAO.addApplicationForProject(project.getId(), contentId);
        } else if (type.equals("service")) {
            aizoo.domain.Service service = serviceDAO.findById(contentId).orElseThrow(() -> new EntityNotFoundException());
            if (!username.equals(service.getUser().getUsername()))
                return false;
            projectDAO.addServiceForProject(project.getId(), contentId);
        } else if (type.equals("serviceJob")) {
            ServiceJob serviceJob = serviceJobDAO.findById(contentId).orElseThrow(() -> new EntityNotFoundException());
            if (!username.equals(serviceJob.getUser().getUsername()))
                return false;
            projectDAO.addServiceJobForProject(project.getId(), contentId);
        } else if (type.equals("component")) {
            Component component = componentDAO.findById(contentId).orElseThrow(() -> new EntityNotFoundException());
            if (!username.equals(component.getUser().getUsername()))
                return false;
            projectDAO.addComponentForProject(project.getId(), contentId);
        } else if (type.equals("graph")) {
            Graph graph = graphDAO.findById(contentId).orElseThrow(() -> new EntityNotFoundException());
            if (!username.equals(graph.getUser().getUsername()))
                return false;
            projectDAO.addGraphForProject(project.getId(), contentId);
        } else if (type.equals("experimentJob")) {
            ExperimentJob experimentJob = experimentJobDAO.findById(contentId).orElseThrow(() -> new EntityNotFoundException());
            if (!username.equals(experimentJob.getUser().getUsername()))
                return false;
            projectDAO.addExperimentJobForProject(project.getId(), contentId);
        } else if (type.equals("datasource")) {
            Datasource datasource = datasourceDAO.findById(contentId).orElseThrow(() -> new EntityNotFoundException());
            if (!username.equals(datasource.getUser().getUsername()))
                return false;
            projectDAO.addDatasourceForProject(project.getId(), contentId);
        } else if (type.equals("code")) {
            Code code = codeDAO.findById(contentId).orElseThrow(() -> new EntityNotFoundException());
            if (!username.equals(code.getUser().getUsername()))
                return false;
            projectDAO.addCodeForProject(project.getId(), contentId);
        } else if (type.equals("mirror")) {
            Mirror mirror = mirrorDAO.findById(contentId).orElseThrow(() -> new EntityNotFoundException());
            if (!username.equals(mirror.getUser().getUsername()))
                return false;
            projectDAO.addMirrorForProject(project.getId(), contentId);
        } else if (type.equals("mirrorJob")) {
            MirrorJob mirrorJob = mirrorJobDAO.findById(contentId).orElseThrow(() -> new EntityNotFoundException());
            if (!username.equals(mirrorJob.getUser().getUsername()))
                return false;
            projectDAO.addMirrorJobForProject(project.getId(), contentId);
        }
        logger.info("End Add Content into Project");
        return true;
    }

    /**
     * 在项目中删除资源，字段表示含义同拉取资源
     * propagation = Propagation.REQUIRES_NEW
     * 如果当前事务存在，则挂起当前事务，并开启新事务，
     * 如果报错了，只会回滚本事务，不影响挂起事务
     * 若本事务的操作结果，对挂起事务有影响，有可能上层事务也跟着回滚
     * @param username  用户名
     * @param projectId 要删除资源的project ID
     * @param type      要删除资源的type
     * @param contentId 删除资源的ID信息
     * @return
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean projectDeleteContent(String username, long projectId, String type, long contentId) {
        logger.info("Start Delete Content form Project");
        // 根据ID找到对应的project，若查找失败则返回异常
        Project project = projectDAO.findById(projectId).orElseThrow(() -> new EntityNotFoundException());
        if (!username.equals(project.getUser().getUsername()))
            return false;

        // 根据不同的type执行删除函数
        if (type.equals("application")) {
            Application application = applicationDAO.findById(contentId).orElseThrow(() -> new EntityNotFoundException());
            if (!username.equals(application.getUser().getUsername()))
                return false;
            projectDAO.deleteApplicationFromProject(projectId, contentId);
        } else if (type.equals("service")) {
            aizoo.domain.Service service = serviceDAO.findById(contentId).orElseThrow(() -> new EntityNotFoundException());
            if (!username.equals(service.getUser().getUsername()))
                return false;
            projectDAO.deleteServiceFromProject(projectId, contentId);
        } else if (type.equals("serviceJob")) {
            ServiceJob serviceJob = serviceJobDAO.findById(contentId).orElseThrow(() -> new EntityNotFoundException());
            if (!username.equals(serviceJob.getUser().getUsername()))
                return false;
            projectDAO.deleteServiceJobFromProject(projectId, contentId);
        } else if (type.equals("component")) {
            Component component = componentDAO.findById(contentId).orElseThrow(() -> new EntityNotFoundException());
            if (!username.equals(component.getUser().getUsername()))
                return false;
            projectDAO.deleteComponentFromProject(projectId, contentId);
        } else if (type.equals("graph")) {
            Graph graph = graphDAO.findById(contentId).orElseThrow(() -> new EntityNotFoundException());
            if (!username.equals(graph.getUser().getUsername()))
                return false;
            projectDAO.deleteGraphFromProject(projectId, contentId);
        } else if (type.equals("experimentJob")) {
            ExperimentJob experimentJob = experimentJobDAO.findById(contentId).orElseThrow(() -> new EntityNotFoundException());
            if (!username.equals(experimentJob.getUser().getUsername()))
                return false;
            projectDAO.deleteExperimentJobFromProject(projectId, contentId);
        } else if (type.equals("datasource")) {
            Datasource datasource = datasourceDAO.findById(contentId).orElseThrow(() -> new EntityNotFoundException());
            if (!username.equals(datasource.getUser().getUsername()))
                return false;
            projectDAO.deleteDatasourceFromProject(projectId, contentId);
        } else if (type.equals("code")) {
            Code code = codeDAO.findById(contentId).orElseThrow(() -> new EntityNotFoundException());
            if (!username.equals(code.getUser().getUsername()))
                return false;
            projectDAO.deleteCodeFromProject(projectId, contentId);
        } else if (type.equals("mirror")) {
            // 不同用户的公共镜像可以共享，删除时不需要判断此镜像是否为当前用户发布
            projectDAO.deleteMirrorFromProject(projectId, contentId);
        } else if (type.equals("mirrorJob")) {
            MirrorJob mirrorJob = mirrorJobDAO.findById(contentId).orElseThrow(() -> new EntityNotFoundException());
            if (!username.equals(mirrorJob.getUser().getUsername()))
                return false;
            projectDAO.deleteMirrorJobFromProject(projectId, contentId);
        }
        logger.info("End Delete Content form Project");
        return true;
    }

    /**
     * 在项目中增删内容
     * @param username 用户名
     * @param projectId 要增删的project ID
     * @param type      增删内容的内容
     * @param contentIdList 具体的增删组件ID
     * @return  是否增删成功的bool变量
     */
    @Transactional
    public boolean projectModifyContent(String username, long projectId, String type, List<Integer> contentIdList) {
        logger.info("Start Modify Project Content,username: {}, projectId: {}", username, projectId);
        // 根据id找到对应的project，查找失败抛出异常
        Project project = projectDAO.findById(projectId).orElseThrow(() -> new EntityNotFoundException());

        //判断传进来的username用户名是否等于该project的拥有者
        if (!username.equals(project.getUser().getUsername()))
            return false;
        // 根据type值调用不同的modify方法
        if (type.equals("application")) {
            modifyApplicationsOfProject(projectId, contentIdList);
        } else if (type.equals("service")) {
            modifyServicesOfProject(projectId, contentIdList);
        } else if (type.equals("serviceJob")) {
            modifyServiceJobsOfProject(projectId, contentIdList);
        } else if (type.equals("component")) {
            modifyComponentsOfProject(projectId, contentIdList);
        } else if (type.equals("graph")) {
            modifyGraphsOfProject(projectId, contentIdList);
        } else if (type.equals("experimentJob")) {
            modifyExperimentJobsOfProject(projectId, contentIdList);
        } else if (type.equals("datasource")) {
            modifyDatasourceOfProject(projectId, contentIdList);
        } else if (type.equals("code")) {
            modifyCodesOfProject(projectId, contentIdList);
        } else if (type.equals("mirror")) {
            modifyMirrorsOfProject(projectId, contentIdList);
        } else if (type.equals("mirrorJob")) {
            modifyMirrorJobsOfProject(projectId, contentIdList);
        }
        logger.info("End Modify Project Content");
        return true;
    }


    /**
     * 解除map中project和application的关系
     * 开启事务，一次报错全部回滚，若事务嵌套，则继承该事务
     *
     * @param projectId2ApplicationIdList   要解除关系的application的ID List
     */
    @Transactional
    public void removeProjectApplicationRelation(List<Long[]> projectId2ApplicationIdList) {
        // 循环遍历依次解除list中的application与project的关系
        for (Long[] projectId2ApplicationId : projectId2ApplicationIdList) {
            long projectId = projectId2ApplicationId[0];
            long applicationId = projectId2ApplicationId[1];
            projectDAO.deleteApplicationFromProject(projectId, applicationId);
        }
    }

    /**
     * 解除map中project和experimentJob的关系
     * 开启事务，一次报错全部回滚，若事务嵌套，则继承该事务
     *
     * @param projectId2ExperimentJobIdList 要解除关系的experimentJob的ID List
     */
    @Transactional
    public void removeProjectExperimentJobRelation(List<Long[]> projectId2ExperimentJobIdList) {
        for (Long[] projectId2ExperimentJobId : projectId2ExperimentJobIdList) {
            long projectId = projectId2ExperimentJobId[0];
            long experimentJobId = projectId2ExperimentJobId[1];
            projectDAO.deleteExperimentJobFromProject(projectId, experimentJobId);
        }
    }

    /**
     * 解除map中project和graph的关系
     * 开启事务，一次报错全部回滚，若事务嵌套，则继承该事务
     *
     * @param projectId2GraphIdList 要解除关系的graph的ID List
     */
    @Transactional
    public void removeProjectGraphRelation(List<Long[]> projectId2GraphIdList) {
        for (Long[] projectId2GraphId : projectId2GraphIdList) {
            long projectId = projectId2GraphId[0];
            long graphId = projectId2GraphId[1];
            projectDAO.deleteGraphFromProject(projectId, graphId);
        }
    }

    /**
     * 解除map中project和datasource的关系
     * 开启事务，一次报错全部回滚，若事务嵌套，则继承该事务
     *
     * @param projectId2DatasourceIdList    要解除关系的DataSource的ID List
     */
    @Transactional
    public void removeProjectDatasourceRelation(List<Long[]> projectId2DatasourceIdList) {
        for (Long[] projectId2DatasourceId : projectId2DatasourceIdList) {
            long projectId = projectId2DatasourceId[0];
            long datasourceId = projectId2DatasourceId[1];
            projectDAO.deleteDatasourceFromProject(projectId, datasourceId);
        }
    }

    /**
     * 解除map中project和service的关系
     * 开启事务，一次报错全部回滚，若事务嵌套，则继承该事务
     *
     * @param projectId2ServiceIdList   要解除关系的service的ID List
     */
    @Transactional
    public void removeProjectServiceRelation(List<Long[]> projectId2ServiceIdList) {
        for (Long[] projectId2ServiceId : projectId2ServiceIdList) {
            long projectId = projectId2ServiceId[0];
            long serviceId = projectId2ServiceId[1];
            projectDAO.deleteServiceFromProject(projectId, serviceId);
        }
    }

    /**
     * 解除map中project和serviceJob的关系
     * 开启事务，一次报错全部回滚，若事务嵌套，则继承该事务
     *
     * @param projectId2ServiceJobIdList    要解除关系的service Job的ID List
     */
    @Transactional
    public void removeProjectServiceJobRelation(List<Long[]> projectId2ServiceJobIdList) {
        for (Long[] projectId2ServiceJobId : projectId2ServiceJobIdList) {
            long projectId = projectId2ServiceJobId[0];
            long serviceJobId = projectId2ServiceJobId[1];
            projectDAO.deleteServiceJobFromProject(projectId, serviceJobId);
        }
    }

    /**
     * 解除map中project和component的关系
     * 开启事务，一次报错全部回滚，若事务嵌套，则继承该事务
     *
     * @param projectId2ComponentIdList 要解除关系的component的ID List
     */
    @Transactional
    public void removeProjectComponentRelation(List<Long[]> projectId2ComponentIdList) {
        for (Long[] projectId2ComponentId : projectId2ComponentIdList) {
            long projectId = projectId2ComponentId[0];
            long componentId = projectId2ComponentId[1];
            projectDAO.deleteComponentFromProject(projectId, componentId);
        }
    }

    /**
     * 解除map中project和code的关系
     * 开启事务，一次报错全部回滚，若事务嵌套，则继承该事务
     *
     * @param projectId2CodeIdList 要解除关系的code的ID List
     */
    @Transactional
    public void removeProjectCodeRelation(List<Long[]> projectId2CodeIdList) {
        for (Long[] projectId2CodeId : projectId2CodeIdList) {
            long projectId = projectId2CodeId[0];
            long codeId = projectId2CodeId[1];
            projectDAO.deleteCodeFromProject(projectId, codeId);
        }
    }

    /**
     * 解除map中project和mirror的关系
     * 开启事务，一次报错全部回滚，若事务嵌套，则继承该事务
     *
     * @param projectId2MirrorIdList 要解除关系的mirror的ID List
     */
    @Transactional
    public void removeProjectMirrorRelation(List<Long[]> projectId2MirrorIdList) {
        for (Long[] projectId2MirrorId : projectId2MirrorIdList) {
            long projectId = projectId2MirrorId[0];
            long mirrorId = projectId2MirrorId[1];
            projectDAO.deleteMirrorFromProject(projectId, mirrorId);
        }
    }

    /**
     * 解除map中project和mirrorJob的关系
     * 开启事务，一次报错全部回滚，若事务嵌套，则继承该事务
     *
     * @param projectId2MirrorJobIdList 要解除关系的mirrorJob的ID List
     */
    @Transactional
    public void removeProjectMirrorJobRelation(List<Long[]> projectId2MirrorJobIdList) {
        for (Long[] projectId2MirrorJobId : projectId2MirrorJobIdList) {
            long projectId = projectId2MirrorJobId[0];
            long mirrorJobId = projectId2MirrorJobId[1];
            projectDAO.deleteMirrorJobFromProject(projectId, mirrorJobId);
        }
    }


    /**
     * 建立map中project和application的关系
     * 开启事务，一次报错全部回滚，若事务嵌套，则继承该事务
     *
     * @param projectId2ApplicationIdList   要建立关系的application的ID List
     */
    @Transactional
    public void addProjectApplicationRelation(List<Long[]> projectId2ApplicationIdList) {
        for (Long[] projectId2ApplicationId : projectId2ApplicationIdList) {
            long projectId = projectId2ApplicationId[0];
            long applicationId = projectId2ApplicationId[1];
            projectDAO.addApplicationForProject(projectId, applicationId);
        }
    }

    /**
     * 建立map中project和experimentJob的关系
     * 开启事务，一次报错全部回滚，若事务嵌套，则继承该事务
     *
     * @param projectId2ExperimentJobIdList 要建立关系的experimentJob的ID List
     */
    @Transactional
    public void addProjectExperimentJobRelation(List<Long[]> projectId2ExperimentJobIdList) {
        for (Long[] projectId2ExperimentJobId : projectId2ExperimentJobIdList) {
            long projectId = projectId2ExperimentJobId[0];
            long experimentJobId = projectId2ExperimentJobId[1];
            projectDAO.addExperimentJobForProject(projectId, experimentJobId);
        }
    }

    /**
     * 建立map中project和graph的关系
     * 开启事务，一次报错全部回滚，若事务嵌套，则继承该事务
     *
     * @param projectId2GraphIdList 要建立关系的graph的ID List
     */
    @Transactional
    public void addProjectGraphRelation(List<Long[]> projectId2GraphIdList) {
        for (Long[] projectId2GraphId : projectId2GraphIdList) {
            long projectId = projectId2GraphId[0];
            long graphId = projectId2GraphId[1];
            projectDAO.addGraphForProject(projectId, graphId);
        }
    }

    /**
     * 建立map中project和datasource的关系
     * 开启事务，一次报错全部回滚，若事务嵌套，则继承该事务
     *
     * @param projectId2DatasourceIdList    要建立关系的DataSource的ID List
     */
    @Transactional
    public void addProjectDatasourceRelation(List<Long[]> projectId2DatasourceIdList) {
        for (Long[] projectId2DatasourceId : projectId2DatasourceIdList) {
            long projectId = projectId2DatasourceId[0];
            long datasourceId = projectId2DatasourceId[1];
            projectDAO.addDatasourceForProject(projectId, datasourceId);
        }
    }

    /**
     * 建立map中project和service的关系
     * 开启事务，一次报错全部回滚，若事务嵌套，则继承该事务
     *
     * @param projectId2ServiceIdList   要建立关系的service的ID List
     */
    @Transactional
    public void addProjectServiceRelation(List<Long[]> projectId2ServiceIdList) {
        for (Long[] projectId2ServiceId : projectId2ServiceIdList) {
            long projectId = projectId2ServiceId[0];
            long serviceId = projectId2ServiceId[1];
            projectDAO.addServiceForProject(projectId, serviceId);
        }
    }

    /**
     * 建立map中project和serviceJob的关系
     * 开启事务，一次报错全部回滚，若事务嵌套，则继承该事务
     *
     * @param projectId2ServiceJobIdList    要建立关系的serviceJob的ID List
     */
    @Transactional
    public void addProjectServiceJobRelation(List<Long[]> projectId2ServiceJobIdList) {
        for (Long[] projectId2ServiceJobId : projectId2ServiceJobIdList) {
            long projectId = projectId2ServiceJobId[0];
            long serviceJobId = projectId2ServiceJobId[1];
            projectDAO.addServiceJobForProject(projectId, serviceJobId);
        }
    }

    /**
     * 建立map中project和component的关系
     * 开启事务，一次报错全部回滚，若事务嵌套，则继承该事务
     *
     * @param projectId2ComponentIdList 要建立关系的component的ID List
     */
    @Transactional
    public void addProjectComponentRelation(List<Long[]> projectId2ComponentIdList) {
        for (Long[] projectId2ComponentId : projectId2ComponentIdList) {
            long projectId = projectId2ComponentId[0];
            long componentId = projectId2ComponentId[1];
            projectDAO.addComponentForProject(projectId, componentId);
        }
    }

    /**
     * 建立map中project和code的关系
     * 开启事务，一次报错全部回滚，若事务嵌套，则继承该事务
     *
     * @param projectId2CodeIdList 要建立关系的code的ID List
     */
    @Transactional
    public void addProjectCodeRelation(List<Long[]> projectId2CodeIdList) {
        for (Long[] projectId2CodeId : projectId2CodeIdList) {
            long projectId = projectId2CodeId[0];
            long codeId = projectId2CodeId[1];
            projectDAO.addCodeForProject(projectId, codeId);
        }
    }

    /**
     * 建立map中project和mirror的关系
     * 开启事务，一次报错全部回滚，若事务嵌套，则继承该事务
     *
     * @param projectId2MirrorIdList 要建立关系的mirror的ID List
     */
    @Transactional
    public void addProjectMirrorRelation(List<Long[]> projectId2MirrorIdList) {
        for (Long[] projectId2MirrorId : projectId2MirrorIdList) {
            long projectId = projectId2MirrorId[0];
            long mirrorId = projectId2MirrorId[1];
            projectDAO.addMirrorForProject(projectId, mirrorId);
        }
    }

    /**
     * 建立map中project和mirrorJob的关系
     * 开启事务，一次报错全部回滚，若事务嵌套，则继承该事务
     *
     * @param projectId2MirrorJobIdList 要建立关系的mirrorJob的ID List
     */
    @Transactional
    public void addProjectMirrorJobRelation(List<Long[]> projectId2MirrorJobIdList) {
        for (Long[] projectId2MirrorJobId : projectId2MirrorJobIdList) {
            long projectId = projectId2MirrorJobId[0];
            long mirrorJobId = projectId2MirrorJobId[1];
            projectDAO.addMirrorJobForProject(projectId, mirrorJobId);
        }
    }

    /**
     * 改某application的对应关系
     *
     * @param projectId 要修改的project的ID
     * @param applicationIdList 需要修改关系的application的ID List
     */
    @Transactional
    public void modifyApplicationsOfProject(long projectId, List<Integer> applicationIdList) {
        // 根据ID找project，查找失败抛出异常
        Project project = projectDAO.findById(projectId).orElseThrow(() -> new EntityNotFoundException());
        List<Long[]> removeList = new ArrayList<>();

        // 先删除project原有的application
        for (Application application : project.getApplications()) {
            removeList.add(new Long[]{projectId, application.getId()});
        }
        removeProjectApplicationRelation(removeList);

        // 将传进来的application加入到project中
        List<Long[]> addList = new ArrayList<>();
        for (Integer applicationId : applicationIdList) {
            addList.add(new Long[]{projectId, Long.valueOf(applicationId)});
        }
        addProjectApplicationRelation(addList);
        // 这里也不需要save
    }

    /**
     * 改某component的对应关系
     *
     * @param projectId 要修改的project ID
     * @param componentIdList   需要修改关系的component的ID List
     */
    @Transactional
    public void modifyComponentsOfProject(long projectId, List<Integer> componentIdList) {
        // 根据ID找project，查找失败抛出异常
        Project project = projectDAO.findById(projectId).orElseThrow(() -> new EntityNotFoundException());
        List<Long[]> removeList = new ArrayList<>();
        // 先删除project原有的component
        for (Component component : project.getComponents()) {
            removeList.add(new Long[]{projectId, component.getId()});
        }
        removeProjectComponentRelation(removeList);

        // 将传进来的component加入到project中
        List<Long[]> addList = new ArrayList<>();
        for (Integer componentId : componentIdList) {
            addList.add(new Long[]{projectId, Long.valueOf(componentId)});
        }
        addProjectComponentRelation(addList);
    }

    /**
     * 改某experimentJob的对应关系
     *
     * @param projectId 要修改的project ID
     * @param experimentJobIdList   需要修改关系的experimentJob的ID List
     */
    @Transactional
    public void modifyExperimentJobsOfProject(long projectId, List<Integer> experimentJobIdList) {
        // 根据ID找project，查找失败抛出异常
        Project project = projectDAO.findById(projectId).orElseThrow(() -> new EntityNotFoundException());
        List<Long[]> removeList = new ArrayList<>();
        // 先删除project原有的experimentJob
        for (ExperimentJob experimentJob : project.getExperimentJobs()) {
            removeList.add(new Long[]{projectId, experimentJob.getId()});
        }
        removeProjectExperimentJobRelation(removeList);
        // 将传进来的experimentJob加入到project中
        List<Long[]> addList = new ArrayList<>();
        for (Integer experimentJobId : experimentJobIdList) {
            addList.add(new Long[]{projectId, Long.valueOf(experimentJobId)});
        }
        addProjectExperimentJobRelation(addList);
    }

    /**
     * 改某graph的对应关系
     *
     * @param projectId 要修改的project ID
     * @param graphIdList   需要修改关系的graph的ID List
     */
    @Transactional
    public void modifyGraphsOfProject(long projectId, List<Integer> graphIdList) {
        // 根据ID找project，查找失败抛出异常
        Project project = projectDAO.findById(projectId).orElseThrow(() -> new EntityNotFoundException());
        List<Long[]> removeList = new ArrayList<>();
        // 先删除project原有的graph
        for (Graph graph : project.getGraphs()) {
            removeList.add(new Long[]{projectId, graph.getId()});
        }
        removeProjectGraphRelation(removeList);
        // 将传进来的graph加入到project中
        List<Long[]> addList = new ArrayList<>();
        for (Integer graphId : graphIdList) {
            addList.add(new Long[]{projectId, Long.valueOf(graphId)});
        }
        addProjectGraphRelation(addList);
    }

    /**
     * 改某datasource的对应关系
     *
     * @param projectId 要修改的project ID
     * @param datasourceIdList  需要修改关系的datasource的ID List
     */
    @Transactional
    public void modifyDatasourceOfProject(long projectId, List<Integer> datasourceIdList) {
        // 根据ID找project，查找失败抛出异常
        Project project = projectDAO.findById(projectId).orElseThrow(() -> new EntityNotFoundException());
        List<Long[]> removeList = new ArrayList<>();
        // 先删除project原有的Datasource
        for (Datasource datasource : project.getDatasourceList()) {
            removeList.add(new Long[]{projectId, datasource.getId()});
        }
        removeProjectDatasourceRelation(removeList);
        // 将传进来的Datasource加入到project中
        List<Long[]> addList = new ArrayList<>();
        for (Integer datasourceId : datasourceIdList) {
            addList.add(new Long[]{projectId, Long.valueOf(datasourceId)});
        }
        addProjectDatasourceRelation(addList);
    }

    /**
     * 改某service的对应关系
     *
     * @param projectId 要修改的project ID
     * @param serviceIdList 需要修改关系的service的ID List
     */
    @Transactional
    public void modifyServicesOfProject(long projectId, List<Integer> serviceIdList) {
        // 根据ID找project，查找失败抛出异常
        Project project = projectDAO.findById(projectId).orElseThrow(() -> new EntityNotFoundException());
        List<Long[]> removeList = new ArrayList<>();
        // 先删除project原有的Service
        for (aizoo.domain.Service service : project.getServices()) {
            removeList.add(new Long[]{projectId, service.getId()});
        }
        removeProjectServiceRelation(removeList);
        // 将传进来的Service加入到project中
        List<Long[]> addList = new ArrayList<>();
        for (Integer serviceId : serviceIdList) {
            addList.add(new Long[]{projectId, Long.valueOf(serviceId)});
        }
        addProjectServiceRelation(addList);
    }

    /**
     * 改某serviceJob的对应关系
     *
     * @param projectId 要修改的project ID
     * @param serviceJobIdList  需要修改关系的serviceJob的ID List
     */
    @Transactional
    public void modifyServiceJobsOfProject(long projectId, List<Integer> serviceJobIdList) {
        // 根据ID找project，查找失败抛出异常
        Project project = projectDAO.findById(projectId).orElseThrow(() -> new EntityNotFoundException());
        // 先删除project原有的ServiceJob
        List<Long[]> removeList = new ArrayList<>();
        for (ServiceJob serviceJob : project.getServiceJobs()) {
            removeList.add(new Long[]{projectId, serviceJob.getId()});
        }
        removeProjectServiceJobRelation(removeList);
        // 将传进来的ServiceJob加入到project中
        List<Long[]> addList = new ArrayList<>();
        for (Integer serviceJobId : serviceJobIdList) {
            addList.add(new Long[]{projectId, Long.valueOf(serviceJobId)});
        }
        addProjectServiceJobRelation(addList);
    }

    /**
     * 改某code的对应关系
     *
     * @param projectId 要修改的project ID
     * @param codeIdList  需要修改关系的code的ID List
     */
    @Transactional
    public void modifyCodesOfProject(long projectId, List<Integer> codeIdList) {
        // 根据ID找project，查找失败抛出异常
        Project project = projectDAO.findById(projectId).orElseThrow(() -> new EntityNotFoundException());
        // 先删除project原有的Code
        List<Long[]> removeList = new ArrayList<>();
        for (Code code : project.getCodes()) {
            removeList.add(new Long[]{projectId, code.getId()});
        }
        removeProjectCodeRelation(removeList);
        // 将传进来的Code加入到project中
        List<Long[]> addList = new ArrayList<>();
        for (Integer codeId : codeIdList) {
            addList.add(new Long[]{projectId, Long.valueOf(codeId)});
        }
        addProjectCodeRelation(addList);
    }

    /**
     * 改某mirror的对应关系
     *
     * @param projectId 要修改的project ID
     * @param mirrorIdList  需要修改关系的mirror的ID List
     */
    @Transactional
    public void modifyMirrorsOfProject(long projectId, List<Integer> mirrorIdList) {
        // 根据ID找project，查找失败抛出异常
        Project project = projectDAO.findById(projectId).orElseThrow(() -> new EntityNotFoundException());
        // 先删除project原有的Mirror
        List<Long[]> removeList = new ArrayList<>();
        for (Mirror mirror : project.getMirrors()) {
            removeList.add(new Long[]{projectId, mirror.getId()});
        }
        removeProjectMirrorRelation(removeList);
        // 将传进来的Mirror加入到project中
        List<Long[]> addList = new ArrayList<>();
        for (Integer mirrorId : mirrorIdList) {
            addList.add(new Long[]{projectId, Long.valueOf(mirrorId)});
        }
        addProjectMirrorRelation(addList);
    }

    /**
     * 改某mirrorJob的对应关系
     *
     * @param projectId 要修改的project ID
     * @param mirrorJobIdList  需要修改关系的mirrorJob的ID List
     */
    @Transactional
    public void modifyMirrorJobsOfProject(long projectId, List<Integer> mirrorJobIdList) {
        // 根据ID找project，查找失败抛出异常
        Project project = projectDAO.findById(projectId).orElseThrow(() -> new EntityNotFoundException());
        // 先删除project原有的MirrorJob
        List<Long[]> removeList = new ArrayList<>();
        for (MirrorJob mirrorJob : project.getMirrorJobs()) {
            removeList.add(new Long[]{projectId, mirrorJob.getId()});
        }
        removeProjectMirrorJobRelation(removeList);
        // 将传进来的MirrorJob加入到project中
        List<Long[]> addList = new ArrayList<>();
        for (Integer mirrorJobId : mirrorJobIdList) {
            addList.add(new Long[]{projectId, Long.valueOf(mirrorJobId)});
        }
        addProjectMirrorJobRelation(addList);
    }
}
