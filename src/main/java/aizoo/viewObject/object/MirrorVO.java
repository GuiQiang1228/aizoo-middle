package aizoo.viewObject.object;

import java.util.Date;

public class MirrorVO extends BaseVO {

    private String description;

    private String name;

    private String userName;

    private String privacy;

    private String path;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrivacy(String privacy) { this.privacy = privacy; }

    public String getPrivacy() { return privacy; }

    public String getUserName() { return userName; }

    public void setUserName(String userName) { this.userName = userName; }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPath() { return path; }

    public void setPath(String path) { this.path = path; }
}
