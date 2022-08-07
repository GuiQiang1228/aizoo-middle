package aizoo.elasticRepository;

import aizoo.elasticObject.ElasticDatasource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface DatasourceRepository extends ElasticsearchRepository<ElasticDatasource,String> {
    Page<ElasticDatasource> findByPrivacy(String privacy, Pageable pageable);
}
