package aizoo.elasticRepository;

import aizoo.elasticObject.ElasticService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ServiceRepository extends ElasticsearchRepository<ElasticService,String> {
    Page<ElasticService> findByPrivacy(String privacy, Pageable pageable);
}
