package aizoo.elasticRepository;

import aizoo.elasticObject.ElasticProject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ProjectRepository extends ElasticsearchRepository<ElasticProject,String> {
    Page<ElasticProject> findByPrivacy(String privacy, Pageable pageable);
}
