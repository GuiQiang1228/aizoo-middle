package aizoo.utils;

import aizoo.common.ComponentType;
import aizoo.common.ElasticSearchType;
import aizoo.common.GraphType;
import aizoo.common.NodeType;
import aizoo.domain.Component;
import org.apache.commons.lang3.EnumUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ElasticUtil {

    private static final Map<String, Map<String, String>> COMPONENT_CATALOGUE = new HashMap<>();

    static{
        buildComponentCatalogue();
    }

    /**
     * 建立 COMPONENT目录，相应的变量名为COMPONENT_CATALOGUE
     * COMPONENT_CATALOGUE数据类型：Map<String, Map<String, String>>
     * 内容含义如下：{
     *      Component类别:{ type:Component类别, name:Component名称 }
     * }
     */
    public static void buildComponentCatalogue() {
        // 对枚举类ComponentType中的元素进行遍历
        for (ComponentType componentType : ComponentType.values()) {
            // 判断elastic搜索页是否需要该componentType
            if (ComponentType.canUsedForElasticSearch(componentType)) {
                Map<String, String> catalogue = new HashMap<>();
                catalogue.put("type", componentType.toString());
                catalogue.put("name", componentType.getName());
                COMPONENT_CATALOGUE.put(componentType.toString(), catalogue);
            }
        }
    }

    /**
     * 功能：根据参数type判断是否返回COMPONENT目录
     * @param type
     *
     * 若type与枚举类ElasticSearchType中的COMPONENT相对应
     * @return COMPONENT_CATALOGUE，即返回COMPONENT目录
     * COMPONENT_CATALOGUE数据类型：Map<String, Map<String, String>>
     * COMPONENT_CATALOGUE中的内容：{
     *                  Component类别:{ type:Component类别, name:Component名称 }
     *              }
     * 否则
     * @return null
     */
    public static Map<String, Map<String, String>> getCatalogue(String type) {
        // 验证type是否在枚举类ElasticSearchType中
        if (EnumUtils.isValidEnum(ElasticSearchType.class, type)) {
            ElasticSearchType elasticSearchType = ElasticSearchType.valueOf(type);
            // 判断type是否对应枚举类中的COMPONENT
            if (elasticSearchType == ElasticSearchType.COMPONENT)
                return COMPONENT_CATALOGUE; //若是，返回COMPONENT目录
        }
        return null;
    }
}
