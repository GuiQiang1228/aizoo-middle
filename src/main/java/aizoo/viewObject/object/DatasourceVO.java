package aizoo.viewObject.object;

import aizoo.common.ComponentType;
import java.util.List;
import java.util.Map;

public class DatasourceVO extends BaseVO {
    private String privacy;

    private String name;

    private String title;

    private String description;

    private String example;

    private ComponentType componentType;

    private String username;

    private String namespace;

    private String port;

    private String sqlUsername;

    private String password;

    private String databaseName;

    private String host;

    private String path;

    private List<Map<String, Object>> outputs;

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

    public String getUsername() { return username; }

    public void setUsername(String username) { this.username = username; }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public List<Map<String, Object>> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<Map<String, Object>> outputs) {
        this.outputs = outputs;
    }

    public ComponentType getComponentType() {
        return componentType;
    }

    public void setComponentType(ComponentType componentType) {
        this.componentType = componentType;
    }

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
    }

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

    @Override
    public String toString() {
        return "DatasourceVO{" +
                "privacy='" + privacy + '\'' +
                ", name='" + name + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", componentType=" + componentType +
                ", username='" + username + '\'' +
                ", namespace='" + namespace + '\'' +
                ", outputs=" + outputs +
                ", id=" + id +
                '}';
    }

    public String getPath() { return path; }

    public void setPath(String path) { this.path = path; }
}
