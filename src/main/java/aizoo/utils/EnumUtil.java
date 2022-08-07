package aizoo.utils;


public class EnumUtil {
    /**
     * 判断参数s是否属于枚举类enumType
     * @param enumType
     * @param s
     * @return 若参数属于枚举类返回true，否则返回false
     */
    public boolean isInclude(Class enumType, String s){
        boolean result = false;
        // 遍历枚举类enumType的元素
        for (Object obj: enumType.getEnumConstants()){
            // 判断s是否与枚举类中的元素相等
            if(s.equals(obj.toString())) {
                result = true; //若是，返回结果为true
                break;
            }
        }
        return result;
    }
}
