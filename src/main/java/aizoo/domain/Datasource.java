package aizoo.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;


@Entity
public class Datasource extends BaseDomain {
    private String privacy;
    private String name;
    private String title;
    @Lob
    private String description;

    @Lob
    private String example;

    private String port;

    private String sqlUsername;

    private String password;

    private String databaseName;

    private String host;

    @Lob
    private String path;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private User user;

    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private Namespace namespace;

    @OneToMany(mappedBy = "datasource", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.LAZY)
    private List<DatasourceOutputParameter> datasourceOutputParameters;

    @JsonBackReference
    @ManyToMany(mappedBy = "datasourceList", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private List<Project> projects;

    @JsonBackReference(value = "forkFrom")
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "fork_from", referencedColumnName = "id")
    private Datasource forkFrom;//复制的哪个数据集

    @JsonBackReference(value = "forkBy")
    @OneToMany(mappedBy = "forkFrom", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.LAZY)
    private List<Datasource> forkBy;//被哪些数据集复制了

    public Datasource(Long id, String privacy, String name, String title, String description, String path, User user, Namespace namespace) {
        this.id = id;
        this.privacy = privacy;
        this.name = name;
        this.title = title;
        this.description = description;
        this.path = path;
        this.user = user;
        this.namespace = namespace;
    }

    public Datasource() {
    }

    public String getPrivacy() {
        return privacy;
    }

    public void setPrivacy(String privacy) {
        this.privacy = privacy;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Namespace getNamespace() {
        return namespace;
    }

    public void setNamespace(Namespace namespace) {
        this.namespace = namespace;
    }

    public List<DatasourceOutputParameter> getDatasourceOutputParameters() {
        if (datasourceOutputParameters == null)
            datasourceOutputParameters = new ArrayList<>();
        return datasourceOutputParameters;
    }

    public void setDatasourceOutputParameters(List<DatasourceOutputParameter> datasourceOutputParameters) {
        this.datasourceOutputParameters = datasourceOutputParameters;
    }

    public List<Project> getProjects() { return projects; }

    public void setProjects(List<Project> projects) { this.projects = projects; }

    public String getPort() { return port; }

    public void setPort(String port) { this.port = port; }

    public String getSqlUsername() { return sqlUsername; }

    public void setSqlUsername(String sqlUsername) { this.sqlUsername = sqlUsername; }

    public String getPassword() { return password; }

    public void setPassword(String password) { this.password = password; }

    public String getHost() { return host; }

    public void setHost(String host) { this.host = host; }

    public String getDatabaseName() { return databaseName; }

    public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }

    public Datasource getForkFrom() {
        return forkFrom;
    }

    public void setForkFrom(Datasource forkFrom) { this.forkFrom = forkFrom; }

    public List<Datasource> getForkBy() { return forkBy; }

    public void setForkBy(List<Datasource> forkBy) { this.forkBy = forkBy; }
}