/*
 * @author herbert
 * @date Jul 28, 2009
 * @version $Id: $
 */
package com.gentics.labs.sso.cas.server.web;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.inspektr.common.ioc.annotation.NotEmpty;
import org.inspektr.common.ioc.annotation.NotNull;
import org.jasig.cas.CentralAuthenticationService;
import org.jasig.cas.authentication.principal.RememberMeCredentials;
import org.jasig.cas.authentication.principal.RememberMeUsernamePasswordCredentials;
import org.jasig.cas.authentication.principal.WebApplicationService;
import org.jasig.cas.ticket.ExpirationPolicy;
import org.jasig.cas.ticket.Ticket;
import org.jasig.cas.ticket.TicketException;
import org.jasig.cas.ticket.registry.TicketRegistry;
import org.jasig.cas.ticket.support.TimeoutExpirationPolicy;
import org.jasig.cas.util.UniqueTicketIdGenerator;
import org.jasig.cas.web.support.ArgumentExtractor;
import org.jasig.cas.web.support.CookieRetrievingCookieGenerator;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;


/**
 * Simple controller which generates login tickets through a RESTful API and
 * validates username/password This is required if you want your own login form
 * embedded in your website/portal/etc instead of using the CAS login form.
 * 
 * @author herbert
 */
public class CredentialValidator extends AbstractController {
    
    private final Log log = LogFactory.getLog(CredentialValidator.class);
    
    private static final String LOGINTICKET_PREFIX = "LT";
    
    @NotNull
    private TicketRegistry loginTicketRegistry;
    
    @NotNull
    private UniqueTicketIdGenerator loginTicketUniqueIdGenerator;
    
    @NotNull
    private CentralAuthenticationService centralAuthenticationService;
    
    @NotNull
    private ExpirationPolicy loginTicketExpirationPolicy = new TimeoutExpirationPolicy(6 * 60 * 60 * 1000);
    
    @NotNull
    private CookieRetrievingCookieGenerator ticketGrantingTicketCookieGenerator;
    
    /** Extractors for finding the service. */
    @NotEmpty
    private List<ArgumentExtractor> argumentExtractors;

    
    public CredentialValidator() {
    }

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest req,
            HttpServletResponse res) throws Exception {
        String cmd = req.getParameter("cmd");
        if ("fetchlt".equals(cmd)) {
            // Requestor wants to generate a login ticket. return one.
            String ticketId = loginTicketUniqueIdGenerator.getNewTicketId(LOGINTICKET_PREFIX);
            LoginTicket loginTicket = new LoginTicket(ticketId, loginTicketExpirationPolicy);
            loginTicketRegistry.addTicket(loginTicket);
            return new ModelAndView("credentialValidatorFetchLT", "loginTicket", loginTicket.getId());
        } else {
            // this was a validation request.
            String lt = req.getParameter("lt");
            String onError = req.getParameter("on_error");
            if (onError == null) {
                throw new IllegalArgumentException("View requires an on_error parameter.");
            }
            try {
                Ticket ticket = loginTicketRegistry.getTicket(lt, LoginTicket.class);
                if (ticket == null || ticket.isExpired()) {
                    logger.debug("Invalid login ticket {" + lt + "} or ticket is expired. {" + ticket + "}");
                    // ticket is already expired, or invalid. send user to the error URL.
                    res.sendRedirect(onError);
                    return null;
                }
                // ticket was validated. now validate username/password ...
                final RememberMeUsernamePasswordCredentials c = new RememberMeUsernamePasswordCredentials();
                c.setUsername(req.getParameter("username"));
                c.setPassword(req.getParameter("password"));
                c.setRememberMe(StringUtils.hasText(req.getParameter(RememberMeCredentials.REQUEST_PARAMETER_REMEMBER_ME)));
                String tgt;
                try {
                    tgt = centralAuthenticationService.createTicketGrantingTicket(c);
                } catch (TicketException e) {
                    // invalid username/password - send to error page.
                    log.debug("Unable to create ticket granting ticket - invalid credentials?", e);
                    res.sendRedirect(onError);
                    return null;
                }
                ticketGrantingTicketCookieGenerator.addCookie(req, res, tgt);
                
                // Ticket Granting Ticket was added. Now we need to create a service ticket.
                WebApplicationService service = null;
                for (ArgumentExtractor argumentExtractor : argumentExtractors) {
                    service = argumentExtractor.extractService(req);
                    if (service != null) {
                        break;
                    }
                }
                if (service == null) {
                    log.error("Unable to retrieve service from request.");
                    res.sendRedirect(onError);
                }
                String st = centralAuthenticationService.grantServiceTicket(tgt, service);
                // successful login. redirect to service.
                res.sendRedirect(service.getResponse(st).getUrl());
            } finally {
                loginTicketRegistry.deleteTicket(lt);
            }
        }
        return null;
    }

    public TicketRegistry getLoginTicketRegistry() {
        return loginTicketRegistry;
    }

    public void setLoginTicketRegistry(TicketRegistry loginTicketRegistry) {
        this.loginTicketRegistry = loginTicketRegistry;
    }

    public CentralAuthenticationService getCentralAuthenticationService() {
        return centralAuthenticationService;
    }

    public void setCentralAuthenticationService(
            CentralAuthenticationService centralAuthenticationService) {
        this.centralAuthenticationService = centralAuthenticationService;
    }
    
    public void setTicketGrantingTicketCookieGenerator(
            CookieRetrievingCookieGenerator ticketGrantingTicketCookieGenerator) {
        this.ticketGrantingTicketCookieGenerator = ticketGrantingTicketCookieGenerator;
    }

    public void setLoginTicketExpirationPolicy(ExpirationPolicy loginTicketExpirationPolicy) {
        this.loginTicketExpirationPolicy = loginTicketExpirationPolicy;
    }
    
    public void setArgumentExtractors(List<ArgumentExtractor> argumentExtractors) {
        this.argumentExtractors = argumentExtractors;
    }
    
    public void setLoginTicketUniqueIdGenerator(
            UniqueTicketIdGenerator loginTicketUniqueIdGenerator) {
        this.loginTicketUniqueIdGenerator = loginTicketUniqueIdGenerator;
    }
}
