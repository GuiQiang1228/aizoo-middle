package aizoo.domain;

import aizoo.common.JobStatus;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;


@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class Job extends BaseDomain{
// 继承Job实体类，表示实体时可以提交到slurm上运行的任务

    @Lob
    private String environment;

    @Enumerated(EnumType.STRING)//枚举字符串
    private JobStatus jobStatus;

    @Lob
    private String args;

    private String executePath;

    private String rootPath;

    private String scriptPath;

    private String jobKey;

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public JobStatus getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(JobStatus jobStatus) {
        this.jobStatus = jobStatus;
    }

    public String getJobKey() {
        return jobKey;
    }

    public void setJobKey(String jobKey) {
        this.jobKey = jobKey;
    }

    public String getArgs() { return args; }

    public void setArgs(String args) { this.args = args; }

    public String getExecutePath() { return executePath; }

    public void setExecutePath(String executePath) { this.executePath = executePath; }

    public String getRootPath() { return rootPath; }

    public void setRootPath(String rootPath) { this.rootPath = rootPath; }

    public String getScriptPath() { return scriptPath; }

    public void setScriptPath(String scriptPath) { this.scriptPath = scriptPath; }
}
