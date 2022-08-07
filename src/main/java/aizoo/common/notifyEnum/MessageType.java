package aizoo.common.notifyEnum;

public enum MessageType {
    TEXT("文本"),
    SHARE("分享"),
    GRAPH("图片");

    private String value;

    MessageType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
