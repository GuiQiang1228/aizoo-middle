package aizoo.elasticRepository;

import aizoo.elasticObject.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdvancedSearchRepository {
    Page<ElasticComponent> findComponentByKeywords(String keywords, String type, Pageable pageable);

    Page<ElasticApplication> findApplicationByKeywords(String keywords, Pageable pageable);

    Page<ElasticDatasource> findDatasourceByKeywords(String keywords, Pageable pageable);

    Page<ElasticExperimentJob> findExperimentJobByKeywords(String keywords, Pageable pageable);

    Page<ElasticGraph> findGraphByKeywords(String keywords, Pageable pageable);

    Page<ElasticNamespace> findNamespaceByKeywords(String keywords, Pageable pageable);

    Page<ElasticProject> findProjectByKeywords(String keywords, Pageable pageable);

    Page<ElasticServiceJob> findServiceJobByKeywords(String keywords, Pageable pageable);

    Page<ElasticService> findServiceByKeywords(String keywords, Pageable pageable);

    Page<ElasticGraph> findGraphByKeywordsAndType(String keywords, String type, Pageable pageable);
}
