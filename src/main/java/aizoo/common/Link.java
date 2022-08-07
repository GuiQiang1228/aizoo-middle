package aizoo.common;

public class Link {
    private String source;

    private String target;

    private String id;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "LinkVO{" +
                "source='" + source + '\'' +
                ", target='" + target + '\'' +
                ", id=" + id + '\'' +
                '}';
    }
}
