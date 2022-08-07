package aizoo.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
public class Mirror extends BaseDomain{

    @JsonBackReference(value = "user")
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private User user;

    @JsonBackReference(value = "mirrorJobs")
    @OneToMany(mappedBy = "mirror", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.LAZY)
    List<MirrorJob> mirrorJobs;

    @JsonBackReference(value = "projects")
    @ManyToMany(mappedBy = "mirrors", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private List<Project> projects;

    private String name;

    private String privacy;

    private String path;

    @Lob
    private String description;

    public List<MirrorJob> getMirrorJobs() { return mirrorJobs; }

    public void setMirrorJobs(List<MirrorJob> mirrorJobs) { this.mirrorJobs = mirrorJobs; }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public void setPrivacy(String privacy) { this.privacy = privacy; }

    public String getPrivacy() { return privacy; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public String getPath() { return path; }

    public void setPath(String path) { this.path = path; }

    public List<Project> getProjects() {
        return projects;
    }

    public void setProjects(List<Project> projects) {
        this.projects = projects;
    }
}
