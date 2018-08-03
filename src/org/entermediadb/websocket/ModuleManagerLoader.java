package org.entermediadb.websocket;


import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

import org.entermediadb.websocket.annotation.AnnotationServer;
import org.openedit.ModuleManager;

public class ModuleManagerLoader extends ServerEndpointConfig.Configurator
{
	@Override
    public void modifyHandshake(ServerEndpointConfig config, 
                                HandshakeRequest request, 
                                HandshakeResponse response)
    {
        HttpSession httpSession = (HttpSession)request.getHttpSession();
        config.getUserProperties().put(HttpSession.class.getName(),httpSession);
    	ModuleManager manager  = (ModuleManager)httpSession.getAttribute("moduleManager");
        config.getUserProperties().put("moduleManager",manager);
    }
}