package aizoo.common;

public enum GraphType {
    COMPONENT("component", "组件"),
    JOB("job", "实验"),
    SERVICE("service","服务"),
    APPLICATION("application","应用");

    private String name;
    private String value;

    GraphType(String name, String value) {
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

    public static boolean isContains(String type) {
        for (GraphType t : GraphType.values()) {
            if (t.getName().equals(type))
                return true;
        }
        return false;
    }
}
