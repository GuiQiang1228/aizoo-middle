package aizoo.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;

import javax.persistence.*;
import java.util.List;

@Entity
public class Project extends BaseDomain{
    private String name;

    @Lob
    private String description;

    @JsonBackReference(value = "user")
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private User user;

    private String privacy;

    public Project(){

    }

    public Project(String projectName, User user, String privacy, String desc){
        this.name = projectName;
        this.user = user;
        this.privacy = privacy;
        this.description = desc;
    }

    @JsonBackReference(value = "applications")
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinTable(name = "project_application",
            joinColumns = {@JoinColumn(name = "project_id", referencedColumnName = "id")},
            inverseJoinColumns = {@JoinColumn(name = "application_id", referencedColumnName = "id")}
    )
    private List<Application> applications;

    @JsonBackReference(value = "components")
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinTable(name = "project_component",
            joinColumns = {@JoinColumn(name = "project_id", referencedColumnName = "id")},
            inverseJoinColumns = {@JoinColumn(name = "component_id", referencedColumnName = "id")}
    )
    private List<Component> components;

    @JsonBackReference(value = "datasourceList")
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinTable(name = "project_datasource",
            joinColumns = {@JoinColumn(name = "project_id", referencedColumnName = "id")},
            inverseJoinColumns = {@JoinColumn(name = "datasource_id", referencedColumnName = "id")}
    )
    private List<Datasource> datasourceList;

    @JsonBackReference(value = "experimentJobs")
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinTable(name = "project_experimentJob",
            joinColumns = {@JoinColumn(name = "project_id", referencedColumnName = "id")},
            inverseJoinColumns = {@JoinColumn(name = "experimentJob_id", referencedColumnName = "id")}
    )
    private List<ExperimentJob> experimentJobs;

    @JsonBackReference(value = "graphs")
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinTable(name = "project_graph",
            joinColumns = {@JoinColumn(name = "project_id", referencedColumnName = "id")},
            inverseJoinColumns = {@JoinColumn(name = "graph_id", referencedColumnName = "id")}
    )
    private List<Graph> graphs;

    @JsonBackReference(value = "services")
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinTable(name = "project_service",
            joinColumns = {@JoinColumn(name = "project_id", referencedColumnName = "id")},
            inverseJoinColumns = {@JoinColumn(name = "service_id", referencedColumnName = "id")}
    )
    private List<Service> services;

    @JsonBackReference(value = "serviceJobs")
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinTable(name = "project_serviceJob",
            joinColumns = {@JoinColumn(name = "project_id", referencedColumnName = "id")},
            inverseJoinColumns = {@JoinColumn(name = "serviceJob_id", referencedColumnName = "id")}
    )
    private List<ServiceJob> serviceJobs;

    @JsonBackReference(value = "codes")
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinTable(name = "project_code",
            joinColumns = {@JoinColumn(name = "project_id", referencedColumnName = "id")},
            inverseJoinColumns = {@JoinColumn(name = "code_id", referencedColumnName = "id")}
    )
    private List<Code> codes;

    @JsonBackReference(value = "mirrors")
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinTable(name = "project_mirror",
            joinColumns = {@JoinColumn(name = "project_id", referencedColumnName = "id")},
            inverseJoinColumns = {@JoinColumn(name = "mirror_id", referencedColumnName = "id")}
    )
    private List<Mirror> mirrors;

    @JsonBackReference(value = "mirrorJobs")
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinTable(name = "project_mirrorJob",
            joinColumns = {@JoinColumn(name = "project_id", referencedColumnName = "id")},
            inverseJoinColumns = {@JoinColumn(name = "mirrorJob_id", referencedColumnName = "id")}
    )
    private List<MirrorJob> mirrorJobs;

    @OneToMany(mappedBy = "project", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private List<ProjectFile> projectFiles;

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public User getUser() { return user; }

    public void setUser(User user) { this.user = user; }

    public String getPrivacy() { return privacy; }

    public void setPrivacy(String privacy) { this.privacy = privacy; }

    public List<Application> getApplications() { return applications; }

    public void setApplications(List<Application> applications) { this.applications = applications; }

    public List<Component> getComponents() { return components; }

    public void setComponents(List<Component> components) { this.components = components; }

    public List<Datasource> getDatasourceList() { return datasourceList; }

    public void setDatasourceList(List<Datasource> datasourceList) { this.datasourceList = datasourceList; }

    public List<ExperimentJob> getExperimentJobs() { return experimentJobs; }

    public void setExperimentJobs(List<ExperimentJob> experimentJobs) { this.experimentJobs = experimentJobs; }

    public List<Graph> getGraphs() { return graphs; }

    public void setGraphs(List<Graph> graphs) { this.graphs = graphs; }

    public List<Service> getServices() { return services; }

    public void setServices(List<Service> services) { this.services = services; }

    public List<ServiceJob> getServiceJobs() { return serviceJobs; }

    public void setServiceJobs(List<ServiceJob> serviceJobs) { this.serviceJobs = serviceJobs; }

    public List<ProjectFile> getProjectFiles() { return projectFiles; }

    public List<Code> getCodes() { return codes; }

    public void setCodes(List<Code> codes) { this.codes = codes; }

    public List<Mirror> getMirrors() { return mirrors; }

    public void setMirrors(List<Mirror> mirrors) { this.mirrors = mirrors; }

    public List<MirrorJob> getMirrorJobs() { return mirrorJobs; }

    public void setMirrorJobs(List<MirrorJob> mirrorJobs) { this.mirrorJobs = mirrorJobs; }

    public void setProjectFiles(List<ProjectFile> projectFiles) { this.projectFiles = projectFiles; }
}
