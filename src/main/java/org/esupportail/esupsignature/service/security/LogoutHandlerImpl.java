package org.esupportail.esupsignature.service.security;

import org.esupportail.esupsignature.service.DataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class LogoutHandlerImpl implements LogoutHandler {

    private static final Logger logger = LoggerFactory.getLogger(LogoutHandlerImpl.class);

    @Resource
    private SessionRegistry sessionRegistry;

    @Resource
    List<SecurityService> securityServices;

    private RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void logout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) {

        List<Object> principals = sessionRegistry.getAllPrincipals();
        for (Object principal : principals) {
            if(principal.equals(authentication.getPrincipal())) {
                for(SessionInformation sessionInformation : sessionRegistry.getAllSessions(principal, false)) {
                    sessionInformation.expireNow();
                }
            }
        }
        try {
            String securityServiceName = httpServletRequest.getSession().getAttribute("securityServiceName").toString();
            for(SecurityService securityService : securityServices) {
                if(securityService.getClass().getSimpleName().equals(securityServiceName)) {
                    redirectStrategy.sendRedirect(httpServletRequest, httpServletResponse, securityService.getLogoutUrl());
                }
            }
        } catch (IOException e) {
            logger.error("error on logout", e);
        }
    }

}
