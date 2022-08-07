package aizoo.viewObject.object;

import java.util.List;

public class ProjectVO extends BaseVO {
    private String name;

    private String description;

    private String privacy;

    private List<ApplicationVO> applicationVOList;

    private List<ServiceVO> serviceVOList;

    private List<ServiceJobVO> serviceJobVOList;

    private List<GraphVO> graphVOList;

    private List<ComponentVO> componentVOList;

    private List<DatasourceVO> datasourceVOList;

    private List<ExperimentJobVO> experimentJobVOList;

    private List<ProjectFileVO> projectFileVOList;

    private List<CodeVO> codeVOList;

    private List<MirrorVO> mirrorVOList;

    private List<MirrorJobVO> mirrorJobVOList;

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public String getPrivacy() { return privacy; }

    public void setPrivacy(String privacy) { this.privacy = privacy; }

    public List<ApplicationVO> getApplicationVOList() { return applicationVOList; }

    public void setApplicationVOList(List<ApplicationVO> applicationVOList) { this.applicationVOList = applicationVOList; }

    public List<ServiceVO> getServiceVOList() { return serviceVOList; }

    public void setServiceVOList(List<ServiceVO> serviceVOList) { this.serviceVOList = serviceVOList; }

    public List<ServiceJobVO> getServiceJobVOList() { return serviceJobVOList; }

    public void setServiceJobVOList(List<ServiceJobVO> serviceJobVOList) { this.serviceJobVOList = serviceJobVOList; }

    public List<GraphVO> getGraphVOList() { return graphVOList; }

    public void setGraphVOList(List<GraphVO> graphVOList) { this.graphVOList = graphVOList; }

    public List<ComponentVO> getComponentVOList() { return componentVOList; }

    public void setComponentVOList(List<ComponentVO> componentVOList) { this.componentVOList = componentVOList; }

    public List<DatasourceVO> getDatasourceVOList() { return datasourceVOList; }

    public void setDatasourceVOList(List<DatasourceVO> datasourceVOList) { this.datasourceVOList = datasourceVOList; }

    public List<ExperimentJobVO> getExperimentJobVOList() { return experimentJobVOList; }

    public void setExperimentJobVOList(List<ExperimentJobVO> experimentJobVOS) { this.experimentJobVOList = experimentJobVOS; }

    public List<ProjectFileVO> getProjectFileVOList() { return projectFileVOList; }

    public void setProjectFileVOList(List<ProjectFileVO> projectFileVOList) { this.projectFileVOList = projectFileVOList; }

    public List<CodeVO> getCodeVOList() { return codeVOList; }

    public void setCodeVOList(List<CodeVO> codeVOList) { this.codeVOList = codeVOList; }

    public List<MirrorVO> getMirrorVOList() { return mirrorVOList; }

    public void setMirrorVOList(List<MirrorVO> mirrorVOList) { this.mirrorVOList = mirrorVOList; }

    public List<MirrorJobVO> getMirrorJobVOList() { return mirrorJobVOList; }

    public void setMirrorJobVOList(List<MirrorJobVO> mirrorJobVOList) { this.mirrorJobVOList = mirrorJobVOList; }
}
