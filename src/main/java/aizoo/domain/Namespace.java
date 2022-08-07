package aizoo.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;


@Entity
public class Namespace extends BaseDomain {
    private String privacy;

    @Column(unique = true)
    private String namespace;

    @JsonBackReference(value = "user")
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private User user;

    @JsonBackReference(value = "components")
    @OneToMany(mappedBy = "namespace", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.LAZY)
    private List<Component> components;

    public Namespace(String namespace) {
        this.namespace = namespace;
    }

    public Namespace() {

    }

    public Namespace(String namespace, User user) {
        this.namespace = namespace;
        this.user = user;
    }

    public List<Component> getComponents() {
        if (components == null)
            components = new ArrayList<>();
        return components;
    }

    public void setComponents(List<Component> components) {
        this.components = components;
    }

    public String getPrivacy() {
        return privacy;
    }

    public void setPrivacy(String privacy) {
        this.privacy = privacy;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "NamespaceVO{" +
                "privacy='" + privacy + '\'' +
                ", namespace='" + namespace + '\'' +
                ", user=" + user +
                ", id=" + id +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if(obj == null || getClass() != obj.getClass()) return false;
        Namespace namespace1 = (Namespace) obj;
        return this.getId().equals(namespace1.getId());
    }
}