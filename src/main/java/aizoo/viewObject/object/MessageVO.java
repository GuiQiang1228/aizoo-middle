package aizoo.viewObject.object;

import aizoo.common.notifyEnum.MessageStatus;
import aizoo.common.notifyEnum.MessageType;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import java.util.Map;

public class MessageVO extends BaseVO{

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date createTime;

    private String fromUser;

    private String toUser;

    private MessageType messageType;

    private MessageStatus messageStatus;

    private Map<String,Object> messageBody;

    private boolean isSender;

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getFromUser() {
        return fromUser;
    }

    public void setFromUser(String fromUser) {
        this.fromUser = fromUser;
    }

    public String getToUser() {
        return toUser;
    }

    public void setToUser(String toUser) {
        this.toUser = toUser;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public Map<String, Object> getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(Map<String, Object> messageBody) {
        this.messageBody = messageBody;
    }

    public boolean isSender() {
        return isSender;
    }

    public void setIsSender(boolean isSender) {
        this.isSender = isSender;
    }

    public MessageStatus getMessageStatus() {
        return messageStatus;
    }

    public void setMessageStatus(MessageStatus messageStatus) {
        this.messageStatus = messageStatus;
    }
}
