package aizoo.common.notifyEnum;

public enum ActionType {
    LIKE("喜欢"),
    COMMENT("评论"),
    SUBSCRIBE("订阅"),
    FORK("拷贝"),
    SHARE("分享");

    private String value;

    ActionType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
