package aizoo.elasticRepository.elasticImpl;

import aizoo.elasticObject.*;
import aizoo.elasticRepository.AdvancedSearchRepository;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class AdvancedSearchRepositoryImpl implements AdvancedSearchRepository {

    @Autowired
    ElasticsearchRestTemplate elasticsearchTemplate;

    public Page<ElasticComponent> findComponentByKeywords(String keywords, String type, Pageable pageable) {
        // 关键字可在name字段进行通配符搜索
        QueryBuilder nameWildcardNameQuery = QueryBuilders.wildcardQuery("name", "*" + keywords + "*");
        QueryBuilder namespaceWildcardNameQuery = QueryBuilders.wildcardQuery("namespace", "*" + keywords + "*");
        // name字段boost提高，从而match之后提高得分
        Map<String, Float> fieldMap = new HashMap<>();
        fieldMap.put("name", 5.0f);
        fieldMap.put("title", 1.5f);
        fieldMap.put("namespace", 1.0f);
        fieldMap.put("description", 0.5f);
        QueryBuilder multiMatchQuery = QueryBuilders.multiMatchQuery(keywords).fields(fieldMap);
        // should不会影响搜索结果，但match多了可提高得分
        QueryBuilder keywordMatchQuery = QueryBuilders.boolQuery().should(nameWildcardNameQuery).should(namespaceWildcardNameQuery).should(multiMatchQuery);

        // 复合组件的搜索条件
        QueryBuilder composedComponentQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("released", true))
                .must(QueryBuilders.termQuery("composed", true));
        // 原子组件的搜索条件
        QueryBuilder atomicComponentQuery = QueryBuilders.termQuery("composed", false);
        // 复合或选择组件的条件，中间用should结合
        QueryBuilder mustQuery = QueryBuilders.boolQuery().should(composedComponentQuery).should(atomicComponentQuery);

        NativeSearchQuery nativeSearchQuery = new NativeSearchQueryBuilder()
                .withPageable(pageable)
                .withQuery(
                        QueryBuilders.boolQuery()
                                .must(keywordMatchQuery)
                                .must(QueryBuilders.termQuery("privacy", "public"))
                                .must(QueryBuilders.termQuery("component_type.keyword", type))
                                .must(mustQuery)
                )
                .withSort(SortBuilders.scoreSort().order(SortOrder.DESC))
                .build();
        AggregatedPage<ElasticComponent> componentAggregatedPage = elasticsearchTemplate.queryForPage(nativeSearchQuery, ElasticComponent.class);
        return componentAggregatedPage;
    }

    public Page<ElasticApplication> findApplicationByKeywords(String keywords, Pageable pageable) {
        // 关键字可在name字段进行通配符搜索
        QueryBuilder nameWildcardNameQuery = QueryBuilders.wildcardQuery("name", "*" + keywords + "*");
        // name字段boost提高，从而match之后提高得分
        Map<String, Float> fieldMap = new HashMap<>();
        fieldMap.put("name", 5.0f);
        fieldMap.put("title", 1.5f);
        QueryBuilder multiMatchQuery = QueryBuilders.multiMatchQuery(keywords).fields(fieldMap);
        // should不会影响搜索结果，但match多了可提高得分
        QueryBuilder keywordMatchQuery = QueryBuilders.boolQuery().should(nameWildcardNameQuery).should(multiMatchQuery);
        NativeSearchQuery nativeSearchQuery = new NativeSearchQueryBuilder()
                .withPageable(pageable)
                //.withQuery(QueryBuilders.boolQuery().must(QueryBuilders.multiMatchQuery(keywords, "name", "title", "username", "job_status")).must(QueryBuilders.termQuery("privacy", "public")))
                .withQuery(keywordMatchQuery)
                .withSort(SortBuilders.scoreSort().order(SortOrder.DESC))
                .build();
        AggregatedPage<ElasticApplication> applicationAggregatedPage = elasticsearchTemplate.queryForPage(nativeSearchQuery, ElasticApplication.class);
        return applicationAggregatedPage;
    }

    public Page<ElasticDatasource> findDatasourceByKeywords(String keywords, Pageable pageable) {
        // 关键字可在name字段进行通配符搜索
        QueryBuilder nameWildcardNameQuery = QueryBuilders.wildcardQuery("name", "*" + keywords + "*");
        QueryBuilder namespaceWildcardNameQuery = QueryBuilders.wildcardQuery("namespace", "*" + keywords + "*");
        // name字段boost提高，从而match之后提高得分
        Map<String, Float> fieldMap = new HashMap<>();
        fieldMap.put("name", 5.0f);
        fieldMap.put("title", 1.5f);
        fieldMap.put("namespace", 1.0f);
        fieldMap.put("description", 0.5f);
        QueryBuilder multiMatchQuery = QueryBuilders.multiMatchQuery(keywords).fields(fieldMap);
        // should不会影响搜索结果，但match多了可提高得分
        QueryBuilder keywordMatchQuery = QueryBuilders.boolQuery().should(nameWildcardNameQuery).should(namespaceWildcardNameQuery).should(multiMatchQuery);
        NativeSearchQuery nativeSearchQuery = new NativeSearchQueryBuilder()
                .withPageable(pageable)
                .withQuery(
                        QueryBuilders.boolQuery()
                                .must(keywordMatchQuery)
                                .must(QueryBuilders.termQuery("privacy", "public"))
                )
                .withSort(SortBuilders.scoreSort().order(SortOrder.DESC))
                .build();
        AggregatedPage<ElasticDatasource> elasticDatasources = elasticsearchTemplate.queryForPage(nativeSearchQuery, ElasticDatasource.class);
        return elasticDatasources;
    }

    @Override
    public Page<ElasticExperimentJob> findExperimentJobByKeywords(String keywords, Pageable pageable) {
        // 关键字可在name字段进行通配符搜索
        QueryBuilder nameWildcardNameQuery = QueryBuilders.wildcardQuery("name", "*" + keywords + "*");
        // name字段boost提高，从而match之后提高得分
        Map<String, Float> fieldMap = new HashMap<>();
        fieldMap.put("name", 5.0f);
        fieldMap.put("description", 1.5f);
        QueryBuilder multiMatchQuery = QueryBuilders.multiMatchQuery(keywords).fields(fieldMap);
        // should不会影响搜索结果，但match多了可提高得分
        QueryBuilder keywordMatchQuery = QueryBuilders.boolQuery().should(nameWildcardNameQuery).should(multiMatchQuery);
        NativeSearchQuery nativeSearchQuery = new NativeSearchQueryBuilder()
                .withPageable(pageable)
//                .withQuery(QueryBuilders.multiMatchQuery(keywords, "name", "description", "job_status", "username", "download_status"))
                .withQuery(keywordMatchQuery)
                .withSort(SortBuilders.scoreSort().order(SortOrder.DESC))
                .build();
        AggregatedPage<ElasticExperimentJob> elasticExperimentJobs = elasticsearchTemplate.queryForPage(nativeSearchQuery, ElasticExperimentJob.class);
        return elasticExperimentJobs;
    }

    @Override
    public Page<ElasticGraph> findGraphByKeywords(String keywords, Pageable pageable) {
        // 关键字可在name字段进行通配符搜索
        QueryBuilder nameWildcardNameQuery = QueryBuilders.wildcardQuery("name", "*" + keywords + "*");
        // name字段boost提高，从而match之后提高得分
        Map<String, Float> fieldMap = new HashMap<>();
        fieldMap.put("name", 5.0f);
        fieldMap.put("description", 1.5f);
        QueryBuilder multiMatchQuery = QueryBuilders.multiMatchQuery(keywords).fields(fieldMap);
        // should不会影响搜索结果，但match多了可提高得分
        QueryBuilder keywordMatchQuery = QueryBuilders.boolQuery().should(nameWildcardNameQuery).should(multiMatchQuery);
        NativeSearchQuery nativeSearchQuery = new NativeSearchQueryBuilder()
                .withPageable(pageable)
//                .withQuery(QueryBuilders.multiMatchQuery(keywords, "name", "description", "graph_type", "username", "graph_version"))
                .withQuery(keywordMatchQuery)
                .withSort(SortBuilders.scoreSort().order(SortOrder.DESC))
                .build();
        AggregatedPage<ElasticGraph> elasticGraphs = elasticsearchTemplate.queryForPage(nativeSearchQuery, ElasticGraph.class);
        return elasticGraphs;
    }

    @Override
    public Page<ElasticGraph> findGraphByKeywordsAndType(String keywords, String type, Pageable pageable) {
        // 关键字可在name字段进行通配符搜索
        QueryBuilder nameWildcardNameQuery = QueryBuilders.wildcardQuery("name", "*" + keywords + "*");
        // name字段boost提高，从而match之后提高得分
        Map<String, Float> fieldMap = new HashMap<>();
        fieldMap.put("name", 5.0f);
        fieldMap.put("description", 1.5f);
        QueryBuilder multiMatchQuery = QueryBuilders.multiMatchQuery(keywords).fields(fieldMap);
        // should不会影响搜索结果，但match多了可提高得分
        QueryBuilder keywordMatchQuery = QueryBuilders.boolQuery().should(nameWildcardNameQuery).should(multiMatchQuery);
        NativeSearchQuery nativeSearchQuery = new NativeSearchQueryBuilder()
                .withPageable(pageable)
                .withQuery(
                        QueryBuilders.boolQuery()
                                .must(keywordMatchQuery)
                                .must(QueryBuilders.termQuery("graph_type", type))
                )
                .withSort(SortBuilders.scoreSort().order(SortOrder.DESC))
                .build();
        AggregatedPage<ElasticGraph> elasticGraphs = elasticsearchTemplate.queryForPage(nativeSearchQuery, ElasticGraph.class);
        return elasticGraphs;
    }

    @Override
    public Page<ElasticNamespace> findNamespaceByKeywords(String keywords, Pageable pageable) {
        NativeSearchQuery nativeSearchQuery = new NativeSearchQueryBuilder()
                .withPageable(pageable)
                // 这里keyword可使用空格分隔，填好要搜索的field即可，默认的空格之间用的是or操作符
                .withQuery(QueryBuilders.boolQuery().must(QueryBuilders.multiMatchQuery(keywords, "username", "namespace")).must(QueryBuilders.termQuery("privacy", "public")))
                .build();
        AggregatedPage<ElasticNamespace> elasticNamespaces = elasticsearchTemplate.queryForPage(nativeSearchQuery, ElasticNamespace.class);
        return elasticNamespaces;
    }

    @Override
    public Page<ElasticProject> findProjectByKeywords(String keywords, Pageable pageable) {
        NativeSearchQuery nativeSearchQuery = new NativeSearchQueryBuilder()
                .withPageable(pageable)
                // 这里keyword可使用空格分隔，填好要搜索的field即可，默认的空格之间用的是or操作符
                .withQuery(QueryBuilders.boolQuery().must(QueryBuilders.multiMatchQuery(keywords, "name", "description", "username")).must(QueryBuilders.termQuery("privacy", "public")))
                .build();
        AggregatedPage<ElasticProject> elasticProjects = elasticsearchTemplate.queryForPage(nativeSearchQuery, ElasticProject.class);
        return elasticProjects;
    }

    @Override
    public Page<ElasticServiceJob> findServiceJobByKeywords(String keywords, Pageable pageable) {
        // 关键字可在name字段进行通配符搜索
        QueryBuilder nameWildcardNameQuery = QueryBuilders.wildcardQuery("name", "*" + keywords + "*");
        // name字段boost提高，从而match之后提高得分
        Map<String, Float> fieldMap = new HashMap<>();
        fieldMap.put("name", 5.0f);
        fieldMap.put("description", 1.5f);
        QueryBuilder multiMatchQuery = QueryBuilders.multiMatchQuery(keywords).fields(fieldMap);
        // should不会影响搜索结果，但match多了可提高得分
        QueryBuilder keywordMatchQuery = QueryBuilders.boolQuery().should(nameWildcardNameQuery).should(multiMatchQuery);
        NativeSearchQuery nativeSearchQuery = new NativeSearchQueryBuilder()
                .withPageable(pageable)
//                .withQuery(QueryBuilders.multiMatchQuery(keywords, "name", "description", "port", "username", "ip", "job_status"))
                .withQuery(keywordMatchQuery)
                .withSort(SortBuilders.scoreSort().order(SortOrder.DESC))
                .build();
        AggregatedPage<ElasticServiceJob> elasticServiceJobs = elasticsearchTemplate.queryForPage(nativeSearchQuery, ElasticServiceJob.class);
        return elasticServiceJobs;
    }

    @Override
    public Page<ElasticService> findServiceByKeywords(String keywords, Pageable pageable) {
        // 关键字可在name字段进行通配符搜索
        QueryBuilder nameWildcardNameQuery = QueryBuilders.wildcardQuery("name", "*" + keywords + "*");
        QueryBuilder namespaceWildcardNameQuery = QueryBuilders.wildcardQuery("namespace", "*" + keywords + "*");
        // name字段boost提高，从而match之后提高得分
        Map<String, Float> fieldMap = new HashMap<>();
        fieldMap.put("name", 5.0f);
        fieldMap.put("title", 1.5f);
        fieldMap.put("namespace", 1.0f);
        fieldMap.put("description", 0.5f);
        QueryBuilder multiMatchQuery = QueryBuilders.multiMatchQuery(keywords).fields(fieldMap);
        // should不会影响搜索结果，但match多了可提高得分
        QueryBuilder keywordMatchQuery = QueryBuilders.boolQuery().should(nameWildcardNameQuery).should(namespaceWildcardNameQuery).should(multiMatchQuery);
        NativeSearchQuery nativeSearchQuery = new NativeSearchQueryBuilder()
                .withPageable(pageable)
                .withQuery(
                        QueryBuilders.boolQuery()
                                .must(keywordMatchQuery)
                                .must(QueryBuilders.termQuery("privacy", "public"))
                )
                .withSort(SortBuilders.scoreSort().order(SortOrder.DESC))
                .build();
        AggregatedPage<ElasticService> elasticServices = elasticsearchTemplate.queryForPage(nativeSearchQuery, ElasticService.class);
        return elasticServices;
    }
}
