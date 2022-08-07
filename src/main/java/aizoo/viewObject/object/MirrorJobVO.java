package aizoo.viewObject.object;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class MirrorJobVO extends BaseVO {
    private String name;

    private String jobKey;

    private String jobStatus;

    private String port;

    private String description;

    private Map<String, Map<String, Object>> environment;

    private String username;

    private Map<String, Object> args;

    private Map<String, Object> userArgs;

    private String command;

    private long mirrorId;

    private long codeId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getJobKey() {
        return jobKey;
    }

    public void setJobKey(String jobKey) {
        this.jobKey = jobKey;
    }

    public String getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(String jobStatus) {
        this.jobStatus = jobStatus;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Map<String, Object>> getEnvironment() {
        return environment;
    }

    public void setEnvironment(Map<String, Map<String, Object>> environment) {
        this.environment = environment;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return "JobVO{" +
                "name='" + name + '\'' +
                ", jobKey='" + jobKey + '\'' +
                ", jobStatus='" + jobStatus + '\'' +
                ", description='" + description + '\'' +
                ", environment=" + environment +
                ", id=" + id +
                '}';
    }

    public Map<String, Object> getArgs() { return args; }

    public void setArgs(Map<String, Object> args) { this.args = args; }

    public String getPort() {  return port; }

    public void setPort(String port) { this.port = port; }

    public String getCommand() { return command; }

    public void setCommand(String command) { this.command = command; }

    public Map<String, Object> getUserArgs() { return userArgs; }

    public void setUserArgs(Map<String, Object> userArgs) { this.userArgs = userArgs; }

    public long getMirrorId() {
        return mirrorId;
    }

    public void setMirrorId(long mirrorId) {
        this.mirrorId = mirrorId;
    }

    public long getCodeId() { return codeId; }

    public void setCodeId(long codeId) { this.codeId = codeId; }
}
