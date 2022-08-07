package aizoo.domain;

import aizoo.common.ComponentType;
import com.fasterxml.jackson.annotation.JsonBackReference;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;


@Entity
public class Component extends BaseDomain implements Cloneable {
    private String privacy;

    private String name;

    private String title;

    @Lob
    private String description;

    @Lob
    private String example;

    @Lob
    private String properties;

    @Lob
    private String path;

    @Lob
    private String fileList;

    private boolean released;

    private String framework;

    private String frameworkVersion;

    private boolean composed;

    private String componentVersion;

    private String childComponentIdList;   // 存放复合组件下一层组件的id列表，注意只存放下一层的子组件

//    private String optimization;

    @JsonBackReference(value = "forkFrom")
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "fork_from", referencedColumnName = "id")
    private Component forkFrom;//复制的哪个组件

    @JsonBackReference(value = "forkBy")
    @OneToMany(mappedBy = "forkFrom", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.LAZY)
    private List<Component> forkBy;//被哪些组件复制了

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private User user;

    @JsonBackReference(value = "graph")
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private Graph graph;

    //    @JsonBackReference(value = "namespace")
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private Namespace namespace;

    @Enumerated(EnumType.STRING)//枚举字符串
    private ComponentType componentType;

    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(mappedBy = "component", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.EAGER)
    private List<ComponentInputParameter> inputs;

    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(mappedBy = "component", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.EAGER)
    private List<ComponentOutputParameter> outputs;

    @JsonBackReference
    @ManyToMany(mappedBy = "components", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private List<Project> projects;

    public Component() {
    }

    public Component(String privacy, String name, String title, String description, String properties, String path, String fileList, boolean released, String framework, String frameworkVersion, boolean composed, Component forkFrom, User user, Namespace namespace, ComponentType componentType, String componentVersion) {
        this.privacy = privacy;
        this.name = name;
        this.title = title;
        this.description = description;
        this.properties = properties;
        this.path = path;
        this.fileList = fileList;
        this.released = released;
        this.framework = framework;
        this.frameworkVersion = frameworkVersion;
        this.composed = composed;
        this.forkFrom = forkFrom;
        this.user = user;
        this.namespace = namespace;
        this.componentType = componentType;
        this.componentVersion = componentVersion;
    }

    public String getComponentVersion() {
        return componentVersion;
    }

    public void setComponentVersion(String componentVersion) {
        this.componentVersion = componentVersion;
    }

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

    public String getProperties() {
        return properties;
    }

    public void setProperties(String properties) {
        this.properties = properties;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFileList() {
        return fileList;
    }

    public void setFileList(String fileList) {
        this.fileList = fileList;
    }

    public boolean isReleased() {
        return released;
    }

    public void setReleased(boolean released) {
        this.released = released;
    }

    public boolean isComposed() {
        return composed;
    }

    public void setComposed(boolean composed) {
        this.composed = composed;
    }

    public Component getForkFrom() {
        return forkFrom;
    }

    public void setForkFrom(Component forkFrom) {
        this.forkFrom = forkFrom;
    }

    public List<Component> getForkBy() {
        if (forkBy == null)
            forkBy = new ArrayList<>();
        return forkBy;
    }

    public void setForkBy(List<Component> forkBy) {
        this.forkBy = forkBy;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Namespace getNamespace() {
        return namespace;
    }

    public void setNamespace(Namespace namespace) {
        this.namespace = namespace;
    }

    public ComponentType getComponentType() {
        return componentType;
    }

    public void setComponentType(ComponentType componentType) {
        this.componentType = componentType;
    }

    public List<ComponentInputParameter> getInputs() {
        if (inputs == null)
            inputs = new ArrayList<>();
        return inputs;
    }

    public void setInputs(List<ComponentInputParameter> inputs) {
        this.inputs = inputs;
    }

    public List<ComponentOutputParameter> getOutputs() {
        if (outputs == null)
            outputs = new ArrayList<>();
        return outputs;
    }

    public void setOutputs(List<ComponentOutputParameter> outputs) {
        this.outputs = outputs;
    }

    public Graph getGraph() {
        return graph;
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
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

    public String getChildComponentIdList() {
        return childComponentIdList;
    }

    public void setChildComponentIdList(String childComponentIdList) {
        this.childComponentIdList = childComponentIdList;
    }

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
    }

    @Override
    public Object clone() {
        Component component = null;
        try{
            component = (Component) super.clone();
        }catch(CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return component;
    }

    public List<Project> getProjects() { return projects; }

    public void setProjects(List<Project> projects) { this.projects = projects; }
}