package aizoo.domain;

import aizoo.common.ResourceType;

import javax.persistence.*;

@Entity
public class ResourceUsage extends BaseDomain{

    @Enumerated(EnumType.STRING)
    private ResourceType resourceType;

    private double usedAmount;

    private boolean released;

    private Long jobId;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private User user;

    public ResourceUsage() {

    }

    public ResourceUsage(ResourceType resourceType, User user) {
        this.resourceType = resourceType;
        this.user = user;
    }

    public ResourceUsage(ResourceType resourceType, double usedAmount, Long jobId, User user) {
        this.resourceType = resourceType;
        this.usedAmount = usedAmount;
        this.jobId = jobId;
        this.user = user;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public double getUsedAmount() {
        return usedAmount;
    }

    public void setUsedAmount(double usedAmount) {
        this.usedAmount = usedAmount;
    }

    public boolean isReleased() {
        return released;
    }

    public void setReleased(boolean released) {
        this.released = released;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
