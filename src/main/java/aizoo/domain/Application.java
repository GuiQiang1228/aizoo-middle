package aizoo.domain;

import aizoo.common.JobStatus;
import com.fasterxml.jackson.annotation.JsonBackReference;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.List;

@Entity
public class Application extends Job{
    private String privacy = "private";

    private String name;

    private String title;

    @Lob
    private String description;

    private String outLogUrl;

    private String errorLogUrl;

    @JsonBackReference(value = "user")
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private User user;

    @JsonBackReference(value = "graph")
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private Graph graph;

    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(mappedBy = "application", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.EAGER)
    List<ApplicationResult> appResults;

    @JsonBackReference
    @ManyToMany(mappedBy = "applications", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
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

    public List<ApplicationResult> getAppResults() {
        return appResults;
    }

    public void setAppResults(List<ApplicationResult> appResults) {
        this.appResults = appResults;
    }

    public String getOutLogUrl() {
        return outLogUrl;
    }

    public void setOutLogUrl(String outLogUrl) {
        this.outLogUrl = outLogUrl;
    }

    public String getErrorLogUrl() {
        return errorLogUrl;
    }

    public void setErrorLogUrl(String errorLogUrl) {
        this.errorLogUrl = errorLogUrl;
    }

    public List<Project> getProjects() { return projects; }

    public void setProjects(List<Project> projects) { this.projects = projects; }
}
