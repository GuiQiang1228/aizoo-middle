package aizoo.common.notifyEnum;

public enum NotifyType {
    ANNOUNCE("公告"),
    REMIND("提醒"),
    MESSAGE("私信"),
    SHARE("分享提醒");

    private String value;

    NotifyType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
