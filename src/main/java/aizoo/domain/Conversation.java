package aizoo.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.security.core.annotation.CurrentSecurityContext;

import javax.persistence.*;
import java.util.List;

@Entity
public class Conversation extends BaseDomain{
    // owner是会话的拥有者，participant 是会话的参与者
    // 每两个用户之间的会话，两个用户都将维护该会话的一条conversation记录
    // 每条 conversation 记录的 owner 是自身，participant 是另一个用户
    // 会话的 updateTime 代表会话中最新一条消息的创建时间


    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private User owner;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private User participant;

    @JsonBackReference
    @ManyToMany(mappedBy = "conversations", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private List<Message> messageList;

    private Long latestMessage;  // 用于保存会话中最新的一条消息的id

    @Value("${some.key:0}")
    private Integer unreadCount;

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public User getParticipant() {
        return participant;
    }

    public void setParticipant(User participant) {
        this.participant = participant;
    }

    public List<Message> getMessageList() {
        return messageList;
    }

    public void setMessageList(List<Message> messageList) {
        this.messageList = messageList;
    }

    public Long getLatestMessage() {
        return latestMessage;
    }

    public void setLatestMessage(Long latestMessage) {
        this.latestMessage = latestMessage;
    }

    public Integer getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(Integer unreadCount) {
        this.unreadCount = unreadCount;
    }
}
