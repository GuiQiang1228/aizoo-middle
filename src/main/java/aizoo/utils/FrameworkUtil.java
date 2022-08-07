package aizoo.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FrameworkUtil {

    private static final Map<String, ArrayList<String>> FRAMEWORK_LIST = new HashMap<>();

    /**
     * 该方法用于获取系统支持的framework以及version
     * @return 返回的map中，key是使用的软件（pytorch)或语言(python3)，value是版本号构成的list
     */
    public static Map<String, ArrayList<String>> getVersionList() {
        ArrayList<String> pytorchVersionList = new ArrayList<>();
        ArrayList<String> python3VersionList = new ArrayList<>();
        pytorchVersionList.add("1.9.0");
        pytorchVersionList.add("latest");
        //pytorch的版本号数组中添加1.9.0版本和latest版本
        python3VersionList.add("latest");
        //python3的版本号数组中添加latest版本
        FRAMEWORK_LIST.put("pytorch",pytorchVersionList);
        FRAMEWORK_LIST.put("python3",python3VersionList);
        return FRAMEWORK_LIST;
    }
}
