package aizoo.grpcObject.object;

import aizoo.common.JobStatus;

import java.util.Map;

public class JobObject {
    private String name;

    private String jobKey;

    private JobStatus jobStatus;

    private String description;

    private Map<String, Map<String,Object>> environment;

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

    @Override
    public String toString() {
        return "JobObject{" +
                "name='" + name + '\'' +
                ", jobKey='" + jobKey + '\'' +
                ", jobStatus=" + jobStatus +
                ", description='" + description + '\'' +
                ", environment=" + environment +
                '}';
    }
}
