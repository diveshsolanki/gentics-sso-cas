package com.gentics.labs.sso.cas.client.gpn;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.portlet.PortletException;
import javax.portlet.PortletURL;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.InsufficientPrivilegesException;
import com.gentics.contentnode.content.GenticsContentObject;
import com.gentics.lib.base.CMSUnavailableException;
import com.gentics.lib.base.NodeIllegalArgumentException;
import com.gentics.portalnode.genericmodules.GenticsContentModule;

/**
 * Simple subclass of GenticsContentModule to handle reauthentication against a
 * CAS server for special security needs.
 * 
 * @author herbert
 */
public class ContentModuleWithReauthentication extends GenticsContentModule {
	
	private static Logger logger = Logger.getLogger(ContentModuleWithReauthentication.class.getName());

	public ContentModuleWithReauthentication(String moduleId)
			throws PortletException {
		super(moduleId);
	}
	
	@Override
	protected void doView(RenderRequest req, RenderResponse res)
			throws PortletException, IOException {
		String reauthenticationAttributeName = ObjectTransformer.getString(
				getGenticsPortletContext().getStringModuleParameter("reauthentication_attribute_name"),
				"requirereauthentication");
		if (ObjectTransformer.getBoolean(getProperty(reauthenticationAttributeName), false)) {
			// this content has to be extra secured.
			String casServerUrlPrefix = getGenticsPortletContext().getStringModuleParameter("casServerUrlPrefix");
			if (!ReauthenticationHelper.getInstance(casServerUrlPrefix)
					.reauthenticateOrShowPasswordField(req, res, getGenticsPortletContext().getModuleId())) {
				return;
			}
		}
		super.doView(req, res);
	}
	
	
	@Override
	public void serveResource(ResourceRequest request, ResourceResponse response)
			throws PortletException, IOException {
		String reauthenticationAttributeName = ObjectTransformer.getString(
				getGenticsPortletContext().getStringModuleParameter("reauthentication_attribute_name"),
				"requirereauthentication");
		try {
			GenticsContentObject obj = getObjectFromRequest(request);
			if (ObjectTransformer.getBoolean(obj.get(reauthenticationAttributeName), false)) {
				// this content has to be extra secured - redirect user to a login form ..
				PortletURL renderUrl = response.createRenderURL();
				renderUrl.setParameter("cmd", "showpasswordfield");
				renderUrl.setParameter("url", "");
			}
			writeResourceIntoResponse(request, response, obj);
		} catch (CMSUnavailableException e) {
			logger.log(Level.SEVERE, "Error while serving resource", e);
		} catch (NodeIllegalArgumentException e) {
			logger.log(Level.SEVERE, "Error while serving resource", e);
		} catch (InsufficientPrivilegesException e) {
			handleInsufficentPermissionsForServeResource(response);
		}
	}

}
