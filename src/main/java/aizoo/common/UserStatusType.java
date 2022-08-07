package aizoo.common;

public enum UserStatusType {
    NORMAL_STATUS("normalStatus"),
    FROZEN_STATUS("frozenStatus");

    private String value;

    UserStatusType(String value){this.value=value;}

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
