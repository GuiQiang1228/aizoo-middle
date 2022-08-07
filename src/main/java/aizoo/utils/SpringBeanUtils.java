package aizoo.utils;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * 获取bean对象 工具类
 * @author LIFULIN
 * @date 2020-04-17
 */
@Component
public class SpringBeanUtils implements ApplicationContextAware {
    /**
     * Spring应用上下文环境
     */
    public static ApplicationContext applicationContext;

    /**
     * 实现ApplicationContextAware接口的回调方法，设置上下文环境，注入Context到静态变量中.
     * @param applicationContext
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringBeanUtils.applicationContext = applicationContext;
    }

    /**
     * 获取applicationContext
     * @return applicationContext
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * 通过name获取 Bean.
     * @param name
     * @return 根据name获取到的Bean
     */
    public static Object getBean(String name) {
        if (applicationContext == null) {
            throw new RuntimeException("applicationContext注入失败");
        }
        return getApplicationContext().getBean(name);
    }

    /**
     * 通过class获取Bean.
     * 从静态变量applicationContext中取得Bean, 自动转型为所赋值对象的类型.
     */
    public static <T> T getBean(Class<T> requiredType) {
        assertContextInjected();
        return getApplicationContext().getBean(requiredType);
    }

    /**
     * 检查ApplicationContext不为空.
     */
    private static void assertContextInjected() {
        Validate.validState(applicationContext != null, "applicaitonContext属性未注入, 请在applicationContext.xml中定义SpringContextHolder.");
    }

    /**
     * 通过name,以及Clazz返回指定的Bean
     * @param name
     * @param requiredType
     * @param <T>
     * @return 根据name和requiredType获取到的Bean
     */
    public static <T> T getBean(String name, Class<T> requiredType) {
        if (applicationContext == null) {
            throw new RuntimeException("applicationContext注入失败");
        }
        return applicationContext.getBean(name, requiredType);
    }

    /**
     * 传入参数name, 检查是否存在该Bean
     */
    public static boolean containsBean(String name) {
        return applicationContext.containsBean(name);
    }

    /**
     * 传入参数name, 检查是否为单例
     */
    public static boolean isSingleton(String name) {
        return applicationContext.isSingleton(name);
    }

    /**
     * 传入参数name, 获取指定名称的Bean的类型
     */
    public static Class<? extends Object> getType(String name) {
        return applicationContext.getType(name);
    }

}

