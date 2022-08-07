package aizoo.utils;

import aizoo.common.*;
import aizoo.domain.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.EnumUtils;

import java.io.File;
import java.util.*;

public class ComponentUtil {
    private static final Map<String, Map<String, String>> MODULE_MODEL_CATALOGUE = new HashMap<>();
    private static final Map<String, Map<String, String>> LOSS_CATALOGUE = new HashMap<>();
    private static final Map<String, Map<String, String>> EXPERIMENT_CATALOGUE = new HashMap<>();
    private static final Map<String, Map<String, String>> SERVICE_CATALOGUE = new HashMap<>();
    private static final Map<String, Map<String, String>> APPLICATION_CATALOGUE = new HashMap<>();

    static {
        buildModuleAndModelCatalogue();
        buildLossCatalogue();
        buildExperimentCatalogue();
        buildServiceCatalogue();
        buildApplicationCatalogue();
    }


    /**
     * 建立模块和模块目录，catalogue<value,name>
     */
    public static void buildModuleAndModelCatalogue() {
        for (NodeType nodeType : NodeType.values()) {
            // 判断该node类型是否属于componentType
            if(NodeType.canUsedForModuleOrModelGraph(nodeType)){
                Map<String, String> catalogue = new HashMap<>();
                catalogue.put("type", nodeType.toString());
                catalogue.put("name", nodeType.getName());
                MODULE_MODEL_CATALOGUE.put(nodeType.toString(), catalogue);
            }
        }
    }

    /**
     * 建立损失目录，catalogue<value,name>
     */
    public static void buildLossCatalogue() {
        for (NodeType nodeType : NodeType.values()) {
            // 判断画loss图的时候是否用到这个类型的组件
            if(NodeType.canUsedForLossGraph(nodeType)){
                Map<String, String> catalogue = new HashMap<>();
                catalogue.put("type", nodeType.toString());
                catalogue.put("name", nodeType.getName());
                LOSS_CATALOGUE.put(nodeType.toString(), catalogue);
            }
        }
    }

    /**
     * 建立Experiment目录
     */
    public static void buildExperimentCatalogue() {
        for (NodeType nodeType : NodeType.values()) {
            // 判断画Experiment图的时候是否用到这个类型的组件
            if(NodeType.canUsedForExperimentGraph(nodeType)){
                Map<String, String> catalogue = new HashMap<>();
                catalogue.put("type", nodeType.toString());
                catalogue.put("name", nodeType.getName());
                EXPERIMENT_CATALOGUE.put(nodeType.toString(), catalogue);
            }
        }
    }

    /**
     * 建立Service目录
     */
    public static void buildServiceCatalogue() {
        for (NodeType nodeType : NodeType.values()) {
            // 判断画Service图的时候是否用到这个类型的组件
            if(NodeType.canUsedForServiceGraph(nodeType)){
                Map<String, String> catalogue = new HashMap<>();
                catalogue.put("type", nodeType.toString());
                catalogue.put("name", nodeType.getName());
                SERVICE_CATALOGUE.put(nodeType.toString(), catalogue);
            }
        }
    }

    /**
     * 建立application目录
     */
    public static void buildApplicationCatalogue() {
        for (NodeType nodeType : NodeType.values()) {
            // 判断画application图的时候是否用到这个类型的组件
            if(NodeType.canUsedForAppGraph(nodeType)){
                Map<String, String> catalogue = new HashMap<>();
                catalogue.put("type", nodeType.toString());
                catalogue.put("name", nodeType.getName());
                APPLICATION_CATALOGUE.put(nodeType.toString(), catalogue);
            }
        }
    }

    /**
     * 删除Component文件夹
     * @param component Component实体
     * @throws Exception
     */
    public static void removeComponentFiles(Component component) throws Exception {
        String path = component.getPath();
        FileUtil.deleteFile(new File(path));
    }

    /**
     * 将字符串首字母转为小写
     * @param s
     * @return 转化后的字符串
     */
    public static String toLowerCaseFirstOne(String s) {
        if (Character.isLowerCase(s.charAt(0))) {
            return s;
        } else {
            return Character.toLowerCase(s.charAt(0)) + s.substring(1);
        }
    }

    /**
     * 获取目录
     * @param type 类型
     * @return 相应类型的目录
     */
    public static Map<String, Map<String, String>> getCatalogue(String type) {
        //判断图类型和组件类型是否有效
        if(!EnumUtils.isValidEnum(GraphType.class, type)){
            if(EnumUtils.isValidEnum(ComponentType.class, type)){
                ComponentType componentType = ComponentType.valueOf(type);
                //根据componentType判断返回模块目录还是损失目录
                if(componentType == ComponentType.MODULE || componentType == ComponentType.MODEL)
                    return MODULE_MODEL_CATALOGUE;
                else if(componentType == ComponentType.LOSS)
                    return LOSS_CATALOGUE;
            }
        }
        //否则依次判断，返回相应类型的目录
        else {
            GraphType graphType = GraphType.valueOf(type);
            if(GraphType.JOB == graphType)
                return EXPERIMENT_CATALOGUE;
            else if(GraphType.SERVICE == graphType)
                return SERVICE_CATALOGUE;
            else if(GraphType.APPLICATION == graphType)
                return APPLICATION_CATALOGUE;
        }
        return null;
    }
}
