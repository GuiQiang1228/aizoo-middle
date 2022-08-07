package aizoo.viewObject.object;

import aizoo.common.NodeType;

import javax.persistence.Lob;
import java.util.List;
import java.util.Map;

public class ServiceVO extends BaseVO {

    private String privacy;

    private String name;

    private String title;

    private String namespace;

    private String example;

    private String description;

    private String framework;

    private String frameworkVersion;

    private NodeType componentType;

    private boolean released;

    private String serviceVersion;

    private Long graphId;

    private String token;

    private Map<String,Object> properties;

    public Long getGraphId() {
        return graphId;
    }

    public void setGraphId(Long graphId) {
        this.graphId = graphId;
    }

    private List<Map<String, Object>> inputs;

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

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public NodeType getComponentType() {
        return componentType;
    }

    public void setComponentType(NodeType componentType) {
        this.componentType = componentType;
    }

    public String getServiceVersion() {
        return serviceVersion;
    }

    public void setServiceVersion(String serviceVersion) {
        this.serviceVersion = serviceVersion;
    }

    public boolean isReleased() {
        return released;
    }

    public void setReleased(boolean released) {
        this.released = released;
    }

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public String getToken() { return token; }

    public void setToken(String token) { this.token = token; }
}
