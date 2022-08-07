package aizoo.elasticObject;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.elasticsearch.annotations.Document;

import javax.persistence.Id;

@Document(indexName = "datasource", type = "_doc")
public class ElasticDatasource {
    @Id
    private Long id;

    private String privacy;

    private String name;

    private String title;

    private String username;

    private String example;

    private String description;

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

    public String getExample() { return example; }

    public void setExample(String example) { this.example = example; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }
}
