package aizoo.elasticRepository;

import aizoo.elasticObject.ElasticApplication;
import aizoo.elasticObject.ElasticComponent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ApplicationRepository extends ElasticsearchRepository<ElasticApplication,String> {
    Page<ElasticApplication> findAll(Pageable pageable);
}
