package aizoo.viewObject.object;


import aizoo.common.NodeType;

import java.util.List;
import java.util.Map;

public class VisualContainerVO extends BaseVO{
    private String name;

    private String title;

    private String privacy;

    private String description;

    private String example;

    private String path;

    private NodeType componentType;

    private Map<String,Object> properties;

    private List<Map<String, Object>> inputs;

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

    public String getPrivacy() {
        return privacy;
    }

    public void setPrivacy(String privacy) {
        this.privacy = privacy;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<Map<String, Object>> getInputs() {
        return inputs;
    }

    public void setInputs(List<Map<String, Object>> inputs) {
        this.inputs = inputs;
    }

    public NodeType getComponentType() {
        return componentType;
    }

    public void setComponentType(NodeType componentType) {
        this.componentType = componentType;
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
}
