package aizoo.filter;

import aizoo.common.UserStatusType;
import aizoo.domain.User;
import aizoo.repository.UserDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;

public class FrozenStatusCheckFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(FrozenStatusCheckFilter.class);

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {

        logger.info("Start doFilter");
        logger.info("doFilter request:{},response:{},filterChain:{}",request,response,filterChain);
        //获取容器
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String url = req.getRequestURL().toString();
        if (!(url.contains("accountFreeze") || url.contains("/static/")) && frozen(req)) {
            resp.sendRedirect("/accountFreeze.html");
            logger.info("End do Filter");
            return;
        }
        filterChain.doFilter(request, response);
        logger.info("End do Filter");
    }

    private boolean frozen(HttpServletRequest req) {//需要考虑未登录，不存在username或principal时的情况
        logger.info("Start frozen");
        ServletContext context = req.getServletContext();
        ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(context);
        UserDAO userDAO = ctx.getBean(UserDAO.class);

        Principal principal = req.getUserPrincipal();
        if (principal == null) {
            logger.info("principal is null, frozen return:false");
            logger.info("End frozen");
            return false;
        }
        String username = principal.getName();
        if (username == null) {
            logger.info("username is null, frozen return:false");
            logger.info("End frozen");
            return false;
        }
        User user = userDAO.findByUsername(username);
        if (user == null) {
            logger.info("user is null, frozen return:false");
            logger.info("End frozen");
            return false;
        }
        if (user.getStatusName() == null) {
            logger.info("user.getStatusName() is null, frozen return:true");
            logger.info("End frozen");
            return true;
        }
        logger.info("{}",user.getStatusName().equals(UserStatusType.FROZEN_STATUS));
        logger.info("End frozen");
        return user.getStatusName().equals(UserStatusType.FROZEN_STATUS);
    }
}
