package org.entermediadb.asset.modules;

import org.entermediadb.websocket.annotation.AnnotationManager;
import org.json.simple.JSONObject;
import org.openedit.WebPageRequest;
import org.openedit.users.User;

public class AnnotationModule extends BaseMediaModule
{
	public AnnotationManager loadAnnotationManager(WebPageRequest inReq)
	{
        AnnotationManager server = (AnnotationManager) getModuleManager().getBean("system","annotationManager");
        inReq.putPageValue("annotationManager", server);
        return server;
	}
	
	public JSONObject loadAnnotatedAsset(WebPageRequest inReq)
	{
		AnnotationManager manager = loadAnnotationManager(inReq);
		String catalogid = inReq.findValue("catalogid");
		String assetid = inReq.findValue("assetid");
		JSONObject asset = manager.loadAnnotatedAsset(catalogid,assetid);
		inReq.putPageValue("jsonasset", asset);
		return asset;
	}
	private final String[] availableColors = {"#00FFFF",
			"#00FF00","#EE82EE","#000080","#000000","#FFFF00","#B8860B","#B8860B","#723421","#523421","#323421",
			"#123421", "#fff120", "#abf000", "#ff4300"};
		
		public void pickUserColor(WebPageRequest inReq) throws Exception
		{
			User user = inReq.getUser();
			
			if( user != null && user.get("defaultcolor") == null )
			{
				char first = user.getScreenName().toLowerCase().charAt(0);
				if( Character.isLetter(first) )
				{
					int index = first - 97;
					index = index /2;
					String color = availableColors[index];
					user.setProperty("defaultcolor",color);
				}
				else
				{
					user.setProperty("defaultcolor","#00FF00");
				}
			}
		}
}
