package aizoo.viewObject.object;


import aizoo.domain.Project;

public class ProjectFileVO extends BaseVO{
    private String name;

    private String privacy;

    private String description;

    private String username;

    private long projectId;

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getPrivacy() { return privacy; }

    public void setPrivacy(String privacy) { this.privacy = privacy; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public String getUsername() { return username; }

    public void setUsername(String username) { this.username = username; }

    public long getProjectId() { return projectId; }

    public void setProjectId(long projectId) { this.projectId = projectId; }
}
