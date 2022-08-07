package aizoo.elasticObject;


import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

import javax.persistence.Id;
import javax.persistence.Lob;
import java.io.Serializable;


@Document(indexName = "component", type = "_doc")
public class ElasticComponent implements Serializable {
    @Id
    private Long id;

    private String privacy;

    private String name;

    private String title;

    @JsonProperty("component_type")
    @Field("component_type")
    private String componentType;

    @Lob
    private String description;

    private boolean released;

    private String framework;

    @JsonProperty("framework_version")
    @Field("framework_version")
    private String frameworkVersion;

    private boolean composed;

    @JsonProperty("component_version")
    @Field("component_version")
    private String componentVersion;

    private String username;

    private String namespace;

    @JsonProperty("graph_id")
    @Field("graph_id")
    private long graphId;

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public String getPrivacy() { return privacy; }

    public void setPrivacy(String privacy) { this.privacy = privacy; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getTitle() { return title; }

    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public boolean isReleased() { return released; }

    public void setReleased(boolean released) { this.released = released; }

    public String getFramework() { return framework; }

    public void setFramework(String framework) { this.framework = framework; }

    public String getFrameworkVersion() { return frameworkVersion; }

    public void setFrameworkVersion(String frameworkVersion) { this.frameworkVersion = frameworkVersion; }

    public boolean isComposed() { return composed; }

    public void setComposed(boolean composed) { this.composed = composed; }

    public String getComponentVersion() { return componentVersion; }

    public void setComponentVersion(String componentVersion) { this.componentVersion = componentVersion; }

    public String getUsername() { return username; }

    public void setUsername(String username) { this.username = username; }

    public String getNamespace() { return namespace; }

    public void setNamespace(String namespace) { this.namespace = namespace; }

    public long getGraphId() { return graphId; }

    public void setGraphId(long graphId) { this.graphId = graphId; }

    public String getComponentType() { return componentType; }

    public void setComponentType(String componentType) { this.componentType = componentType; }
}
