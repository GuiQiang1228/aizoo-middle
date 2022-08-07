package aizoo.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;

import javax.persistence.*;
import java.util.List;

@Entity
public class ModelCategory extends BaseDomain{
    private String name;

    private String icon;

    @Lob
    private String sceneIntroduction;

    private String scenePicture;

    @JsonBackReference(value = "modelInfo")
    @OneToMany(mappedBy = "modelCategory", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.LAZY)
    private List<ModelInfo> modelInfoList;

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getIcon() { return icon; }

    public void setIcon(String icon) { this.icon = icon; }

    public List<ModelInfo> getModelInfoList() { return modelInfoList; }

    public void setModelInfoList(List<ModelInfo> modelInfoList) { this.modelInfoList = modelInfoList; }

    public String getSceneIntroduction() { return sceneIntroduction; }

    public void setSceneIntroduction(String sceneIntroduction) { this.sceneIntroduction = sceneIntroduction; }

    public String getScenePicture() { return scenePicture; }

    public void setScenePicture(String scenePicture) { this.scenePicture = scenePicture; }
}
