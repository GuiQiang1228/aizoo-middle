package aizoo.viewObject.object;

import aizoo.common.JobStatus;

import java.util.Map;

public class ServiceJobVO extends BaseVO {
    private String port;

    private String ip;

    private String url;

    private String name;

    private String jobKey;

    private String graphName;

    private Long graphId;

    private JobStatus jobStatus;

    private String description;

    private String username;

    private Map<String, Object> args;

    private Map<String, Map<String,Object>> environment;

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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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

    public String getGraphName() {
        return graphName;
    }

    public void setGraphName(String graphName) {
        this.graphName = graphName;
    }

    public JobStatus getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(JobStatus jobStatus) {
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

    public String getUsername() { return username; }

    public void setUsername(String username) { this.username = username; }

    public Long getGraphId() {
        return graphId;
    }

    public void setGraphId(Long graphId) {
        this.graphId = graphId;
    }

    public Map<String, Object> getArgs() { return args; }

    public void setArgs(Map<String, Object> args) { this.args = args; }


}
