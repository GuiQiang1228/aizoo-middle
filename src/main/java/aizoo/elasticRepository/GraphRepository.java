package aizoo.elasticRepository;

import aizoo.elasticObject.ElasticGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface GraphRepository extends ElasticsearchRepository<ElasticGraph,String> {
    Page<ElasticGraph> findByGraphType(String type, Pageable pageable);
}
