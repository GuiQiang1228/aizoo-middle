package aizoo.elasticObject;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

import javax.persistence.Id;

@Document(indexName = "experiment_job", type = "_doc")
public class ElasticExperimentJob {
    @Id
    private Long id;

    private String name;

    @JsonProperty("job_status")
    @Field("job_status")
    private String jobStatus;

    @JsonProperty("download_status")
    @Field("download_status")
    private String downloadStatus;

    private String description;

    private String username;

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getUsername() { return username; }

    public void setUsername(String username) { this.username = username; }

    public String getJobStatus() { return jobStatus; }

    public void setJobStatus(String jobStatus) { this.jobStatus = jobStatus; }

    public String getDownloadStatus() { return downloadStatus; }

    public void setDownloadStatus(String downloadStatus) { this.downloadStatus = downloadStatus; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }
}
