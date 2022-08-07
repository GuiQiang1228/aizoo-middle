package aizoo.utils;

import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import aizoo.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;


public class SessionUtil {

    private static SecurityContext attribute;

    private static final Logger logger = LoggerFactory.getLogger(SessionUtil.class);
    /**
     * 根据当前session获取当前登录用户对象
     * @param session Session
     * @return guser
     */
    public static User getUser(HttpSession session) {
        try {
            attribute = (SecurityContext) session.getAttribute("SPRING_SECURITY_CONTEXT");
            return (User) attribute.getAuthentication().getPrincipal();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 根据当前session获取当前登录用户ID
     * @param session Session
     * @return guser
     */
    public static Integer getUserId(HttpSession session) {
        try {
            attribute = (SecurityContext) session.getAttribute("SPRING_SECURITY_CONTEXT");
            User principal = (User) attribute.getAuthentication().getPrincipal();
            return principal.getId().intValue();
        } catch (Exception e) {
            logger.error("获取用户Id失败, 错误: ",e.getMessage());
        }
        return null;
    }

    /**
     * 根据session获取用户名称
     * @param session Session
     * @return void
     */
    public static String getUserName(HttpSession session) {
        try {
            attribute = (SecurityContext) session.getAttribute("SPRING_SECURITY_CONTEXT");
            User principal = (User) attribute.getAuthentication().getPrincipal();
            return principal.getUsername();
        } catch (Exception e) {
            logger.error("获取用户名失败, 错误: ", e.getMessage());
        }
        return null;
    }

    /**
     * 根据session获取count
     * session中包含一个count键默认为null，可以用来统计登录次数
     * @param session session
     */
    public static void count(HttpSession session) {
        ServletContext context = session.getServletContext();
        logger.info("sessionId: {} 的count是: {}", session.getId(), context.getAttribute("count"));
    }


    /**
     * 辨别用户是否已经登录，如果已经登录就不能登录了。
     *
     * @param request request
     * @param sessionRegistry sessionRegistry
     * @param loginedUser 已登录用户
     */
    public static void deleteSameUser(HttpServletRequest request, SessionRegistry sessionRegistry, org.springframework.security.core.userdetails.User loginedUser) {
        SecurityContext sc = (SecurityContext) request.getSession().getAttribute("SPRING_SECURITY_CONTEXT");
        List<SessionInformation> sessionsInfo;
        sessionsInfo = sessionRegistry.getAllSessions(sc.getAuthentication().getPrincipal(), true);
        String currentSessionId;
        if (null != sessionsInfo && sessionsInfo.size() == 0) {
            sessionRegistry.registerNewSession(request.getSession().getId(), sc.getAuthentication().getPrincipal());
            sessionsInfo = sessionRegistry.getAllSessions(sc.getAuthentication().getPrincipal(), false);
        }
        assert sessionsInfo != null;
        currentSessionId = sessionsInfo.get(0).getSessionId();
        List<Object> o = sessionRegistry.getAllPrincipals();
        for (Object principal : o) {
            if (principal instanceof org.springframework.security.core.userdetails.User && (loginedUser.getUsername().equals(((org.springframework.security.core.userdetails.User) principal).getUsername()))) {
                List<SessionInformation> oldSessionsInfo = sessionRegistry.getAllSessions(principal, false);
                if (null != oldSessionsInfo && oldSessionsInfo.size() > 0 && !oldSessionsInfo.get(0).getSessionId().equals(currentSessionId)) {
                    for (SessionInformation sessionInformation : sessionsInfo) {
                        //当前session失效
                        sessionInformation.expireNow();
                        sc.setAuthentication(null);
                        sessionRegistry.removeSessionInformation(currentSessionId);
                        logger.info("用户已经登录..");
                    }
                }
            }
        }
    }

    /**
     * 会话销毁(剔除前一个用户)
     * @param session session
     * @param , SysMessageService sysMessageService
     */
    public static void expireSession(HttpSession session) {
        session.invalidate();
    }


    /**
     * 剔除前一个用户
     * @param request request
     * @param sessionRegistry sessionRegistry
     * @param loginedUser loginedUser
     * @param , SysMessageService sysMessageService
     */
    public static void dropPreviousUser2(HttpServletRequest request, SessionRegistry sessionRegistry, User loginedUser) {
        List<SessionInformation> sessionsInfo = null;
        //登录以后session里才会加入键名为"SPRING_SECURITY_CONTEXT"的字段
        SecurityContext sc = (SecurityContext) request.getSession().getAttribute("SPRING_SECURITY_CONTEXT");

        if(sc!=null) {
            //获取当前登录用户的会话信息集合
            sessionsInfo = sessionRegistry.getAllSessions(sc.getAuthentication().getPrincipal(), false);
            if (sessionsInfo.size() > 0) {
                //当前会话ID
                String  currentSessionId = sessionsInfo.get(0).getSessionId();
                //获取所有已经登录的用户
                List<Object> o = sessionRegistry.getAllPrincipals();
                for (Object principal : o) {
                    //当登录用户的名字和已经登录用户的名字相同，也就是登录用户已经登录过了。
                    if (principal instanceof org.springframework.security.core.userdetails.User && (loginedUser.getUsername().equals(((org.springframework.security.core.userdetails.User) principal).getUsername()))) {
                        //获取已经登录用户的会话信息集合
                        List<SessionInformation> oldSessionsInfo = sessionRegistry.getAllSessions(principal, false);
                        //如果会话信息不为空且会话信息的ID不等于当前会话ID
                        if (null != oldSessionsInfo && oldSessionsInfo.size() > 0 && !oldSessionsInfo.get(0).getSessionId().equals(currentSessionId)) {
                            //遍历已经登录用户的会话信息，并设置过期，即删除session
                            for (SessionInformation sessionInformation : oldSessionsInfo) {
                                //旧用户的session失效、
                                logger.info("旧用户的session失效");
                                sessionInformation.expireNow();
                            }
                        }
                    }
                }
            }
        }

    }

    /**
     * session 失效
     * @param request request
     * @param sessionRegistry sessionRegistry
     */
    public static void expireSession(HttpServletRequest request, org.springframework.security.core.userdetails.User user, SessionRegistry sessionRegistry) {
        List<SessionInformation> sessionsInfo = null;
        if (null != user) {
            List<Object> o = sessionRegistry.getAllPrincipals();
            for (Object principal : o) {
                if (principal instanceof org.springframework.security.core.userdetails.User && (user.getUsername().equals(((org.springframework.security.core.userdetails.User) principal).getUsername()))) {
                    sessionsInfo = sessionRegistry.getAllSessions(principal, false);
                }
            }
        } else if (null != request) {
            SecurityContext sc = (SecurityContext) request.getSession().getAttribute("SPRING_SECURITY_CONTEXT");
            if (null != sc.getAuthentication().getPrincipal()) {
                sessionsInfo = sessionRegistry.getAllSessions(sc.getAuthentication().getPrincipal(), false);
                sc.setAuthentication(null);
            }
        }
        if (null != sessionsInfo && sessionsInfo.size() > 0) {
            for (SessionInformation sessionInformation : sessionsInfo) {
                //当前session失效
                sessionInformation.expireNow();
                sessionRegistry.removeSessionInformation(sessionInformation.getSessionId());
            }
        }
    }

    /**
     * 打印Session相关信息
     * @param request
     */
    public void showSession(HttpServletRequest request) {
        //获取session
        HttpSession session = request.getSession();
        // 获取session中所有的键值
        Enumeration<String> attrs = session.getAttributeNames();
        // 遍历attrs中的
        while(attrs.hasMoreElements()){
            // 获取session键值
            String name = attrs.nextElement();
            // 根据键值取session中的值
            Object value = session.getAttribute(name);
            logger.info("--sessionID: {}\n--name: {}\n--value: {}\n", session.getId(), name, value);
        }

    }
}