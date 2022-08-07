package aizoo.service;

import aizoo.domain.*;
import aizoo.repository.*;
import aizoo.viewObject.mapper.*;
import aizoo.viewObject.object.*;
import aizoo.viewObject.pagebleUtils.VO2EntityMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class CommonService {

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

    // 在IDE控制台打印日志
    private static final Logger logger = LoggerFactory.getLogger(NamespaceService.class);

    /**
     * 检查所有实体的关联情况，并返回给前端
     *
     * @param id
     * @param type (可传值为：component service experimentJob  serviceJob graph project datasource)
     * @return 双层Map，第一层的string共有以下十种类型，第二层为<objectId, object>形式
     */
    public HashMap<String, HashMap<Long, Object>> checkCommon(long id, String type) {
        logger.info("Start check Common");
        logger.info("id: {},type: {}",id,type);

        //1.初始化返回结果object
        HashMap<String, HashMap<Long, Object>> object = new HashMap<>();
        //将第一层map的key设置为以下十种类型
        object.put("component", new HashMap<>());
        object.put("service", new HashMap<>());
        object.put("namespace", new HashMap<>());
        object.put("experimentJob", new HashMap<>());
        object.put("serviceJob", new HashMap<>());
        object.put("graph", new HashMap<>());
        object.put("project", new HashMap<>());
        object.put("datasource", new HashMap<>());
        object.put("application", new HashMap<>());
        object.put("checkPoint", new HashMap<>());
        List<ApplicationVO> applicationVOS = new ArrayList<>();
        List<ComponentVO> componentVOS = new ArrayList<>();
        List<ExperimentJobVO> experimentJobVOS = new ArrayList<>();
        List<GraphVO> graphVOS = new ArrayList<>();
        List<DatasourceVO> datasourceVOList = new ArrayList<>();
        List<ServiceVO> serviceVOS = new ArrayList<>();
        List<ServiceJobVO> serviceJobVOS = new ArrayList<>();
        List<NamespaceVO> namespaceVOS = new ArrayList<>();
        List<Project> projectList = new ArrayList<>();

        //2. 对于待检查实体按其类型，检查其关联情况，与以上定义列表有关联的，添加进列表
        if (type.equals("service")) {
            //若为service类型，将graph，namespace，project加入列表
            aizoo.domain.Service service = serviceDAO.findById(id).orElseThrow(()-> new EntityNotFoundException());
            if(service.getGraph()!=null)
                graphVOS.add(GraphVOEntityMapper.MAPPER.graph2GraphVO(service.getGraph()));
            namespaceVOS.add(NamespaceVOEntityMapper.MAPPER.namespace2NamespaceVO(service.getNamespace()));
            projectList = service.getProjects();
        } else if (type.equals("serviceJob")) {
            //若为serviceJob类型，将graph，service，project加入列表
            ServiceJob serviceJob = serviceJobDAO.findById(id).orElseThrow(()-> new EntityNotFoundException());
            if(serviceJob.getGraph()!=null)
                graphVOS.add(GraphVOEntityMapper.MAPPER.graph2GraphVO(serviceJob.getGraph()));
            serviceVOS.add(ServiceVOEntityMapper.MAPPER.Service2ServiceVO(serviceJob.getService()));
            projectList = serviceJob.getProjects();
        } else if (type.equals("component")) {
            //若为component类型，将graph，namespace，project加入列表
            Component component = componentDAO.findById(id).orElseThrow(()-> new EntityNotFoundException());
//            if(component.getGraph()!=null)
//                graphVOS.add(GraphVOEntityMapper.MAPPER.graph2GraphVO(component.getGraph()));
            if(component.isComposed())
                namespaceVOS.add(NamespaceVOEntityMapper.MAPPER.namespace2NamespaceVO(component.getNamespace()));
            projectList = component.getProjects();
        } else if (type.equals("graph")) {
            //若为component类型，将graph，project，application,experimentJob加入列表
            Graph graph = graphDAO.findById(id).orElseThrow(()-> new EntityNotFoundException());
            //对graph的component和service分别进行操作
//            if(graph.getComponent()!=null)
//                componentVOS.add(ComponentVOEntityMapper.MAPPER.component2ComponentVO(graph.getComponent()));
//            if(graph.getService()!=null)
//                serviceVOS.add(ServiceVOEntityMapper.MAPPER.Service2ServiceVO(graph.getService()));
            if(graph.getService() != null){
                List<ServiceJob> serviceJobs = serviceJobDAO.findByServiceId(graph.getService().getId());
                serviceJobVOS = VO2EntityMapper.mapEntityList2VOList(ServiceJobVOEntityMapper.MAPPER::serviceJob2ServiceJobVO, serviceJobs);
            }
            projectList = graph.getProjects();
            applicationVOS = VO2EntityMapper.mapEntityList2VOList(ApplicationVOEntityMapper.MAPPER::application2ApplicationVO, graph.getApplications());
            experimentJobVOS = VO2EntityMapper.mapEntityList2VOList(ExperimentJobVOEntityMapper.MAPPER::job2JobVO, graph.getJobs());

        } else if (type.equals("experimentJob")) {
            //若为experimentJob类型，将graph，component，project,checkPoint加入列表
            ExperimentJob experimentJob = experimentJobDAO.findById(id).orElseThrow(()-> new EntityNotFoundException());
            if(experimentJob.getGraph()!=null)
                graphVOS.add(GraphVOEntityMapper.MAPPER.graph2GraphVO(experimentJob.getGraph()));
            if(experimentJob.getComponent()!=null)
                componentVOS.add(ComponentVOEntityMapper.MAPPER.component2ComponentVO(experimentJob.getComponent()));
            projectList = experimentJob.getProjects();
            List<CheckPointVO> checkPoints = VO2EntityMapper.mapEntityList2VOList(CheckPointVOEntityMapper.MAPPER::CheckPoint2CheckPointVO, experimentJob.getCheckPoints());
            for(CheckPointVO checkPointVO : checkPoints)
                object.get("checkPoint").put(checkPointVO.getId(), checkPointVO);
        } else if (type.equals("datasource")) {
            //若为datasource类型，将namespace，project加入列表
            Datasource datasource = datasourceDAO.findById(id).orElseThrow(()-> new EntityNotFoundException());
            namespaceVOS.add(NamespaceVOEntityMapper.MAPPER.namespace2NamespaceVO(datasource.getNamespace()));
            projectList = datasource.getProjects();
        } else if (type.equals("project")) {
            //若为project类型，将application，component，service，serviceJob，datasource，experimentJob，graph都加入列表
            Project project = projectDAO.findById(id).orElseThrow(()-> new EntityNotFoundException());
            applicationVOS = VO2EntityMapper.mapEntityList2VOList(ApplicationVOEntityMapper.MAPPER::application2ApplicationVO, project.getApplications());
            componentVOS = VO2EntityMapper.mapEntityList2VOList(ComponentVOEntityMapper.MAPPER::component2ComponentVO, project.getComponents());
            serviceVOS = VO2EntityMapper.mapEntityList2VOList(ServiceVOEntityMapper.MAPPER::Service2ServiceVO, project.getServices());
            serviceJobVOS = VO2EntityMapper.mapEntityList2VOList(ServiceJobVOEntityMapper.MAPPER::serviceJob2ServiceJobVO, project.getServiceJobs());
            datasourceVOList = VO2EntityMapper.mapEntityList2VOList(DatasourceVOEntityMapper.MAPPER::datasource2DatasourceVO, project.getDatasourceList());
            experimentJobVOS = VO2EntityMapper.mapEntityList2VOList(ExperimentJobVOEntityMapper.MAPPER::job2JobVO, project.getExperimentJobs());
            graphVOS = VO2EntityMapper.mapEntityList2VOList(GraphVOEntityMapper.MAPPER::graph2GraphVO, project.getGraphs());
        }

        //3. 将列表按组织形式放入返回结果object中，并返回
        //对于每一个列表，将每一个元素按照格式<objectId, object>加入对应map
        for(ApplicationVO applicationVO : applicationVOS)
            object.get("application").put(applicationVO.getId(), applicationVO);
        for(ExperimentJobVO experimentJobVO : experimentJobVOS)
            object.get("experimentJob").put(experimentJobVO.getId(), experimentJobVO);
        for(GraphVO graphVO : graphVOS)
            object.get("graph").put(graphVO.getId(), graphVO);
        for(ServiceVO serviceVO : serviceVOS)
            object.get("service").put(serviceVO.getId(), serviceVO);
        for(ServiceJobVO serviceJobVO : serviceJobVOS)
            object.get("serviceJob").put(serviceJobVO.getId(), serviceJobVO);
        for(DatasourceVO datasourceVO : datasourceVOList)
            object.get("datasource").put(datasourceVO.getId(), datasourceVO);
        for(ComponentVO componentVO : componentVOS)
            object.get("component").put(componentVO.getId(), componentVO);
        for(NamespaceVO namespaceVO : namespaceVOS)
            object.get("namespace").put(namespaceVO.getId(), namespaceVO);
        for(Project project : projectList)
            object.get("project").put(project.getId(), project);

        logger.info("End check Common");
        return object;
    }
}
