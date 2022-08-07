package aizoo.common;

import com.fasterxml.jackson.annotation.JsonValue;

public enum UserRoleType {
    USER("ROLE_USER"),ADMIN("ROLE_ADMIN");
    String value;

    UserRoleType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "UserRoleType{" +
                "value='" + value + '\'' +
                '}';
    }
}
