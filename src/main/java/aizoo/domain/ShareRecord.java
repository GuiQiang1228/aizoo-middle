package aizoo.domain;

import aizoo.common.notifyEnum.ShareableResource;

import javax.persistence.*;
import java.util.Date;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

@Entity
public class ShareRecord extends BaseDomain implements Delayed {

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private User sender;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private User recipient;

    @Enumerated(EnumType.STRING)
    private ShareableResource resourceType;

    private Long resourceId;  // 用于分享时记录资源的id

    private boolean accepted;

    private boolean isRead;

    private Long graphId;

    private Long componentId;

    private Date expireDate;

    public ShareRecord() {
    }

    public ShareRecord(User sender, User recipient, ShareableResource resourceType, Long resourceId, boolean accepted, boolean isRead) {
        this.sender = sender;
        this.recipient = recipient;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.accepted = accepted;
        this.isRead = isRead;
    }

    /**
     *  判断过期策略，当创建时间+有效期 >= 当前时间，则说明过期
     */
    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(this.getCreateTime().getTime() + this.expireDate.getTime() - System.currentTimeMillis(), TimeUnit.NANOSECONDS);
    }

    /**
     *  订单加入队列的排序规则
     */
    @Override
    public int compareTo(Delayed o) {
        ShareRecord record1 = (ShareRecord) o;
        long time1 = record1.getCreateTime().getTime() + record1.expireDate.getTime();
        long time = this.getCreateTime().getTime() + this.expireDate.getTime();
        return time == time1 ? 0 : time1 < time ? 1 : -1;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public User getRecipient() {
        return recipient;
    }

    public void setRecipient(User recipient) {
        this.recipient = recipient;
    }

    public ShareableResource getResourceType() {
        return resourceType;
    }

    public void setResourceType(ShareableResource resourceType) {
        this.resourceType = resourceType;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    public boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(boolean isRead) {
        this.isRead = isRead;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public Date getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(Date expireDate) {
        this.expireDate = expireDate;
    }

    public Long getGraphId() {
        return graphId;
    }

    public void setGraphId(Long graphId) {
        this.graphId = graphId;
    }

    public Long getComponentId() {
        return componentId;
    }

    public void setComponentId(Long componentId) {
        this.componentId = componentId;
    }
}
