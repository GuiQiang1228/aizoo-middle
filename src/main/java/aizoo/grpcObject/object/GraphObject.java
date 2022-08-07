package aizoo.grpcObject.object;

import aizoo.common.GraphType;
import aizoo.domain.Component;

import java.util.List;
import java.util.Map;

public class GraphObject {
    private String name;

    private Map<String,Object> component;

    private GraphType graphType;

    private String graphKey;

    private String token;

    private Integer port;

    //nodeList中的node保存component和datasource，而不是componentVO和datasourceVO
    private List<Map<String,Object>> nodeList;

    private List<Map<String,Object>> linkList;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getComponent() {
        return component;
    }

    public void setComponent(Map<String, Object> component) {
        this.component = component;
    }

    public GraphType getGraphType() {
        return graphType;
    }

    public void setGraphType(GraphType graphType) {
        this.graphType = graphType;
    }

    public String getGraphKey() {
        return graphKey;
    }

    public void setGraphKey(String graphKey) {
        this.graphKey = graphKey;
    }

    public List<Map<String, Object>> getNodeList() {
        return nodeList;
    }

    public void setNodeList(List<Map<String, Object>> nodeList) {
        this.nodeList = nodeList;
    }

    public List<Map<String, Object>> getLinkList() {
        return linkList;
    }

    public void setLinkList(List<Map<String, Object>> linkList) {
        this.linkList = linkList;
    }

    public String getToken() { return token; }

    public void setToken(String token) { this.token = token; }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }
}
