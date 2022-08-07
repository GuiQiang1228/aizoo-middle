package aizoo.domain;

import aizoo.common.GraphType;
import com.fasterxml.jackson.annotation.JsonBackReference;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;


@Entity
public class Graph extends BaseDomain {
    private String name;
    @JsonBackReference(value = "user")
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private User user;

    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private Component component;

    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private Service service;

    @OneToMany(mappedBy = "graph",cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private List<Application> applications;

    @OneToMany(mappedBy = "graph",cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private List<ExperimentJob> experimentJobs;

    @Enumerated(EnumType.STRING)//枚举字符串
    private GraphType graphType;

    private String graphKey;

    private String graphVersion;

    private boolean released;

    @Lob
    private String description;

    @Lob
    private String nodeList;

    @Lob
    private String linkList;

    @Lob
    private String originJson;

    @JsonBackReference
    @ManyToMany(mappedBy = "graphs", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private List<Project> projects;

    public Graph() {
    }

    public Graph(String name) {
        this.name = name;
    }

    public Graph(String name, String graphKey, String nodeList, String linkList) {
        this.name = name;
        this.graphKey = graphKey;
        this.nodeList = nodeList;
        this.linkList = linkList;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Component getComponent() {
        return component;
    }

    public void setComponent(Component component) {
        this.component = component;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
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

    public String getGraphVersion() {
        return graphVersion;
    }

    public void setGraphVersion(String graphVersion) {
        this.graphVersion = graphVersion;
    }

    public boolean isReleased() {
        return released;
    }

    public void setReleased(boolean released) {
        this.released = released;
    }

    public String getNodeList() {
        return nodeList;
    }

    public void setNodeList(String nodeList) {
        this.nodeList = nodeList;
    }

    public String getLinkList() {
        return linkList;
    }

    public void setLinkList(String linkList) {
        this.linkList = linkList;
    }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public List<ExperimentJob> getJobs() {
        if (experimentJobs == null)
            experimentJobs = new ArrayList<>();
        return experimentJobs;
    }

    public void setJobs(List<ExperimentJob> experimentJobs) {
        this.experimentJobs = experimentJobs;
    }

    public String getOriginJson() {
        return originJson;
    }

    public void setOriginJson(String originJson) {
        this.originJson = originJson;
    }

    public List<Application> getApplications() {
        if (applications == null)
            applications = new ArrayList<>();
        return applications;
    }

    public void setApplications(List<Application> application) {
        this.applications = application;
    }

    public List<Project> getProjects() { return projects; }

    public void setProjects(List<Project> projects) { this.projects = projects; }
}