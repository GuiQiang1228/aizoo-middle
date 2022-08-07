package aizoo.common;

import java.util.Arrays;
import java.util.List;

public enum JobStatus {
    RUNNING("running","正在执行"),
    CANCELLED("cancelled","已经取消"),
    PENDING("pending","排队中"),
    FAILED("failed","已失败"),
    COMPLETING("completing","完成中"),
    COMPLETED("completed","已完成"),
    SUSPENDED("suspended","已暂停"),
    NODE_FAIL("node_fail","节点失效"),
    PREEMPTED("preempted","被抢占"),
    BOOT_FAIL("boot_fail","节点启动失败"),
    DEADLINE("deadline","达到截止时间终止"),
    OUT_OF_MEMORY("out_of_memory","内存溢出"),
    CONFIGURING("configuring","配置中"),
    RESIZING("resizing","调整大小中"),
    REVOKED("revoked","已撤销"),
    SPECIAL_EXIT("special_exit","特殊退出"),
    TIMEOUT("timeout","超时终止");

    private String name;
    private String value;

    JobStatus(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private static final JobStatus[] needUpdateJobStatus = {JobStatus.PENDING, JobStatus.RUNNING, JobStatus.COMPLETING,
            JobStatus.SUSPENDED, JobStatus.CONFIGURING, JobStatus.RESIZING};

    public static List<JobStatus> needUpdate(){
        return Arrays.asList(needUpdateJobStatus);
    }

    private static final JobStatus[] stopJobStatus = {JobStatus.CANCELLED, JobStatus.FAILED, JobStatus.COMPLETED,
            JobStatus.NODE_FAIL, JobStatus.PREEMPTED, JobStatus.BOOT_FAIL, JobStatus.DEADLINE, JobStatus.OUT_OF_MEMORY, JobStatus.REVOKED, JobStatus.SPECIAL_EXIT ,JobStatus.TIMEOUT};

    public static List<JobStatus> stopJobStatus(){
        return Arrays.asList(stopJobStatus);
    }
}
