package aizoo.common.notifyEnum;

public enum  MessageStatus {
    SUCCESS("成功"),
    FAILED("失败");

    private String value;

    MessageStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
