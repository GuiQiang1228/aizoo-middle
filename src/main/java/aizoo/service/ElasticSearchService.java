package aizoo.service;

import aizoo.elasticObject.*;
import aizoo.elasticRepository.*;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;


@Service("ElasticSearchService")
public class ElasticSearchService {
    /**
     * 实现对Component、Application、Datasource、Experiment、Graph、Namespace、Project、Service的检索
     */

    @Autowired
    RestHighLevelClient client;

    @Autowired
    ComponentRepository componentRepository;

    @Autowired
    AdvancedSearchRepository advancedSearchRepository;

    @Autowired
    ApplicationRepository applicationRepository;

    @Autowired
    DatasourceRepository datasourceRepository;

    @Autowired
    ExperimentJobRepository experimentJobRepository;

    @Autowired
    GraphRepository graphRepository;

    @Autowired
    NamespaceRepository namespaceRepository;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    ServiceRepository serviceRepository;

    @Autowired
    ServiceJobRepository serviceJobRepository;

    /**
     * 对Component进行全部检索
     * @param type 要检索内容所属类型
     * @param pageNum 检索起始页
     * @param pageSize 页面数量
     * @return 满足检索条件的Component，类型Page<ElasticComponent>
     * @throws IOException
     */
    public Page<ElasticComponent> searchComponentAll(String type, Integer pageNum, Integer pageSize) throws IOException {
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.Direction.DESC, "update_time");
        Page<ElasticComponent> elasticComponents = null;
        if(type.equals(""))
            elasticComponents = componentRepository.findByPrivacy("public", pageable);
        else
            elasticComponents = componentRepository.findByPrivacyAndComponentType("public", type, pageable);
        return elasticComponents;
    }

    /**
     * 对Component进行条件检索
     * @param keywords 检索关键词
     * @param type 要检索内容所属类型
     * @param pageNum 检索起始页
     * @param pageSize 页面数量
     * @return 满足检索条件的Component，类型Page<ElasticComponent>
     * @throws IOException
     */
    public Page<ElasticComponent> searchComponent(String keywords, String type, Integer pageNum, Integer pageSize) throws IOException {
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        Page<ElasticComponent> elasticComponents = advancedSearchRepository.findComponentByKeywords(keywords, type, pageable);
        return elasticComponents;
    }

    /**
     * 对Application进行全部检索
     * @param pageNum 检索起始页
     * @param pageSize 页面数量
     * @return 满足检索条件的Application，类型Page<ElasticApplication>
     * @throws IOException
     */
    public Page<ElasticApplication> searchApplicationAll(Integer pageNum, Integer pageSize) throws IOException {
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.Direction.DESC, "update_time");
        Page<ElasticApplication> elasticApplications = applicationRepository.findAll(pageable);
        return elasticApplications;
    }

    /**
     * 对Application进行条件检索
     * @param keywords 检索关键词
     * @param pageNum 检索起始页
     * @param pageSize 页面数量
     * @return 满足检索条件的Application，类型Page<ElasticApplication>
     * @throws IOException
     */
    public Page<ElasticApplication> searchApplication(String keywords, Integer pageNum, Integer pageSize) throws IOException {
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.Direction.DESC, "update_time");
        Page<ElasticApplication> elasticApplications = advancedSearchRepository.findApplicationByKeywords(keywords, pageable);
        return elasticApplications;
    }

    /**
     * 对Datasource进行全部检索
     * @param pageNum 检索起始页
     * @param pageSize 页面数量
     * @return 满足检索条件的Datasource，类型Page<ElasticDatasource>
     * @throws IOException
     */
    public Page<ElasticDatasource> searchDatasourceAll(Integer pageNum, Integer pageSize) throws IOException {
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.Direction.DESC, "update_time");
        Page<ElasticDatasource> elasticDatasources = datasourceRepository.findAll(pageable);
        return elasticDatasources;
    }

    /**
     * 对Datasource进行条件检索
     * @param keywords 检索关键词
     * @param pageNum 检索起始页
     * @param pageSize 页面数量
     * @return 满足检索条件的Datasource，类型Page<ElasticDatasource>
     * @throws IOException
     */
    public Page<ElasticDatasource> searchDatasource(String keywords, Integer pageNum, Integer pageSize) throws IOException {
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.Direction.DESC, "update_time");
        Page<ElasticDatasource> elasticDatasources = advancedSearchRepository.findDatasourceByKeywords(keywords, pageable);
        return elasticDatasources;
    }

    /**
     * 对ExperimentJob进行全部检索
     * @param pageNum 检索起始页
     * @param pageSize 页面数量
     * @return 满足检索条件的ExperimentJob，类型Page<ElasticExperimentJob>
     * @throws IOException
     */
    public Page<ElasticExperimentJob> searchExperimentJobAll(Integer pageNum, Integer pageSize) throws IOException {
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.Direction.DESC, "update_time");
        Page<ElasticExperimentJob> elasticExperimentJobs = experimentJobRepository.findAll(pageable);
        return elasticExperimentJobs;
    }

    /**
     * 对ExperimentJob进行条件检索
     * @param keywords 检索关键词
     * @param pageNum 检索起始页
     * @param pageSize 页面数量
     * @return 满足检索条件的ExperimentJob，类型Page<ElasticExperimentJob>
     * @throws IOException
     */
    public Page<ElasticExperimentJob> searchExperimentJob(String keywords, Integer pageNum, Integer pageSize) throws IOException {
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.Direction.DESC, "update_time");
        Page<ElasticExperimentJob> elasticExperimentJobs = advancedSearchRepository.findExperimentJobByKeywords(keywords, pageable);
        return elasticExperimentJobs;
    }

    /**
     * 对Graph进行全部检索
     * @param type 要检索内容所属类型
     * @param pageNum 检索起始页
     * @param pageSize 页面数量
     * @return 满足检索条件的Graph，类型Page<ElasticGraph>
     * @throws IOException
     */
    public Page<ElasticGraph> searchGraphAll(String type, Integer pageNum, Integer pageSize) throws IOException {
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.Direction.DESC, "update_time");
        Page<ElasticGraph> elasticGraphs = null;
        if(type.equals(""))
            elasticGraphs = graphRepository.findAll(pageable);
        else
            elasticGraphs = graphRepository.findByGraphType(type, pageable);
        return elasticGraphs;
    }

    /**
     * 对Graph进行条件检索
     * @param keywords 检索关键词
     * @param type 要检索内容所属类型
     * @param pageNum 检索起始页
     * @param pageSize 页面数量
     * @return 满足检索条件的Graph，类型Page<ElasticGraph>
     * @throws IOException
     */
    public Page<ElasticGraph> searchGraph(String keywords, String type, Integer pageNum, Integer pageSize) throws IOException {
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.Direction.DESC, "update_time");
        Page<ElasticGraph> elasticGraphs = null;
        if(type.equals(""))
            elasticGraphs = advancedSearchRepository.findGraphByKeywords(keywords, pageable);
        else
            elasticGraphs = advancedSearchRepository.findGraphByKeywordsAndType(keywords, type, pageable);
        return elasticGraphs;
    }

    /**
     * 对Namespace进行全部检索
     * @param pageNum 检索起始页
     * @param pageSize 页面数量
     * @return 满足检索条件的Namespace，类型Page<ElasticNamespace>
     * @throws IOException
     */
    public Page<ElasticNamespace> searchNamespaceAll(Integer pageNum, Integer pageSize) throws IOException {
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.Direction.DESC, "update_time");
        Page<ElasticNamespace> elasticNamespaces = namespaceRepository.findByPrivacy("public", pageable);
        return elasticNamespaces;
    }

    /**
     * 对Namespace进行条件检索
     * @param keywords 检索关键词
     * @param pageNum 检索起始页
     * @param pageSize 页面数量
     * @return 满足检索条件的Namespace，类型Page<ElasticNamespace>
     * @throws IOException
     */
    public Page<ElasticNamespace> searchNamespace(String keywords, Integer pageNum, Integer pageSize) throws IOException {
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.Direction.DESC, "update_time");
        Page<ElasticNamespace> elasticNamespaces = advancedSearchRepository.findNamespaceByKeywords(keywords, pageable);
        return elasticNamespaces;
    }

    /**
     * 对Project进行全部检索
     * @param pageNum 检索起始页
     * @param pageSize 页面数量
     * @return 满足检索条件的Project，类型Page<ElasticProject>
     * @throws IOException
     */
    public Page<ElasticProject> searchProjectAll(Integer pageNum, Integer pageSize) throws IOException {
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.Direction.DESC, "update_time");
        Page<ElasticProject> elasticExperimentJobs = projectRepository.findByPrivacy("public", pageable);
        return elasticExperimentJobs;
    }

    /**
     * 对Project进行条件检索
     * @param keywords 检索关键词
     * @param pageNum 检索起始页
     * @param pageSize 页面数量
     * @return 满足检索条件的Project，类型Page<ElasticProject>
     * @throws IOException
     */
    public Page<ElasticProject> searchProject(String keywords, Integer pageNum, Integer pageSize) throws IOException {
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.Direction.DESC, "update_time");
        Page<ElasticProject> elasticProjects = advancedSearchRepository.findProjectByKeywords(keywords, pageable);
        return elasticProjects;
    }

    /**
     * 对Service进行全部检索
     * @param pageNum 检索起始页
     * @param pageSize 页面数量
     * @return 满足检索条件的Service，类型Page<ElasticService>
     * @throws IOException
     */
    public Page<ElasticService> searchServiceAll(Integer pageNum, Integer pageSize) throws IOException {
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.Direction.DESC, "update_time");
        Page<ElasticService> elasticServices = serviceRepository.findByPrivacy("public", pageable);
        return elasticServices;
    }

    /**
     * 对Service进行条件检索
     * @param keywords 检索关键词
     * @param pageNum 检索起始页
     * @param pageSize 页面数量
     * @return 满足检索条件的Service，类型Page<ElasticService>
     * @throws IOException
     */
    public Page<ElasticService> searchService(String keywords, Integer pageNum, Integer pageSize) throws IOException {
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.Direction.DESC, "update_time");
        Page<ElasticService> elasticServices = advancedSearchRepository.findServiceByKeywords(keywords, pageable);
        return elasticServices;
    }

    /**
     * 对ServiceJob进行全部检索
     * @param pageNum 检索起始页
     * @param pageSize 页面数量
     * @return 满足检索条件的ServiceJob，类型Page<ElasticServiceJob>
     * @throws IOException
     */
    public Page<ElasticServiceJob> searchServiceJobAll(Integer pageNum, Integer pageSize) throws IOException {
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.Direction.DESC, "update_time");
        Page<ElasticServiceJob> elasticServiceJobs = serviceJobRepository.findAll(pageable);
        return elasticServiceJobs;
    }

    /**
     * 对ServiceJob进行条件检索
     * @param keywords 检索关键词
     * @param pageNum 检索起始页
     * @param pageSize 页面数量
     * @return 满足检索条件的ServiceJob，类型Page<ElasticServiceJob>
     * @throws IOException
     */
    public Page<ElasticServiceJob> searchServiceJob(String keywords, Integer pageNum, Integer pageSize) throws IOException {
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.Direction.DESC, "update_time");
        Page<ElasticServiceJob> elasticServiceJobs = advancedSearchRepository.findServiceJobByKeywords(keywords, pageable);
        return elasticServiceJobs;
    }
}
