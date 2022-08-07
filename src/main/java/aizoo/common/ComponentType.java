package aizoo.common;


import java.util.Arrays;

public enum ComponentType {
    LOSS("loss", "损失"),
    DATALOADER("dataLoader", "数据加载器"),
    MODULE("module", "组件"),
    MODEL("model", "模型"),
    DATASOURCE("datasource", "数据集"),
    PROCESS("process", "过程"),
    FUNCTION("function", "函数"),
    PARAMETER("parameter", "服务入参"),
    METRIC("metric", "评测指标"),
    OPERATOR("operator", "运算符"),
    OPERATOR_TEMPLATE("operator_template", "运算符（模板）");



    private String value;
    private String name;

    ComponentType(String value, String name) {
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

    private static final ComponentType[] COMPONENT_TYPE = {ComponentType.LOSS, ComponentType.DATALOADER, ComponentType.PROCESS,
            ComponentType.FUNCTION, ComponentType.METRIC, ComponentType.MODULE, ComponentType.MODEL, ComponentType.PARAMETER};

    // 判断elastic搜索页是否需要该类型的图
    public static boolean canUsedForElasticSearch(ComponentType type) {
        return Arrays.asList(COMPONENT_TYPE).contains(type);
    }

    public static boolean isContains(String type) {
        for (ComponentType t : ComponentType.values()) {
            if (t.getValue().equals(type))
                return true;
        }
        return false;
    }
}
