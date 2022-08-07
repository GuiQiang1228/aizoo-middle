package aizoo.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Service extends BaseDomain {

    private String privacy;

    private String name;

    private String title;

    @Lob
    private String description;

    @Lob
    private String example;

    @Lob
    private String path;

    @Lob
    private String fileList;

    private boolean released;

    private String framework;

    private String frameworkVersion;

    private String serviceVersion;

    @Lob
    private String properties;

    private String token;

    @JsonBackReference(value = "forkFrom")
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "fork_from", referencedColumnName = "id")
    private Service forkFrom;//复制的哪个组件

    @JsonBackReference(value = "forkBy")
    @OneToMany(mappedBy = "forkFrom", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.LAZY)
    private List<Service> forkBy;//被哪些组件复制了

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private User user;

    @JsonBackReference(value = "graph")
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private Graph graph;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private Namespace namespace;

    @OneToMany(mappedBy = "service", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.LAZY)
    private List<ServiceInputParameter> inputs;

    @OneToMany(mappedBy = "service", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.LAZY)
    private List<ServiceOutputParameter> outputs;

    @JsonBackReference
    @ManyToMany(mappedBy = "services", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private List<Project> projects;

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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isReleased() {
        return released;
    }

    public void setReleased(boolean released) {
        this.released = released;
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

    public Service getForkFrom() {
        return forkFrom;
    }

    public void setForkFrom(Service forkFrom) {
        this.forkFrom = forkFrom;
    }

    public List<Service> getForkBy() {
        return forkBy;
    }

    public void setForkBy(List<Service> forkBy) {
        this.forkBy = forkBy;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Graph getGraph() {
        return graph;
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    public Namespace getNamespace() {
        return namespace;
    }

    public void setNamespace(Namespace namespace) {
        this.namespace = namespace;
    }

    public String getServiceVersion() {
        return serviceVersion;
    }

    public void setServiceVersion(String serviceVersion) {
        this.serviceVersion = serviceVersion;
    }

    public List<ServiceInputParameter> getInputs() {
        if (inputs == null)
            inputs = new ArrayList<>();
        return inputs;
    }

    public void setInputs(List<ServiceInputParameter> inputs) {
        this.inputs = inputs;
    }

    public List<ServiceOutputParameter> getOutputs() {
        if (outputs == null)
            outputs = new ArrayList<>();
        return outputs;
    }

    public void setOutputs(List<ServiceOutputParameter> outputs) {
        this.outputs = outputs;
    }

    public String getFileList() {
        return fileList;
    }

    public void setFileList(String fileList) {
        this.fileList = fileList;
    }

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
    }

    public String getProperties() {
        return properties;
    }

    public void setProperties(String properties) {
        this.properties = properties;
    }

    public String getToken() { return token; }

    public void setToken(String token) { this.token = token; }

    public List<Project> getProjects() { return projects; }

    public void setProjects(List<Project> projects) { this.projects = projects; }
}
