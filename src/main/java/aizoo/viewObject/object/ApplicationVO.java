package aizoo.viewObject.object;


import java.util.List;
import java.util.Map;

public class ApplicationVO extends BaseVO{
    private String name;

    private String title;

    private String description;

    private String username;

    private String graphName;

    private Long graphId;

    private Map<String, Object> args;

    private String path;

    private String jobStatus;

    private String jobKey;

    private String outLogUrl;

    private String errorLogUrl;

    private Map<String, Map<String,Object>> environment;

    private Map<String,List<ApplicationResultVO>> appResultVOMap;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGraphName() {
        return graphName;
    }

    public void setGraphName(String graphName) {
        this.graphName = graphName;
    }

    public Map<String, List<ApplicationResultVO>> getAppResultVOMap() {
        return appResultVOMap;
    }

    public void setAppResultVOMap(Map<String, List<ApplicationResultVO>> appResultVOMap) {
        this.appResultVOMap = appResultVOMap;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(String jobStatus) {
        this.jobStatus = jobStatus;
    }

    public String getJobKey() {
        return jobKey;
    }

    public void setJobKey(String jobKey) {
        this.jobKey = jobKey;
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

    public String getOutLogUrl() {
        return outLogUrl;
    }

    public void setOutLogUrl(String outLogUrl) {
        this.outLogUrl = outLogUrl;
    }

    public String getErrorLogUrl() {
        return errorLogUrl;
    }

    public void setErrorLogUrl(String errorLogUrl) {
        this.errorLogUrl = errorLogUrl;
    }

    public Map<String, Object> getArgs() { return args; }

    public void setArgs(Map<String, Object> args) { this.args = args; }


}
