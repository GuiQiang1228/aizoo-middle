package aizoo.config;

import aizoo.filter.CorsFilter;
import aizoo.filter.FrozenStatusCheckFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {
    @Bean
    public FilterRegistrationBean CorsFilter() {
        FilterRegistrationBean registrationBean = new FilterRegistrationBean();
        //注入过滤器
        registrationBean.setFilter(new CorsFilter());
        //过滤器名称
        registrationBean.setName("CorsFilter");
        //拦截规则
        registrationBean.addUrlPatterns("/*");
        //过滤器顺序
        registrationBean.setOrder(FilterRegistrationBean.HIGHEST_PRECEDENCE);// 这个需要最高，所有的请求，都是支持跨域的，包括/login、/logout等

        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean FrozenStatusCheckFilter() {
        FilterRegistrationBean registrationBean = new FilterRegistrationBean();
        //注入过滤器
        registrationBean.setFilter(new FrozenStatusCheckFilter());
        //过滤器名称
        registrationBean.setName("FrozenStatusCheckFilter");
        //拦截规则
        registrationBean.addUrlPatterns("/*");
        //过滤器顺序
        registrationBean.setOrder(150);// securityconfig的order是100，排在它后边就行

        return registrationBean;
    }
}
