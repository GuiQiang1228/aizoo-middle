package aizoo.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;

import javax.persistence.*;

@Entity
public class Model extends BaseDomain{
    private String name;

    @JsonBackReference(value = "graph")
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private Graph graph;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private User user;

    @Lob
    private String description;

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public Graph getGraph() { return graph; }

    public void setGraph(Graph graph) { this.graph = graph; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public User getUser() { return user; }

    public void setUser(User user) { this.user = user; }
}
