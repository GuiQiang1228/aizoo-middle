package aizoo.common;

public enum ElasticSearchType {
    COMPONENT("component", "组件"),
    APPLICATION("application", "应用图"),
    SERVICE("service", "服务图"),
    JOB("job", "实验图"),
    PROJECT("project", "项目");



    private String value;
    private String name;

    ElasticSearchType(String value, String name) {
        this.value = value;
        this.name = name;
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
}
