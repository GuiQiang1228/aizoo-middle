package aizoo.elasticRepository;

import aizoo.elasticObject.ElasticExperimentJob;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ExperimentJobRepository extends ElasticsearchRepository<ElasticExperimentJob,String> {
}
