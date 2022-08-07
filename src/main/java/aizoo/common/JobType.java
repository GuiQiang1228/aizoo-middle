package aizoo.common;

public enum JobType {
    EXPERIMENT_JOB("experimentJob"),
    SERVICE_JOB("serviceJob"),
    APPLICATION("application"),
    MIRROR_JOB("mirrorJob");

    JobType(String value) {
        this.value = value;
    }

    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
