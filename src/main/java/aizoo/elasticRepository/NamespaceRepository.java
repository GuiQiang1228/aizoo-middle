package aizoo.elasticRepository;

import aizoo.elasticObject.ElasticNamespace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface NamespaceRepository extends ElasticsearchRepository<ElasticNamespace,String> {
    Page<ElasticNamespace> findByPrivacy(String privacy, Pageable pageable);
}
