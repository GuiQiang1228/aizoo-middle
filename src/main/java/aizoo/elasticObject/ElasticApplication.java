package aizoo.elasticObject;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

import javax.persistence.Id;
import javax.persistence.Lob;

@Document(indexName = "application", type = "_doc")
public class ElasticApplication {
    @Id
    private Long id;

    private String privacy;

    private String name;

    private String title;

    @JsonProperty("job_status")
    @Field("job_status")
    private String jobStatus;

    @JsonProperty("graph_id")
    @Field("graph_id")
    private long graphId;

    private String username;

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public String getPrivacy() { return privacy; }

    public void setPrivacy(String privacy) { this.privacy = privacy; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getTitle() { return title; }

    public void setTitle(String title) { this.title = title; }

    public String getUsername() { return username; }

    public void setUsername(String username) { this.username = username; }

    public String getJobStatus() { return jobStatus; }

    public void setJobStatus(String jobStatus) { this.jobStatus = jobStatus; }

    public long getGraphId() { return graphId; }

    public void setGraphId(long graphId) { this.graphId = graphId; }
}
