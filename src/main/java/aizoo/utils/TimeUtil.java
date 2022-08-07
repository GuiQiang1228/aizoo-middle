package aizoo.utils;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeUtil {
    /**
     * 将时间戳转换为时间
     *
     * @param date 时间戳
     * @return res 时间 Date格式format为string
     */
    public static String stampToDate(Date date){
        String res;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        res = simpleDateFormat.format(date);
        return res;
    }
}