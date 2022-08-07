package aizoo.viewObject.object;

import java.util.List;

public class ExperimentJobCheckpointVO extends BaseVO{

    private String name;
    private List<CheckPointVO> checkPoints;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<CheckPointVO> getCheckPoints() {
        return checkPoints;
    }

    public void setCheckPoints(List<CheckPointVO> checkPoints) {
        this.checkPoints = checkPoints;
    }

}
