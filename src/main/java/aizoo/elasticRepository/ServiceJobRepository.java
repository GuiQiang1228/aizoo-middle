package aizoo.elasticRepository;

import aizoo.elasticObject.ElasticServiceJob;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ServiceJobRepository extends ElasticsearchRepository<ElasticServiceJob,String> {
}
