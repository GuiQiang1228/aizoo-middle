package aizoo.domain;

import aizoo.common.LevelChangeType;
import aizoo.common.UserStatusType;
import com.fasterxml.jackson.annotation.JsonFormat;

import javax.persistence.*;
import java.util.Date;

@Entity
public class UserLevelChangeLog extends BaseDomain {
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private User user;

    private String previousLevel;

    private String appliedLevel;

    private int appliedDuration;//用户申请某等级的月数

    private boolean changed;

    @Enumerated(EnumType.STRING)
    private LevelChangeType changeType;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date appliedTime; //用户申请的事件

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date changeTime; //用户等级变更的时间

    private boolean levelChange;//用户是请求升级还是请求续期

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getPreviousLevel() {
        return previousLevel;
    }

    public void setPreviousLevel(String previousLevel) {
        this.previousLevel = previousLevel;
    }

    public String getAppliedLevel() {
        return appliedLevel;
    }

    public void setAppliedLevel(String appliedLevel) {
        this.appliedLevel = appliedLevel;
    }

    public int getAppliedDuration() {
        return appliedDuration;
    }

    public void setAppliedDuration(int appliedDuration) {
        this.appliedDuration = appliedDuration;
    }

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public Date getAppliedTime() {
        return appliedTime;
    }

    public void setAppliedTime(Date appliedTime) {
        this.appliedTime = appliedTime;
    }

    public Date getChangeTime() {
        return changeTime;
    }

    public void setChangeTime(Date changeTime) {
        this.changeTime = changeTime;
    }

    public LevelChangeType getChangeType() {
        return changeType;
    }

    public void setChangeType(LevelChangeType changeType) {
        this.changeType = changeType;
    }

    public boolean isLevelChange() {
        return levelChange;
    }

    public void setLevelChange(boolean levelChange) {
        this.levelChange = levelChange;
    }
}
