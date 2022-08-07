package aizoo.domain;

import aizoo.common.notifyEnum.ActionType;
import aizoo.common.notifyEnum.SubscriptionType;
import com.fasterxml.jackson.annotation.JsonBackReference;

import javax.persistence.CascadeType;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;

public class Subscription extends BaseDomain{

    private Long target; //目标id

    @Enumerated(EnumType.STRING)
    private SubscriptionType targetType;

    @Enumerated(EnumType.STRING)
    private ActionType action;

    @JsonBackReference(value = "user")
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private User user;  // 订阅的所属者（接收者）

    public Long getTarget() {
        return target;
    }

    public void setTarget(Long target) {
        this.target = target;
    }

    public SubscriptionType getTargetType() {
        return targetType;
    }

    public void setTargetType(SubscriptionType targetType) {
        this.targetType = targetType;
    }

    public ActionType getAction() {
        return action;
    }

    public void setAction(ActionType action) {
        this.action = action;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
