package aizoo.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;

import javax.persistence.*;

@Entity
public class CheckPoint extends BaseDomain {
    private String privacy;

    private String name;

    private String title;

    @Lob
    private String description;

    @Lob
    private String path;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private User user;

    @JsonBackReference(value = "experimentJob")
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private ExperimentJob experimentJob;

    //    @JsonBackReference(value = "namespace")
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private Namespace namespace;

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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ExperimentJob getExperimentJob() {
        return experimentJob;
    }

    public void setExperimentJob(ExperimentJob experimentJob) {
        this.experimentJob = experimentJob;
    }

    public Namespace getNamespace() {
        return namespace;
    }

    public void setNamespace(Namespace namespace) {
        this.namespace = namespace;
    }

    @Override
    public String toString() {
        return "CheckPoint{" +
                "name='" + name + '\'' +
                ", user=" + user +
                ", description='" + description + '\'' +
                ", id=" + id +
                '}';
    }
}
