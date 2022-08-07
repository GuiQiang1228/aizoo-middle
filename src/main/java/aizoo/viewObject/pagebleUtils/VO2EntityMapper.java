package aizoo.viewObject.pagebleUtils;

import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class VO2EntityMapper {
    public static <T1, T2> Page<T1> mapEntityPage2VOPage(Function<T2, T1> mapperFunction, Page<T2> page) {
        return page.map(mapperFunction);
    }

    public static <T1, T2> List<T1> mapEntityList2VOList(Function<T2, T1> mapperFunction, List<T2> entityList) {
        List<T1> list = new ArrayList<>();
        entityList.forEach(entity -> {
            list.add(mapperFunction.apply(entity));
        });
        return list;
    }

}
