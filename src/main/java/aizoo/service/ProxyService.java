package aizoo.service;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ProxyService {
    private final static Logger logger = LoggerFactory.getLogger(ProxyService.class);

    /**
     * 执行代理查询
     * @param request 请求对象
     * @param response 响应对象
     * @param url 路径
     * @throws IOException
     */

    public void methodForward(HttpServletRequest request, HttpServletResponse response, URI url) throws IOException {
        logger.info("Start method Forward");
        logger.info("methodForward request:{},response:{},url{}",request,response,url);
        logger.info("执行代理查询...");
        //1.执行代理查询，获取客户端表单提交方法名称，将给定的方法值解析为HttpMethod
        logger.info("执行代理查询，获取客户端表单提交方法名称，将给定的方法值解析为HttpMethod");
        String methodName = request.getMethod();
        HttpMethod httpMethod = HttpMethod.resolve(methodName);
        if(httpMethod == null) {
            logger.info("httpMethod is null,End method Forward");
            return;
        }
        //2.构造请求对象
        logger.info("构造请求对象...");
        ClientHttpRequest delegate = new SimpleClientHttpRequestFactory().createRequest(url, httpMethod);
        Enumeration<String> headerNames = request.getHeaderNames();
        //3.设置请求头
        logger.info("设置请求头...");
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            Enumeration<String> v = request.getHeaders(headerName);
            List<String> arr = new ArrayList<>();
            while (v.hasMoreElements()) {
                arr.add(v.nextElement());
            }
            delegate.getHeaders().addAll(headerName, arr);
        }
        StreamUtils.copy(request.getInputStream(), delegate.getBody());
        //4.执行远程调用
        logger.info("执行远程调用...");
        ClientHttpResponse clientHttpResponse = delegate.execute();
        response.setStatus(clientHttpResponse.getStatusCode().value());
        //5.设置响应头
        logger.info("设置响应头...");
        clientHttpResponse.getHeaders().forEach((key, value) -> value.forEach(it -> {
            response.setHeader(key, it);
        }));
        StreamUtils.copy(clientHttpResponse.getBody(), response.getOutputStream());
        logger.info("End method Forward");
    }
}
