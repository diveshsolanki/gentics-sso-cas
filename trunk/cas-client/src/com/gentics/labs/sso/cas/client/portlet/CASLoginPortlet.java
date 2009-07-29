/*
 * @author herbert
 * @date Jul 28, 2009
 * @version $Id: $
 */
package com.gentics.labs.sso.cas.client.portlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.portlet.GenericPortlet;
import javax.portlet.PortletException;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.PortletSession;
import javax.portlet.PortletURL;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

public class CASLoginPortlet extends GenericPortlet {
    
    final static Logger logger = Logger.getLogger(CASLoginPortlet.class.getName());
    
    @Override
    protected void doView(RenderRequest request, RenderResponse response)
            throws PortletException, IOException {
        String serviceUrl = (String) request.getPortletSession().getAttribute("serviceUrl", PortletSession.APPLICATION_SCOPE);
        
        
        PortletPreferences prefs = request.getPreferences();
        String serverUrlPrefix = prefs.getValue("casServerUrlPrefix", null);
        String service = prefs.getValue("service", null);
        String backUrlPrefix = prefs.getValue("backUrlPrefix", null);
        
        if (serverUrlPrefix == null) {
            logger.severe("Invalid configuration: serverUrlPrefix is null.");
            return;
        }
        if (service == null) {
            logger.severe("Invalid configuration: service is null.");
            return;
        }
        
        PortletURL errorurl = response.createRenderURL();
        // we require a full url including protocol://host (this is only possible vendor specific)
        errorurl.setProperty("com.gentics.portalnode.hostabsolute", "true");
        errorurl.setParameter("error", "true");
        String onError = errorurl.toString();
        if (!onError.contains("://")) {
            // url was not generated absolute.
            onError = backUrlPrefix + onError;
        }
        
        String credentialValidate = serverUrlPrefix + "/credentialValidate";
        
        String lt = fetchLoginToken(credentialValidate);
        
        PortletRequestDispatcher dispatcher = getPortletContext().getRequestDispatcher("/views/loginform.jsp");
        
        request.setAttribute("credentialValidateUrl", credentialValidate);
        request.setAttribute("loginToken", lt);
        request.setAttribute("onErrorUrl", onError);
        request.setAttribute("service", service);
        
        try {
        dispatcher.include(request, response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    private String fetchLoginToken(String serviceValidate) {
        try {
            URL url = new URL(serviceValidate + "?cmd=fetchlt");
            InputStream stream = url.openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line = reader.readLine();
            stream.close();
            return line;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error while fetching login token.", e);
        }
        return null;
    }
}
