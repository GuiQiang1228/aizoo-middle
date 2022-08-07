package aizoo.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import javax.persistence.*;

@Embeddable
public class Parameter {
    private String name;//变量名称（用户可以修改的），和datatype的name不是同一个
    private String title;
    @Lob
    private String description;
    private String originName; //初始名，不可更改，用户最初始定义的名字

    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private Datatype datatype;

    public Parameter() {
    }

    public Parameter(String name, String title, String description, String originName, Datatype datatype) {
        this.name = name;
        this.title = title;
        this.description = description;
        this.originName = originName;
        this.datatype = datatype;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Datatype getDatatype() {
        return datatype;
    }

    public void setDatatype(Datatype datatype) {
        this.datatype = datatype;
    }

    public String getOriginName() {
        return originName;
    }

    public void setOriginName(String originName) {
        this.originName = originName;
    }
}
