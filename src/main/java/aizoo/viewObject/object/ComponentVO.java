package aizoo.viewObject.object;

import aizoo.common.ComponentType;
import java.util.List;
import java.util.Map;

public class ComponentVO extends BaseVO {
    private String privacy;

    private String name;

    private String title;

    private String description;
    
    private String example;

    private Map<String,Object> properties;

    private Boolean released;

    private String framework;

    private String frameworkVersion;

    private Boolean composed;

    private String forkFromUser;//复制的哪个组件,由于绝大多数都是一样的，所以这里只留用户名

    private String username;

    private String namespace;

    private ComponentType componentType;

    private List<Map<String, Object>> inputs;

    private List<Map<String, Object>> outputs;

    private String componentVersion;

    private Long graphId;

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
    }

    public Long getGraphId() {
        return graphId;
    }

    public void setGraphId(Long graphId) {
        this.graphId = graphId;
    }

    public String getComponentVersion() {
        return componentVersion;
    }

    public void setComponentVersion(String componentVersion) {
        this.componentVersion = componentVersion;
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

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public Boolean getReleased() {
        return released;
    }

    public void setReleased(Boolean released) {
        this.released = released;
    }

    public String getFramework() {
        return framework;
    }

    public void setFramework(String framework) {
        this.framework = framework;
    }

    public String getFrameworkVersion() {
        return frameworkVersion;
    }

    public void setFrameworkVersion(String frameworkVersion) {
        this.frameworkVersion = frameworkVersion;
    }

    public Boolean getComposed() {
        return composed;
    }

    public void setComposed(Boolean composed) {
        this.composed = composed;
    }

    public String getForkFromUser() {
        return forkFromUser;
    }

    public void setForkFromUser(String forkFromUser) {
        this.forkFromUser = forkFromUser;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public ComponentType getComponentType() {
        return componentType;
    }

    public void setComponentType(ComponentType componentType) {
        this.componentType = componentType;
    }

    public List<Map<String, Object>> getInputs() {
        return inputs;
    }

    public void setInputs(List<Map<String, Object>> inputs) {
        this.inputs = inputs;
    }

    public List<Map<String, Object>> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<Map<String, Object>> outputs) {
        this.outputs = outputs;
    }

    public String getUsername() { return username; }

    public void setUsername(String username) { this.username = username; }

    @Override
    public String toString() {
        return "ComponentVO{" +
                "privacy='" + privacy + '\'' +
                ", name='" + name + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", properties=" + properties +
                ", released=" + released +
                ", framework='" + framework + '\'' +
                ", frameworkVersion='" + frameworkVersion + '\'' +
                ", composed=" + composed +
                ", forkFromUser='" + forkFromUser + '\'' +
                ", username='" + username + '\'' +
                ", namespace'=" + namespace + '\'' +
                ", componentType=" + componentType +
                ", inputs=" + inputs +
                ", outputs=" + outputs +
                ", id=" + id +
                '}';
    }
}
