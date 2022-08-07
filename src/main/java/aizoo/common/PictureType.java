package aizoo.common;

/**
 * FileName:PictureType
 * Description:保存和下载截图的所有类型
 */
public enum PictureType {
    JOB("job", "实验"),
    SERVICE("service","服务"),
    APPLICATION("application","应用"),
    LOSS("loss","损失"),
    MODULE("module","组件"),
    MODEL("model","模型");

    private String value;
    private String name;

    PictureType(String value, String name) {
        this.value = value;
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static boolean isContains(String type){
        for(PictureType t : PictureType.values()){
            if(t.getValue().equals(type))
                return true;
        }
        return false;
    }
}
