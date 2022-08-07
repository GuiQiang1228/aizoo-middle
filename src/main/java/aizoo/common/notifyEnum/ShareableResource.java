package aizoo.common.notifyEnum;

public enum ShareableResource {
    DATASOURCE("数据资源", "DATASOURCE"),
    COMPONENT("算子", "COMPONENT"),
    COMPONENT_GRAPH("复合组件图", "COMPONENT"),
    EXPERIMENT_GRAPH("实验图", "JOB"),
    SERVICE_GRAPH("服务图", "SERVICE"),
    APPLICATION_GRAPH("应用图", "APPLICATION");

    private String value;

    private String type;

    ShareableResource(String value, String type) {
        this.value = value;
        this.type = type;
    }

    ShareableResource(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
