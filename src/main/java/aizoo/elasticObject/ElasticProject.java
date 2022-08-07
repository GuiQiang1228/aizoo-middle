package aizoo.elasticObject;

import org.springframework.data.elasticsearch.annotations.Document;

import javax.persistence.Id;

@Document(indexName = "project", type = "_doc")
public class ElasticProject {
    @Id
    private Long id;

    private String name;

    private String description;

    private String username;

    private String privacy;

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getUsername() { return username; }

    public void setUsername(String username) { this.username = username; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public String getPrivacy() { return privacy; }

    public void setPrivacy(String privacy) { this.privacy = privacy; }
}
