/*
 * @author norbert
 * @date 10.07.2009
 * @version $Id: $
 */
package com.gentics.labs.sso.cas.client;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.jasig.cas.client.util.CommonUtils;
import org.jasig.cas.client.validation.Assertion;

/**
 * @author norbert
 */
public class CASIntegrationFilter implements Filter {
    
    private String sessionAttributeName;

    /*
     * (non-Javadoc)
     * @see javax.servlet.Filter#destroy()
     */
    public void destroy() {
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
     *      javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest servletRequest = (HttpServletRequest) request;
            HttpSession session = servletRequest.getSession();
            String serviceUrl = CommonUtils.constructServiceUrl(servletRequest, (HttpServletResponse) response,
                    null, null, null, false);
            session.setAttribute("serviceUrl", serviceUrl);
            if (session != null) {
                Object assertObj = session.getAttribute("_const_cas_assertion_");
                if (assertObj instanceof Assertion) {
                    Assertion assertion = (Assertion) assertObj;
                    
                    session.setAttribute(
                            sessionAttributeName,
                            assertion.getPrincipal().getAttributes());
                }
            }
        }
        chain.doFilter(request, response);
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    public void init(FilterConfig config) throws ServletException {
        sessionAttributeName = config.getInitParameter("sessionAttributeName");
        if (sessionAttributeName == null) {
            sessionAttributeName = "com.gentics.portalnode.remoteuserdata";
        }
    }
}
