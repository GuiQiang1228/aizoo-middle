package aizoo.config;

import aizoo.service.ServiceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;

@Configuration
public class WebConfig extends WebMvcConfigurerAdapter {

    @Value("${download.url}")
    private String downloadUrl;

    @Value("${download.dir}")
    private String downloadDir;

    @Value("${icon.url}")
    private String iconUrl;

    @Value("${icon.dir}")
    private String iconDir;

    @Value("${img.url}")
    private String imgUrl;

    @Value("${img.dir}")
    private String imgDir;

    private final static Logger logger = LoggerFactory.getLogger(WebConfig.class);

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        // 添加MultiRequestBody参数解析器
        argumentResolvers.add(new MultiRequestBodyArgumentResolver());
    }

    @Bean
    public HttpMessageConverter<String> responseBodyConverter() {
        // 解决中文乱码问题
        return new StringHttpMessageConverter(Charset.forName("UTF-8"));
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        super.configureMessageConverters(converters);
        converters.add(responseBodyConverter());
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        try {
            URL url = new URL(downloadUrl);
            registry.addResourceHandler(url.getPath() + "/**").addResourceLocations("file:" + downloadDir);
            url = new URL(iconUrl);
            registry.addResourceHandler(url.getPath() + "/**").addResourceLocations("file:" + iconDir);
            url = new URL(imgUrl);
            registry.addResourceHandler(url.getPath() + "/**").addResourceLocations("file:" + imgDir);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("AddResourceHandlers failed! e={}", e);
        }

    }
}