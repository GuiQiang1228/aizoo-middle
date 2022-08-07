package aizoo.domain;

import aizoo.common.notifyEnum.ActionType;
import aizoo.common.notifyEnum.NotifyType;
import aizoo.common.notifyEnum.SubscriptionType;

import javax.persistence.*;

@Entity
public class Notify extends BaseDomain{  // 公共的消息列表(用户执行动作信息、管理员发送公告信息)
    @Lob
    private String content;

    private String title;

    @Enumerated(EnumType.STRING)
    private NotifyType type;

    private Long target; //目标id

    @Enumerated(EnumType.STRING)
    private SubscriptionType targetType;

    @Enumerated(EnumType.STRING)
    private ActionType action;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private User sender;

    public Notify(String content, String title, NotifyType type, User sender) {
        this.content = content;
        this.title = title;
        this.type = type;
        this.sender = sender;
    }

    public Notify() {

    }

    public Notify(User sender, NotifyType type) {
        this.type = type;
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public NotifyType getType() {
        return type;
    }

    public void setType(NotifyType type) {
        this.type = type;
    }

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

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }
}
