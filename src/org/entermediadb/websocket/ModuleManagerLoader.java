package org.entermediadb.websocket;


import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

import org.entermediadb.websocket.annotation.AnnotationManager;
import org.openedit.ModuleManager;

public class ModuleManagerLoader extends ServerEndpointConfig.Configurator
{
	@Override
    public void modifyHandshake(ServerEndpointConfig config, 
                                HandshakeRequest request, 
                                HandshakeResponse response)
    {
        HttpSession httpSession = (HttpSession)request.getHttpSession();
        if( httpSession != null)
        {
        	//We will have a session because the path is the same as the filter:
        	//https://stackoverflow.com/questions/17936440/accessing-httpsession-from-httpservletrequest-in-a-web-socket-serverendpoint
        	/*
        	 First, use a filter with the same path as the WebSocket. This will give you access to the HttpServletRequest and HttpSession. It also gives you a chance to create a session if it doesn't already exist (although in that case using an HTTP session at all seems dubious).
        	 */
	        config.getUserProperties().put(HttpSession.class.getName(),httpSession);
	    	ModuleManager manager  = (ModuleManager)httpSession.getAttribute("moduleManager");
	        config.getUserProperties().put("moduleManager",manager);
        } 
    }
}