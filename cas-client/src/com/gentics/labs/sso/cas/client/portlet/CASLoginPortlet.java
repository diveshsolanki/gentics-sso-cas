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
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

public class CASLoginPortlet extends GenericPortlet {
    
    final static Logger logger = Logger.getLogger(CASLoginPortlet.class.getName());
    
    @Override
    protected void doView(RenderRequest request, RenderResponse response)
            throws PortletException, IOException {
        PortletPreferences prefs = request.getPreferences();
        String serverUrlPrefix = prefs.getValue("serverUrlPrefix", null);
        String credentialValidate = serverUrlPrefix + "/credentialValidate";
        
        String lt = fetchLoginToken(credentialValidate);
        
        PortletRequestDispatcher dispatcher = getPortletContext().getRequestDispatcher("views/loginform.jsp");
        
        request.setAttribute("credentialValidateUrl", credentialValidate);
        request.setAttribute("loginToken", lt);
        
        dispatcher.include(request, response);
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
