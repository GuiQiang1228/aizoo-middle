package aizoo.domain;

import aizoo.common.ComponentType;
import aizoo.common.GraphType;
import com.fasterxml.jackson.annotation.JsonBackReference;

import javax.persistence.*;

@Entity
public class ModelInfo extends BaseDomain {
    private String privacy;

    private String name;

    private String title;

    @Lob
    private String description;

    @Lob
    private String example;

    private String framework;

    private String frameworkVersion;

    private boolean isGraph;

    private String componentVersion;

    @Enumerated(EnumType.STRING)//枚举字符串
    private GraphType graphType;

    private String graphKey;

    private String graphVersion;

    private String ChineseName;

    private String imgPath;

    private long sourceId;

    @Lob
    private String paperUrl;

    @Lob
    private String modelDescription;

    private String paperName;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private User user;

    @Enumerated(EnumType.STRING)//枚举字符串
    private ComponentType componentType;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private ModelCategory modelCategory;

    public String getPrivacy() { return privacy; }

    public void setPrivacy(String privacy) { this.privacy = privacy; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getTitle() { return title; }

    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public String getExample() { return example; }

    public void setExample(String example) { this.example = example; }

    public String getFramework() { return framework; }

    public void setFramework(String framework) { this.framework = framework; }

    public String getFrameworkVersion() { return frameworkVersion; }

    public void setFrameworkVersion(String frameworkVersion) { this.frameworkVersion = frameworkVersion; }

    public String getComponentVersion() { return componentVersion; }

    public void setComponentVersion(String componentVersion) { this.componentVersion = componentVersion; }

    public GraphType getGraphType() { return graphType; }

    public void setGraphType(GraphType graphType) { this.graphType = graphType; }

    public String getGraphVersion() { return graphVersion; }

    public void setGraphVersion(String graphVersion) { this.graphVersion = graphVersion; }

    public String getChineseName() { return ChineseName; }

    public void setChineseName(String chineseName) { ChineseName = chineseName; }

    public String getImgPath() { return imgPath; }

    public void setImgPath(String imgPath) { this.imgPath = imgPath; }

    public String getPaperUrl() { return paperUrl; }

    public void setPaperUrl(String paperUrl) { this.paperUrl = paperUrl; }

    public ModelCategory getModelCategory() { return modelCategory; }

    public void setModelCategory(ModelCategory modelCategory) { this.modelCategory = modelCategory; }

    public User getUser() { return user; }

    public void setUser(User user) { this.user = user; }

    public ComponentType getComponentType() { return componentType; }

    public void setComponentType(ComponentType componentType) { this.componentType = componentType; }

    public boolean isGraph() { return isGraph; }

    public void setGraph(boolean graph) { isGraph = graph; }

    public String getGraphKey() { return graphKey; }

    public void setGraphKey(String graphKey) { this.graphKey = graphKey; }

    public String getModelDescription() { return modelDescription; }

    public void setModelDescription(String modelDescription) { this.modelDescription = modelDescription; }

    public String getPaperName() { return paperName; }

    public void setPaperName(String paperName) { this.paperName = paperName; }

    public long getSourceId() { return sourceId; }

    public void setSourceId(long sourceId) { this.sourceId = sourceId; }
}
