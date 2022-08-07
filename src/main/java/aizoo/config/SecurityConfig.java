package aizoo.config;

import aizoo.config.oauth2.AizooAuthoritiesExtractor;
import aizoo.config.oauth2.AizooLogoutHandler;
import aizoo.config.oauth2.AizooPrincipalExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.client.EnableOAuth2Sso;
import org.springframework.boot.autoconfigure.security.oauth2.resource.AuthoritiesExtractor;
import org.springframework.boot.autoconfigure.security.oauth2.resource.PrincipalExtractor;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * Description: Security 配置类
 */
@EnableWebSecurity
@EnableOAuth2Sso //使用/login端点用于触发基于OAuth2的SSO流程，这个入口地址也可以通过security.oauth2.sso.login-path来修改
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private AizooLogoutHandler aizooLogoutHandler;

    @Value("${server.servlet.session.cookie.name}")
    private String oauthCookieName;

    @Value("${logoutSuccessUrl}")
    private String logoutSuccessUrl;
    /**
     * 所有的访问，都需要经过验证oauth server验证
     * 匹配 "/api" 及其以下所有路径，需要 "USER" 或者"ADMIN"权限
     * 匹配 "/admin" 及其以下所有路径，都需要 "ADMIN" 权限
     * 登录地址为 "/login"
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .authorizeRequests()
                .antMatchers("/api/**").hasAnyRole("USER", "ADMIN")
                .antMatchers("/admin/**").hasRole("ADMIN")

                .and().logout().deleteCookies("JSESSIONID", oauthCookieName).invalidateHttpSession(true)
                .logoutSuccessUrl(logoutSuccessUrl)
//                .addLogoutHandler(aizooLogoutHandler)

                .and()
                .authorizeRequests()
                .anyRequest()
                .authenticated();

        http.sessionManagement().maximumSessions(1).expiredUrl("/logout");

    }

    @Bean
    public PrincipalExtractor principalExtractor() {
        return new AizooPrincipalExtractor();
    }

    @Bean
    public AuthoritiesExtractor authoritiesExtractor() {
        return new AizooAuthoritiesExtractor();
    }
}