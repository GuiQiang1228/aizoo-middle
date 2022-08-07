package aizoo.controller;

import aizoo.aspect.WebLog;
import aizoo.elasticObject.*;
import aizoo.response.BaseResponse;
import aizoo.service.ElasticSearchService;
import aizoo.utils.ElasticUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.Map;

@BaseResponse
@RestController
public class ElasticSearchController {
    /**
     * 根据Request对component、application、datasource、experimentJob、GraphAllPage、Namespace、Project、Service、ServiceJob进行检索
     *
     * 检索方法包括全部检索和条件检索
     * 全部检索的Request中涉及的参数：
     * @param pageNum 检索起始页，默认值为0
     * @param pageSize 检索页面数量，默认值为10
     * @param type 需搜索内容所属类型
     * 若要进行条件检索，需添加参数
     * @param keywords 检索关键词，用于进行条件检索
     *
     */

    @Autowired
    ElasticSearchService elasticSearchService;

    /**
     * @return Page<ElasticComponent>
     * @throws IOException
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/elastic/search/component", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "elasticsearch component条件搜索")
    public Page<ElasticComponent> searchComponentPage(@RequestParam(value = "keywords", required = false, defaultValue = "") String keywords,
                                                      @RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                                      @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
                                                      @RequestParam(value = "type") String type) throws IOException {
       return elasticSearchService.searchComponent(keywords, type, pageNum, pageSize);
    }

    /**
     * @return Page<ElasticComponent>
     * @throws IOException
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/elastic/search/component/all", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "elasticsearch component全部搜索")
    public Page<ElasticComponent> searchComponentAllPage(@RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                                         @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
                                                         @RequestParam(value = "type") String type) throws IOException {
        return elasticSearchService.searchComponentAll(type, pageNum, pageSize);
    }

    /**
     * @return Page<ElasticApplication>
     * @throws IOException
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/elastic/search/application", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "elasticsearch application条件搜索")
    public Page<ElasticApplication> searchApplicationPage(@RequestParam(value = "keywords", required = false, defaultValue = "") String keywords,
                                                        @RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                                        @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) throws IOException {
        return elasticSearchService.searchApplication(keywords, pageNum, pageSize);
    }

    /**
     * @return Page<ElasticApplication>
     * @throws IOException
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/elastic/search/application/all", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "elasticsearch application全部搜索")
    public Page<ElasticApplication> searchApplicationAllPage(@RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                                         @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) throws IOException {
        return elasticSearchService.searchApplicationAll(pageNum, pageSize);
    }

    /**
     * @return Page<ElasticDatasource>
     * @throws IOException
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/elastic/search/datasource/all", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "elasticsearch datasource全部搜索")
    public Page<ElasticDatasource> searchDatasourceAllPage(@RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                                           @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) throws IOException {
        return elasticSearchService.searchDatasourceAll(pageNum, pageSize);
    }

    /**
     * @return Page<ElasticDatasource>
     * @throws IOException
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/elastic/search/datasource", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "elasticsearch datasource条件搜索")
    public Page<ElasticDatasource> searchDatasourcePage(@RequestParam(value = "keywords", required = false, defaultValue = "") String keywords,
                                                          @RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                                          @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) throws IOException {
        return elasticSearchService.searchDatasource(keywords, pageNum, pageSize);
    }

    /**
     * @return Page<ElasticExperimentJob>
     * @throws IOException
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/elastic/search/experimentJob/all", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "elasticsearch experimentJob全部搜索")
    public Page<ElasticExperimentJob> searchExperimentJobAllPage(@RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                                              @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) throws IOException {
        return elasticSearchService.searchExperimentJobAll(pageNum, pageSize);
    }

    /**
     * @return Page<ElasticExperimentJob>
     * @throws IOException
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/elastic/search/experimentJob", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "elasticsearch experimentJob条件搜索")
    public Page<ElasticExperimentJob> searchExperimentJobPage(@RequestParam(value = "keywords", required = false, defaultValue = "") String keywords,
                                                        @RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                                        @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) throws IOException {
        return elasticSearchService.searchExperimentJob(keywords, pageNum, pageSize);
    }

    /**
     * @return Page<ElasticGraph>
     * @throws IOException
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/elastic/search/graph/all", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "elasticsearch graph全部搜索")
    public Page<ElasticGraph> searchGraphAllPage(@RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                                 @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
                                                 @RequestParam(value = "type", required = false, defaultValue = "") String type) throws IOException {
        return elasticSearchService.searchGraphAll(type, pageNum, pageSize);
    }

    /**
     * @return Page<ElasticGraph>
     * @throws IOException
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/elastic/search/graph", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "elasticsearch graph条件搜索")
    public Page<ElasticGraph> searchGraphPage(@RequestParam(value = "keywords", required = false, defaultValue = "") String keywords,
                                              @RequestParam(value = "type",  defaultValue = "") String type,
                                              @RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                              @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) throws IOException {
        return elasticSearchService.searchGraph(keywords, type, pageNum, pageSize);
    }

    /**
     * @return Page<ElasticNamespace>
     * @throws IOException
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/elastic/search/namespace/all", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "elasticsearch namespace全部搜索")
    public Page<ElasticNamespace> searchNamespaceAllPage(@RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                                           @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) throws IOException {
        return elasticSearchService.searchNamespaceAll(pageNum, pageSize);
    }

    /**
     * @return Page<ElasticNamespace>
     * @throws IOException
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/elastic/search/namespace", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "elasticsearch namespace条件搜索")
    public Page<ElasticNamespace> searchNamespacePage(@RequestParam(value = "keywords", required = false, defaultValue = "") String keywords,
                                                        @RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                                        @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) throws IOException {
        return elasticSearchService.searchNamespace(keywords, pageNum, pageSize);
    }

    /**
     * @return Page<ElasticProject>
     * @throws IOException
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/elastic/search/project/all", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "elasticsearch project全部搜索")
    public Page<ElasticProject> searchProjectAllPage(@RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                                           @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) throws IOException {
        return elasticSearchService.searchProjectAll(pageNum, pageSize);
    }

    /**
     * @return Page<ElasticProject>
     * @throws IOException
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/elastic/search/project", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "elasticsearch project条件搜索")
    public Page<ElasticProject> searchProjectPage(@RequestParam(value = "keywords", required = false, defaultValue = "") String keywords,
                                                        @RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                                        @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) throws IOException {
        return elasticSearchService.searchProject(keywords, pageNum, pageSize);
    }

    /**
     * @return  Page<ElasticService>
     * @throws IOException
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/elastic/search/service/all", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "elasticsearch service全部搜索")
    public Page<ElasticService> searchServiceAllPage(@RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                                           @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) throws IOException {
        return elasticSearchService.searchServiceAll(pageNum, pageSize);
    }

    /**
     * @return  Page<ElasticService>
     * @throws IOException
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/elastic/search/service", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "elasticsearch service条件搜索")
    public Page<ElasticService> searchServicePage(@RequestParam(value = "keywords", required = false, defaultValue = "") String keywords,
                                                        @RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                                        @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) throws IOException {
        return elasticSearchService.searchService(keywords, pageNum, pageSize);
    }

    /**
     * @return  Page<ElasticServiceJob>
     * @throws IOException
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/elastic/search/serviceJob/all", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "elasticsearch serviceJob全部搜索")
    public Page<ElasticServiceJob> searchServiceJobAllPage(@RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                                           @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) throws IOException {
        return elasticSearchService.searchServiceJobAll(pageNum, pageSize);
    }

    /**
     * @return  Page<ElasticServiceJob>
     * @throws IOException
     */
    @CrossOrigin(origins = "*")
    @ResponseBody
    @RequestMapping(value = "/api/elastic/search/serviceJob", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "elasticsearch serviceJob条件搜索")
    public Page<ElasticServiceJob> searchServiceJobPage(@RequestParam(value = "keywords", required = false, defaultValue = "") String keywords,
                                                        @RequestParam(value = "pageNum", defaultValue = "0") Integer pageNum,
                                                        @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) throws IOException {
        return elasticSearchService.searchServiceJob(keywords, pageNum, pageSize);
    }

    /**
     * 获取目录
     * @param type
     * @return Map<String, Map<String, String>> 格式
     * {
     *      Component类别:{
     *          type:Component类别,
     *          name:Component名称
     *          }
     * }
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/api/elastic/get/catalogue", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @WebLog(description = "获取es搜索页面的目录")
    public Map<String, Map<String, String>> getCatalogue(String type) {
        return ElasticUtil.getCatalogue(type);
    }

}
