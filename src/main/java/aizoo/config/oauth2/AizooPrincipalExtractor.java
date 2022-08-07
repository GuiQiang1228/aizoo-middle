package aizoo.config.oauth2;

import aizoo.repository.UserDAO;
import aizoo.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.PrincipalExtractor;
import java.util.Map;


public class AizooPrincipalExtractor implements PrincipalExtractor {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private UserService userService;

    private Logger logger = LoggerFactory.getLogger(AizooPrincipalExtractor.class);

    @Override
    public Object extractPrincipal(Map<String, Object> map) {
        System.out.println("~~~~~~~~~~~~~~~~~~~~~~~"+map);
        Map<String, Object> userPrincipal = (Map<String, Object>) map.get("principal");
        String username = userPrincipal.get("username").toString();
        String email = userPrincipal.get("email").toString();
        logger.info("username:" + username);
        logger.info("email:" + email);
        //检查用户名是否已经在系统中存在，不存在则创建用户
        userService.addUserSync(username,email);
        return userDAO.findByUsername(username);
    }
}
