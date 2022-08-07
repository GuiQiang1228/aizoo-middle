package aizoo.common;

public enum ParameterIOType {
    INPUT("input", "输入"),
    OUTPUT("output", "输出");
    private String name;
    private String value;

    ParameterIOType(String name, String value) {
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
        for (ParameterIOType t : ParameterIOType.values()) {
            if (t.getName().equals(type))
                return true;
        }
        return false;
    }
}
