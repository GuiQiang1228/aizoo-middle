package aizoo.common;

public enum ResourceType {
    GPU("GPU节点数量"),
    CPU("CPU数量"),
    MEMORY("内存大小"),
    DISK("硬盘容量"),
    EXPERIMENT_TOTAL_NUMBER("实验总数"),
    EXPERIMENT_RUNNING_NUMBER("正在运行实验总数"),
    SERVICE_TOTAL_NUMBER("服务总数"),
    SERVICE_RUNNING_NUMBER("正在运行服务总数"),
    APPLICATION_TOTAL_NUMBER("应用总数"),
    APPLICATION_RUNNING_NUMBER("正在运行应用总数"),
    MIRROR_JOB_TOTAL_NUMBER("镜像实验总数"),
    MIRROR_JOB_RUNNING_NUMBER("正在运行镜像实验总数");

    private String value;

    ResourceType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
