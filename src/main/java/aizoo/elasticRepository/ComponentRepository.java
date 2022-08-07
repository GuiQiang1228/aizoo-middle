package aizoo.elasticRepository;

import aizoo.elasticObject.ElasticComponent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ComponentRepository extends ElasticsearchRepository<ElasticComponent,String> {
    Page<ElasticComponent> findByPrivacy(String privacy, Pageable pageable);

    Page<ElasticComponent> findByPrivacyAndComponentType(String privacy, String type, Pageable pageable);

}
