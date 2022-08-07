package aizoo.viewObject.object;

import java.util.List;
import java.util.Map;

public class ExperimentJobVO extends BaseVO {
    private String name;

    private String jobKey;

    private String jobStatus;

    private String description;

    private Map<String, Map<String, Object>> environment;

    private String graphName;

    private Map<String, Object> args;

    private Long graphId;

    private String username;

    private List<CheckPointVO> checkPoints;

    public String getGraphName() {
        return graphName;
    }

    public void setGraphName(String graphName) {
        this.graphName = graphName;
    }

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

    public List<CheckPointVO> getCheckPoints() {
        return checkPoints;
    }

    public void setCheckPoints(List<CheckPointVO> checkPoints) {
        this.checkPoints = checkPoints;
    }

    public String getUsername() { return username; }

    public void setUsername(String username) { this.username = username; }

    @Override
    public String toString() {
        return "JobVO{" +
                "name='" + name + '\'' +
                ", jobKey='" + jobKey + '\'' +
                ", jobStatus='" + jobStatus + '\'' +
                ", description='" + description + '\'' +
                ", environment=" + environment +
                ", graphName='" + graphName + '\'' +
                ", id=" + id +
                '}';
    }

    public Long getGraphId() {
        return graphId;
    }

    public void setGraphId(Long graphId) {
        this.graphId = graphId;
    }

    public Map<String, Object> getArgs() { return args; }

    public void setArgs(Map<String, Object> args) { this.args = args; }

}
