package aizoo.elasticObject;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

import javax.persistence.Id;

@Document(indexName = "graph", type = "_doc")
public class ElasticGraph {
    @Id
    private Long id;

    private String name;

    private boolean released;

    @JsonProperty("graph_version")
    @Field("graph_version")
    private String graphVersion;

    @JsonProperty("graph_type")
    @Field("graph_type")
    private String graphType;

    private String description;

    private String username;

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getUsername() { return username; }

    public void setUsername(String username) { this.username = username; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public Boolean getReleased() { return released; }

    public void setReleased(Boolean released) { this.released = released; }

    public String getGraphVersion() { return graphVersion; }

    public void setGraphVersion(String graphVersion) { this.graphVersion = graphVersion; }

    public String getGraphType() { return graphType; }

    public void setGraphType(String graphType) { this.graphType = graphType; }
}
