package aizoo.domain;

import aizoo.common.LevelType;
import aizoo.common.UserStatusType;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.*;
import java.util.*;

/**
 * 用户表user对应的类，同时实现了UserDetails接口，成为登录验证的信息类
 */

@Entity
public class User extends BaseDomain implements UserDetails {

    @Column(unique = true)
    private String username;
    @JsonBackReference(value = "password")
    private String password;

    @JsonBackReference(value = "email")
    private String email;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date levelStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date levelFinishTime;


    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private Level level;

    @Enumerated(EnumType.STRING)
    private UserStatusType statusName;


    @JsonBackReference(value = "roles")
    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinTable(name = "user_roles",
            joinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "id")},
            inverseJoinColumns = {@JoinColumn(name = "role_id", referencedColumnName = "id")}
    )
    private List<Role> roles;


    //一对多的一方，不维护任何关系，由多方全权维护
    // CascadeType.PERSIST: 如果user的namespace不存在，则一并保存，但namespace的user没有被set，则表中不会有userid
    //CascadeType.MERGE：如果保存了user，顺便更新了对应namespace，会一并被更新
    //CascadeType.REFRESH：两方如果同时修改user，在save之前会先查询一遍，获取最新数据
    @JsonBackReference(value = "namespaces")
    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.LAZY)
    private List<Namespace> namespaces;

    @JsonBackReference(value = "forkEdition")
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private ForkEdition forkEdition;


    @JsonBackReference(value = "datatype")
    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.LAZY)
    private List<Datatype> datatype;

    @JsonBackReference(value = "components")
    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.LAZY)
    private List<Component> components;

    @JsonBackReference(value = "graphs")
    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.LAZY)
    private List<Graph> graphs;

    @JsonBackReference(value = "experimentJobs")
    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.LAZY)
    private List<ExperimentJob> experimentJobs;

    @JsonBackReference(value = "slurmAccount")
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private SlurmAccount slurmAccount;

    public User() {
    }

    public User(String username, String email) {
        this.username = username;
        this.email = email;
    }

    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }

    public User(String username) {
        this.username = username;
    }

    public List<Namespace> getNamespaces() {
        if (namespaces == null)
            namespaces = new ArrayList<>();
        return namespaces;
    }

    public List<Datatype> getDatatype() {
        if (datatype == null)
            datatype = new ArrayList<>();
        return datatype;
    }

    public List<Component> getComponents() {
        if (components == null)
            components = new ArrayList<>();
        return components;
    }

    public void setComponents(List<Component> components) {
        this.components = components;
    }

    public List<Graph> getGraphs() {
        if (graphs == null)
            graphs = new ArrayList<>();
        return graphs;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public List<ExperimentJob> getExperimentJobs() {
        if (experimentJobs == null)
            experimentJobs = new ArrayList<>();
        return experimentJobs;
    }

    public void setExperimentJobs(List<ExperimentJob> experimentJobs) {
        this.experimentJobs = experimentJobs;
    }

    public void setGraphs(List<Graph> graphs) {
        this.graphs = graphs;
    }

    public void setDatatype(List<Datatype> datatype) {
        this.datatype = datatype;
    }

    public void setNamespaces(List<Namespace> namespaces) {
        this.namespaces = namespaces;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }

    /**
     * 从数据库中取出roles字符串后，进行分解，构成一个GrantedAuthority的List返回
     *
     * @return
     */
    @Override
    @JsonBackReference(value = "getAuthorities")
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<SimpleGrantedAuthority> simpleGrantedAuthorities = new ArrayList<>();
        for (Role role : roles) {
            simpleGrantedAuthorities.add(new SimpleGrantedAuthority(role.getUserRoleType().getValue()));
        }
        return simpleGrantedAuthorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Date getLevelStartTime() {
        return levelStartTime;
    }

    public Date getLevelFinishTime() {
        return levelFinishTime;
    }

    public void setLevelStartTime(Date levelStartTime) {
        this.levelStartTime = levelStartTime;
    }

    public void setLevelFinishTime(Date levelFinishTime) {
        this.levelFinishTime = levelFinishTime;
    }

    public UserStatusType getStatusName() {
        return statusName;
    }

    public void setStatusName(UserStatusType statusName) {
        this.statusName = statusName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public SlurmAccount getSlurmAccount() {
        return slurmAccount;
    }

    public void setSlurmAccount(SlurmAccount slurmAccount) {
        this.slurmAccount = slurmAccount;
    }

    public ForkEdition getForkEdition() {
        return forkEdition;
    }

    public void setForkEdition(ForkEdition forkEdition) {
        this.forkEdition = forkEdition;
    }

    @Override
    @JsonBackReference(value = "isAccountNonExpired")
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    @JsonBackReference(value = "isAccountNonLocked")
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    @JsonBackReference(value = "isCredentialsNonExpired")
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    @JsonBackReference(value = "isEnabled")
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return username.equals(user.username) &&
                email.equals(user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, email);
    }
}