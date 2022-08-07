package aizoo.common;

import java.util.Arrays;

public enum NodeType {
    LOSS("loss", "损失"),
    DATALOADER("dataLoader", "数据加载器"),
    MODULE("module", "模块"),
    MODEL("model", "模型"),
    DATASOURCE("datasource", "数据集"),
    PROCESS("process", "过程"),
    FUNCTION("function", "函数"),
    METRIC("metric", "评测指标"),
    SERVICE("service", "服务"),
    CHECKPOINT("checkPoint", "模型断点"),
    PARAMETER("parameter", "服务入参"),
    VISUALCONTAINER("visualContainer", "可视化容器"),
    OPERATOR("operator", "运算符"),
    OPERATOR_TEMPLATE("operator_template", "运算符（模板）");

    private String value;
    private String name;
    NodeType(String value, String name) {
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

    private static final NodeType[] USED_FOR_MODULE_OR_MODEL_GRAPH = {NodeType.PROCESS, NodeType.FUNCTION, NodeType.MODULE, NodeType.OPERATOR_TEMPLATE};
    private static final NodeType[] USED_FOR_LOSS_GRAPH = {NodeType.PROCESS, NodeType.FUNCTION, NodeType.MODULE, NodeType.LOSS, NodeType.OPERATOR_TEMPLATE};

    private static final NodeType[] USED_FOR_SERVICE_GRAPH = {NodeType.FUNCTION, NodeType.MODEL, NodeType.PROCESS, NodeType.PARAMETER, NodeType.OPERATOR_TEMPLATE};
    private static final NodeType[] USED_FOR_APP_GRAPH = {NodeType.DATALOADER, NodeType.DATASOURCE, NodeType.FUNCTION, NodeType.SERVICE, NodeType.PROCESS, NodeType.OPERATOR_TEMPLATE};
    private static final NodeType[] USED_FOR_EXPERIMENT_GRAPH = {NodeType.DATALOADER, NodeType.DATASOURCE, NodeType.METRIC, NodeType.MODEL, NodeType.LOSS, NodeType.PROCESS};

    private static final NodeType[] COMPONENT_TYPE = {NodeType.LOSS, NodeType.DATASOURCE, NodeType.DATALOADER, NodeType.PROCESS,
            NodeType.FUNCTION, NodeType.METRIC, NodeType.MODULE, NodeType.MODEL, NodeType.PARAMETER, NodeType.OPERATOR,NodeType.OPERATOR_TEMPLATE};

    // 判断画service图的时候是否用到这个类型的组件
    public static boolean canUsedForServiceGraph(NodeType type) {
        return Arrays.asList(USED_FOR_SERVICE_GRAPH).contains(type);
    }

    // 判断画application图的时候是否用到这个类型的组件
    public static boolean canUsedForAppGraph(NodeType type) {
        return Arrays.asList(USED_FOR_APP_GRAPH).contains(type);
    }

    // 判断画component图的时候是否用到这个类型的组件
    // 判断该node类型是否属于componentType
    public static boolean canUsedForModuleOrModelGraph(NodeType type) {
        return Arrays.asList(USED_FOR_MODULE_OR_MODEL_GRAPH).contains(type);
    }

    public static boolean canUsedForLossGraph(NodeType type) {
        return Arrays.asList(USED_FOR_LOSS_GRAPH).contains(type);
    }

    public static boolean canUsedForExperimentGraph(NodeType type) {
        return Arrays.asList(USED_FOR_EXPERIMENT_GRAPH).contains(type);
    }

    // 判断该node类型是否属于componentType
    public static boolean isComponentType(NodeType type) {
        return Arrays.asList(COMPONENT_TYPE).contains(type);
    }

}
