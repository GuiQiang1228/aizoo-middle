package aizoo.domain;

import aizoo.common.DownloadStatus;
import com.fasterxml.jackson.annotation.JsonBackReference;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
public class MirrorJob extends Job{

    private String port;

    private String ip;

    private String name;

    private String command;

    @JsonBackReference(value = "user")
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private User user;

    @JsonBackReference(value = "mirror")
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private Mirror mirror;

    @JsonBackReference(value = "code")
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private Code code;

    @JsonBackReference(value = "projects")
    @ManyToMany(mappedBy = "mirrorJobs", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private List<Project> projects;

    @Lob
    private String userArgs;

    @Lob
    private String description;

    @Enumerated(EnumType.STRING)//枚举字符串
    private DownloadStatus downloadStatus;

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Code getCode() { return code; }

    public void setCode(Code code) { this.code = code; }

    public Mirror getMirror() { return mirror; }

    public void setMirror(Mirror mirror) { this.mirror = mirror; }

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DownloadStatus getDownloadStatus() {
        return downloadStatus;
    }

    public void setDownloadStatus(DownloadStatus downloadStatus) {
        this.downloadStatus = downloadStatus;
    }

    public String getUserArgs() { return userArgs; }

    public void setUserArgs(String userArgs) { this.userArgs = userArgs; }

    public String getCommand() { return command; }

    public void setCommand(String command) { this.command = command; }

    public List<Project> getProjects() {
        return projects;
    }

    public void setProjects(List<Project> projects) {
        this.projects = projects;
    }
}
