package aizoo.viewObject.object.canLink;

import aizoo.common.Link;
import java.util.List;

public class ConnectVO {
    private ConnectNodeVO source;

    private ConnectNodeVO target;

    private List<Link> linklist;

    public ConnectNodeVO getSource() {
        return source;
    }

    public void setSource(ConnectNodeVO source) {
        this.source = source;
    }

    public ConnectNodeVO getTarget() {
        return target;
    }

    public void setTarget(ConnectNodeVO target) {
        this.target = target;
    }

    public List<Link> getLinklist() {
        return linklist;
    }

    public void setLinklist(List<Link> linklist) {
        this.linklist = linklist;
    }

    @Override
    public String toString(){
        return "ConnectVO{" +
                "source=" + source +
                ", target='" + target +
                ", linklist='" + linklist +
                '}';
    }
}
