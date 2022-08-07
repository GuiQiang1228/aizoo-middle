package aizoo.viewObject.object;

import java.util.List;
import java.util.Map;

public class StructureFileVO {
    private String name;

    private String description;

    private String example;

    private String framework;

    private String frameworkVersion;

    private List<Map<String, Object>> outputs;

    private List<Map<String, Object>> inputs;

    private Map<String,Object> properties;

    private String componentVersion;

    public StructureFileVO() {
    }

    public StructureFileVO(String name, String description, String example, String framework, String frameworkVersion, String componentVersion) {
        this.name = name;
        this.description = description;
        this.example = example;
        this.framework = framework;
        this.frameworkVersion = frameworkVersion;
        this.componentVersion = componentVersion;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public List<Map<String, Object>> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<Map<String, Object>> outputs) {
        this.outputs = outputs;
    }

    public List<Map<String, Object>> getInputs() {
        return inputs;
    }

    public void setInputs(List<Map<String, Object>> inputs) {
        this.inputs = inputs;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public String getComponentVersion() {
        return componentVersion;
    }

    public void setComponentVersion(String componentVersion) {
        this.componentVersion = componentVersion;
    }
}
