package aizoo.viewObject.object.canLink;

import aizoo.common.ComponentType;

public class ConnectNodeVO {
    private String nodeName;

    private String nodeId;

    private String nodeNamespace;

    private String endpointDataType;

    private String endpointId;

    private String endpointName;

    private ComponentType componentType;

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeNamespace() {
        return nodeNamespace;
    }

    public void setNodeNamespace(String nodeNamespace) {
        this.nodeNamespace = nodeNamespace;
    }

    public String getEndpointDataType() {
        return endpointDataType;
    }

    public void setEndpointDataType(String endpointDataType) {
        this.endpointDataType = endpointDataType;
    }

    public String getEndpointId() {
        return endpointId;
    }

    public void setEndpointId(String endpointId) {
        this.endpointId = endpointId;
    }

    public String getEndpointName() {
        return endpointName;
    }

    public void setEndpointName(String endpointName) {
        this.endpointName = endpointName;
    }

    public ComponentType getComponentType() {
        return componentType;
    }

    public void setComponentType(ComponentType componentType) {
        this.componentType = componentType;
    }

    @Override
    public String toString(){
        return "ConnectNodeVO{" +
                "nodeName='" + nodeName + '\'' +
                ", nodeId='" + nodeId + '\'' +
                ", Namespace='" + nodeNamespace +
                ", endpointDataType='" + endpointDataType + '\'' +
                ", endpointId='" + endpointId + '\'' +
                ", endpointName=" + endpointName + '\'' +
                ", componentType=" + componentType +
                '}';
    }
}
