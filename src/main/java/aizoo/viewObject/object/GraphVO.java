package aizoo.viewObject.object;

import aizoo.common.ComponentType;
import aizoo.common.GraphType;
import aizoo.common.Link;
import aizoo.common.Node;

import java.util.List;
import java.util.Map;


public class GraphVO extends BaseVO {
    private String name;

    private GraphType graphType;

    //    前端打开图若为COMPONENT需要知道具体类型(componentType),当为JOB/SERVICE/APPLICATION该值为null
    private ComponentType componentType;

    private String graphVersion;

    private String description;

    private Boolean graphReleased;

    private String graphKey;

    private String namespace;   // 图的复合组件或服务的命名空间

    private String graphPrivacy;  //图的权限

    private List<Node> nodeList;

    private List<Link> linkList;

    private Map<String,Object> originJson;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public GraphType getGraphType() {
        return graphType;
    }

    public void setGraphType(GraphType graphType) {
        this.graphType = graphType;
    }

    public ComponentType getComponentType() {
        return componentType;
    }

    public void setComponentType(ComponentType componentType) {
        this.componentType = componentType;
    }

    public String getGraphVersion() {
        return graphVersion;
    }

    public void setGraphVersion(String graphVersion) {
        this.graphVersion = graphVersion;
    }

    public Boolean getGraphReleased() {
        return graphReleased;
    }

    public void setGraphReleased(Boolean graphReleased) {
        this.graphReleased = graphReleased;
    }

    public String getGraphKey() {
        return graphKey;
    }

    public void setGraphKey(String graphKey) {
        this.graphKey = graphKey;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getGraphPrivacy() { return graphPrivacy; }

    public void setGraphPrivacy(String graphPrivacy) { this.graphPrivacy = graphPrivacy; }

    public List<Node> getNodeList() {
        return nodeList;
    }

    public void setNodeList(List<Node> nodeList) {
        this.nodeList = nodeList;
    }

    public List<Link> getLinkList() {
        return linkList;
    }

    public void setLinkList(List<Link> linkList) {
        this.linkList = linkList;
    }

    public Map<String, Object> getOriginJson() {
        return originJson;
    }

    public void setOriginJson(Map<String, Object> originJson) {
        this.originJson = originJson;
    }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() {
        return "GraphVO{" +
                "name='" + name + '\'' +
                ", graphKey='" + graphKey + '\'' +
                ", graphType=" + graphType +
                ", componentType=" + componentType +
                ", nodeList=" + nodeList  +
                ", linkList=" + linkList +
                ", originJson=" + originJson +
                ", id=" + id +
                '}';
    }
}
