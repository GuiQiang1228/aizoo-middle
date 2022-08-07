package aizoo.utils;

import aizoo.domain.Namespace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NamespaceUtil {
    /**
     * 根据Namespace列表,获取到其中所有namespace和privacy的映射关系
     *
     * @param namespaceList   Namespace的列表
     * @return List<Map<String,String>>类型，Map格式为：{"namespace":"","privacy":"namespace对应的privacy"}
     */
    public static List<Map<String,String>> formatNamespaceList(List<Namespace> namespaceList){

        List<Map<String,String>> list = new ArrayList<>();

        // 获取列表中每个Namespace的namespace和privacy，将生成的map加入list
        for(Namespace namespace:namespaceList){
            Map<String,String> map = new HashMap<>();
            map.put("namespace", namespace.getNamespace());
            map.put("privacy", namespace.getPrivacy());
            list.add(map);
        }

        return list;
    }
}
