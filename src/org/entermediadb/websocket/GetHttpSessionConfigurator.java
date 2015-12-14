package org.entermediadb.websocket;


import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

import org.entermediadb.websocket.annotation.AnnotationServer;
import org.openedit.ModuleManager;

public class GetHttpSessionConfigurator extends ServerEndpointConfig.Configurator
{
	protected AnnotationServer fieldAnnotationServer;
	
	
    public AnnotationServer getAnnotationServer()
	{
    	if (fieldAnnotationServer == null)
		{
			fieldAnnotationServer = new AnnotationServer();
		}
		return fieldAnnotationServer;
	}


	public void setAnnotationServer(AnnotationServer inAnnotationServer)
	{
		fieldAnnotationServer = inAnnotationServer;
	}


	@Override
    public void modifyHandshake(ServerEndpointConfig config, 
                                HandshakeRequest request, 
                                HandshakeResponse response)
    {
        HttpSession httpSession = (HttpSession)request.getHttpSession();
        config.getUserProperties().put(HttpSession.class.getName(),httpSession);
        config.getUserProperties().put("AnnotationServer",getAnnotationServer());
        if( getAnnotationServer().getModuleManager() == null)
        {
        	ModuleManager manager  = (ModuleManager)httpSession.getAttribute("moduleManager");
        	getAnnotationServer().setModuleManager(manager);
        }
    }
}