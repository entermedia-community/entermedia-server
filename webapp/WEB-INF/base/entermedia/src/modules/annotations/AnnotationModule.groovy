package model.projects;

import java.util.Iterator;

import org.openedit.Data;
import com.openedit.users.*;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.modules.BaseMediaModule;
import org.openedit.profile.UserProfile;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;

public class AnnotationModule extends BaseMediaModule
{
	private final String[] availableColors = ["#00FFFF",
		"#00FF00","#EE82EE","#000080","#000000","#FFFF00","#B8860B","#B8860B","#723421","#523421","#323421",
		"#123421", "#fff120", "#abf000", "#ff4300"];
	
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
