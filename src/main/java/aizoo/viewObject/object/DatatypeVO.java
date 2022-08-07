package aizoo.viewObject.object;

public class DatatypeVO {
    private String name;

    public DatatypeVO() {
    }

    public DatatypeVO(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "DatatypeVO{" +
                "name='" + name + '\'' +
                '}';
    }
}
