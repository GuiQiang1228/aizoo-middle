package aizoo.common;

public enum LevelChangeType {
    SUPER_CHANGE("管理员主动修改"),
    PAY_CHANGE("通过支付升级"),
    USER_APPLY("用户申请");

    private String value;

    LevelChangeType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
