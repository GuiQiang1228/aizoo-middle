package aizoo.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

@Entity
public class ForkEdition extends  BaseDomain{

    @JsonBackReference(value = "user")
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private User user;

    @JsonBackReference(value = "edition")
    private long edition;

    public ForkEdition(long edition){
        this.edition = edition;
    }

    public ForkEdition(long edition, User user){
        this.edition = edition;
        this.user = user;
    }

    public ForkEdition(){

    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }


    public long getEdition() {
        return edition;
    }

    public void setEdition(long edition) {
        this.edition = edition;
    }
}
