package aizoo.common;

import aizoo.viewObject.object.*;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;


public class Node {
//    这几项VO只有一项非空，剩余都为空
    @JsonProperty(value = "component")
    private ComponentVO componentVO;

    @JsonProperty(value = "datasource")
    private DatasourceVO datasourceVO;

    @JsonProperty(value = "service")
    private ServiceVO serviceVO;

    @JsonProperty(value = "visualContainer")
    private VisualContainerVO visualContainerVO;

    private String id;

    private NodeType componentType;

    private String componentVersion;

    //    变量名
    private String variable;

    private String parameterName;

    private String dataloaderType;

    //graphType为JOB的是saveOutput,是端点的id集合
    private List<Long> saveOutput;

    //graphType为COMPONENT的是 exposedOutput,是端点的id集合
    private List<Long> exposedOutput;

    private Long checkPointId;

    private Long serviceJobId;

    public String getComponentVersion() {
        return componentVersion;
    }

    public void setComponentVersion(String componentVersion) {
        this.componentVersion = componentVersion;
    }

    public ComponentVO getComponentVO() {
        return componentVO;
    }

    public void setComponentVO(ComponentVO componentVO) {
        this.componentVO = componentVO;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVariable() {
        return variable;
    }

    public void setVariable(String variable) {
        this.variable = variable;
    }

    public List<Long> getSaveOutput() {
        return saveOutput;
    }

    public void setSaveOutput(List<Long> saveOutput) {
        this.saveOutput = saveOutput;
    }

    public List<Long> getExposedOutput() {
        return exposedOutput;
    }

    public void setExposedOutput(List<Long> exposedOutput) {
        this.exposedOutput = exposedOutput;
    }

    public DatasourceVO getDatasourceVO() {
        return datasourceVO;
    }

    public void setDatasourceVO(DatasourceVO datasourceVO) {
        this.datasourceVO = datasourceVO;
    }

    public NodeType getComponentType() {
        return componentType;
    }

    public void setComponentType(NodeType componentType) {
        this.componentType = componentType;
    }

    public String getDataloaderType() {
        return dataloaderType;
    }

    public void setDataloaderType(String dataloaderType) {
        this.dataloaderType = dataloaderType;
    }

    public ServiceVO getServiceVO() {
        return serviceVO;
    }

    public void setServiceVO(ServiceVO serviceVO) {
        this.serviceVO = serviceVO;
    }

    public VisualContainerVO getVisualContainerVO() {
        return visualContainerVO;
    }

    public void setVisualContainerVO(VisualContainerVO visualContainerVO) {
        this.visualContainerVO = visualContainerVO;
    }

    public Long getServiceJobId() {
        return serviceJobId;
    }

    public void setServiceJobId(Long serviceJobId) {
        this.serviceJobId = serviceJobId;
    }

    public Long getCheckPointId() {
        return checkPointId;
    }

    public void setCheckPointId(Long checkPointId) {
        this.checkPointId = checkPointId;
    }

    public String getParameterName() {
        return parameterName;
    }

    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }

    @Override
    public String toString() {
        return "Node{" +
                "componentVO=" + componentVO +
                ", datasourceVO=" + datasourceVO +
                ", serviceVO=" + serviceVO +
                ", visualContainerVO=" + visualContainerVO +
                ", id='" + id + '\'' +
                ", componentType=" + componentType +
                ", variable='" + variable + '\'' +
                ", dataloaderType='" + dataloaderType + '\'' +
                ", saveOutput=" + saveOutput +
                ", exposedOutput=" + exposedOutput +
                '}';
    }
}
