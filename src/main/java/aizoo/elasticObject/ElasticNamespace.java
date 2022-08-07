package aizoo.elasticObject;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

import javax.persistence.Id;

@Document(indexName = "namespace", type = "_doc")
public class ElasticNamespace {
    @Id
    private Long id;

    private String namespace;

    private String privacy;

    private String username;

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public String getNamespace() { return namespace; }

    public void setNamespace(String namespace) { this.namespace = namespace; }

    public String getUsername() { return username; }

    public void setUsername(String username) { this.username = username; }

    public String getPrivacy() { return privacy; }

    public void setPrivacy(String privacy) { this.privacy = privacy; }
}
