package aizoo.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

@Entity
public class ProjectFile extends BaseDomain {
    private String name;

    @Lob
    private String path;

    private String privacy;

    @Lob
    private String description;

    @JsonBackReference(value = "project")
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private Project project;

    @JsonBackReference(value = "user")
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private User user;

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getPath() { return path; }

    public void setPath(String path) { this.path = path; }

    public Project getProject() { return project; }

    public void setProject(Project project) { this.project = project; }

    public User getUser() { return user; }

    public void setUser(User user) { this.user = user; }

    public String getPrivacy() { return privacy; }

    public void setPrivacy(String privacy) { this.privacy = privacy; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }
}
