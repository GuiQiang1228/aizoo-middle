package aizoo.elasticObject;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

import javax.persistence.Id;

@Document(indexName = "service_job", type = "_doc")
public class ElasticServiceJob {
    @Id
    private Long id;

    private String port;

    private String name;

    private String ip;

    private String description;

    @JsonProperty("job_status")
    @Field("job_status")
    private String jobStatus;

    @JsonProperty("graph_id")
    @Field("graph_id")
    private long graphId;

    @JsonProperty("service_id")
    @Field("service_id")
    private long serviceId;

    private String username;

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getUsername() { return username; }

    public void setUsername(String username) { this.username = username; }

    public String getJobStatus() { return jobStatus; }

    public void setJobStatus(String jobStatus) { this.jobStatus = jobStatus; }

    public long getGraphId() { return graphId; }

    public void setGraphId(long graphId) { this.graphId = graphId; }

    public String getPort() { return port; }

    public void setPort(String port) { this.port = port; }

    public String getIp() { return ip; }

    public void setIp(String ip) { this.ip = ip; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public long getServiceId() { return serviceId; }

    public void setServiceId(long serviceId) { this.serviceId = serviceId; }
}
