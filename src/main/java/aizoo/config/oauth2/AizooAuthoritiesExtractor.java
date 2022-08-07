package aizoo.config.oauth2;

import aizoo.domain.User;
import aizoo.repository.UserDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.AuthoritiesExtractor;
import org.springframework.security.core.GrantedAuthority;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AizooAuthoritiesExtractor implements AuthoritiesExtractor {

    @Autowired
    private UserDAO userDAO;

    @Override
    public List<GrantedAuthority> extractAuthorities(Map<String, Object> map) {
        Map<String, Object> userPrincipal= (Map<String, Object>) map.get("principal");
        String username=userPrincipal.get("username").toString();
        User user=userDAO.findByUsername(username);
        return user==null? new ArrayList<>(): (List<GrantedAuthority>) user.getAuthorities();
    }
}
