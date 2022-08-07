package aizoo.viewObject.object;

import aizoo.common.LevelType;
import aizoo.common.UserRoleType;
import aizoo.domain.Level;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;
import java.util.List;

public class UserVO extends BaseVO {
    private String username;

    private List<UserRoleType> roles;// 需要知道它是什么角色,enum类型的需要在原来的类加 @jsonvalue注解

    private LevelType level;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date levelFinishTime;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<UserRoleType> getRoles() {
        return roles;
    }

    public void setRoles(List<UserRoleType> roles) {
        this.roles = roles;
    }

    public LevelType getLevel() {
        return level;
    }

    public void setLevel(LevelType level) {
        this.level = level;
    }

    public Date getLevelFinishTime() {
        return levelFinishTime;
    }

    public void setLevelFinishTime(Date levelFinishTime) {
        this.levelFinishTime = levelFinishTime;
    }

     @Override
    public String toString() {
        return "UserVO{" +
                "username='" + username + '\'' +
                ", roles=" + roles +
                ", id=" + id +
                '}';
    }
}
