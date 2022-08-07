package aizoo.common;

public enum LevelType {
    LEVEL1("等级1"),
    LEVEL2("等级2"),
    LEVEL3("等级3"),
    LEVEL4("等级4"),
    LEVEL5("等级5");

    private String value;

    LevelType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
