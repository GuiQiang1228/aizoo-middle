package aizoo.common.notifyEnum;

public enum SubscriptionType {
    USER("用户"),
    EXPERIMENT_REPORT("实验报告"),
    COURSE_WORK("课程作业");

    private String value;

    SubscriptionType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
