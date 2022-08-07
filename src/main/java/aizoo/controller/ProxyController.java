package aizoo.controller;

import aizoo.aspect.WebLog;

import aizoo.response.BaseResponse;
import aizoo.service.ProxyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@BaseResponse
public class ProxyController {

    @Autowired
    private ProxyService proxyService;

    @Value("${proxy.targetAddr}")
    private String targetAddr;

    private static final Logger logger = LoggerFactory.getLogger(ProxyController.class);

    /**
     * 代理所有请求
     * 若是代理转发1，则先获取路径，判断路径是否要编辑文件，若要则判断用户是否可以访问这个命名空间下的文件，之后进行代理查询
     * @param request 存储http请求对象
     * @param response 存储http响应对象
     * @throws Exception
     */

    @WebLog(description = "代理转发1")
    @RequestMapping(
            value = "/notebook/**",
            produces = "application/json;charset=UTF-8")
    public void proxy(HttpServletRequest request, HttpServletResponse response, Principal principal) throws IOException, URISyntaxException {
        //1.获取请求的除去host部分的路径
        logger.info("获取请求的除去host部分的路径");
        URI uri = new URI(request.getRequestURI());
        String path = uri.getPath();
        //2.动态编辑用户文件时，先判断该用户是否可以访问这个命名空间下的文件
        // 获取命名空间的拥有者，若其与用户名不相同则提示无法访问其他用户的文件
        logger.info("动态编辑用户文件时，先判断该用户是否可以访问这个命名空间下的文件");
        if (path.contains("/notebook/edit")) {
            String username = principal.getName();
            String realPath = path.replaceAll("/notebook/edit/", "");
            String owner = realPath.substring(0, realPath.indexOf('/'));
            if (!owner.equals(username))
                response.sendError(403, "无法访问其它用户的文件！");
        }
        // 3.更新路径，执行代理查询
        logger.info("更新路径，执行代理查询");
        URI newUri = new URI(targetAddr + path.replace("/notebook", ""));
        proxyService.methodForward(request, response, newUri);
    }

    /**
     * 若是代理转发notebook静态文件，则获取路径，更新路径，执行代理查询
     * @param request 存储http请求对象
     * @param response 存储http响应对象
     * @param principal 带有用户信息的对象
     * @throws IOException
     * @throws URISyntaxException
     */
    @WebLog(description = "代理转发notebook静态文件")
    @RequestMapping(
            value = {
                    "/static/components/**",
                    "/static/custom/**",
                    "/static/edit/**",
                    "/static/services/**",
                    "/static/style/**"
            },
            produces = "application/json;charset=UTF-8")
    public void proxyStatic(HttpServletRequest request, HttpServletResponse response, Principal principal) throws IOException, URISyntaxException {
        //1.获取请求的除去host部分的路径
        logger.info("获取请求的除去host部分的路径");
        URI uri = new URI(request.getRequestURI());
        String path = uri.getPath();
        //2.更新路径
        logger.info("更新路径");
        URI newUri = new URI(targetAddr + path);
        System.out.println("new Uri=" + newUri);
        //3.执行代理查询
        logger.info("执行代理查询");
        proxyService.methodForward(request, response, newUri);
    }

    /**
     *若是代理转发2，则获取路径，获取查询字符串，根据路径更新路径，执行代理查询
     * @param request 存储http请求对象
     * @param response 存储http响应对象
     * @param principal 带有用户信息的对象
     * @throws IOException
     * @throws URISyntaxException
     */
    @WebLog(description = "代理转发2")
    @RequestMapping(
            value = {
            "/api/config/**",
            "/custom/**",
            "/api/contents/**"
            },
            produces = "application/json;charset=UTF-8")
    public void proxy2(HttpServletRequest request, HttpServletResponse response, Principal principal) throws IOException, URISyntaxException {
        //1.获取请求的除去host部分的路径，获取查询字符串
        logger.info("获取请求的除去host部分的路径，获取查询字符串");
        URI uri = new URI(request.getRequestURI());
        String path = uri.getPath();
        String query = request.getQueryString();
        URI newUri;
        if (query != null) {
            // 2.判断该用户是否可以访问这个命名空间下的文件
            logger.info("判断该用户是否可以访问这个命名空间下的文件");
            // http请求url示例：http://localhost:8080/api/contents/gq/function/function0409/my_add.py?type=file&format=text&_=1626051829966
            if (path.contains("contents")) {
                String username = principal.getName();
                String realPath = path.replaceAll("/api/contents/", "");
                // 获取命名空间的拥有者
                String owner = realPath.substring(0, realPath.indexOf('/'));
                if (!owner.equals(username))
                    response.sendError(403, "无法访问其它用户的文件！");
            }
            newUri = new URI(targetAddr + path + "?" + query);
        } else
            newUri = new URI(targetAddr + path);
        // 3.执行代理查询
        logger.info("执行代理查询");
        proxyService.methodForward(request, response, newUri);
    }

}
