package aizoo.viewObject.object;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NamespaceVO extends BaseVO {
    private String privacy;

    private String namespace;

    private String username;

    @JsonProperty(value = "user")
    private UserVO userVO;

    public String getPrivacy() {
        return privacy;
    }

    public void setPrivacy(String privacy) {
        this.privacy = privacy;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public UserVO getUserVO() {
        return userVO;
    }

    public void setUserVO(UserVO userVO) {
        this.userVO = userVO;
    }

    public String getUsername() { return username; }

    public void setUsername(String username) { this.username = username; }

    @Override
    public String toString() {
        return "NamespaceVO{" +
                "privacy='" + privacy + '\'' +
                ", namespace='" + namespace + '\'' +
                ", userVO=" + userVO +
                ", id=" + id +
                '}';
    }
}
