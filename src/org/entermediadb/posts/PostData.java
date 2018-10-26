package org.entermediadb.posts;

import java.util.HashMap;
import java.util.Map;

import org.openedit.data.BaseData;

public class PostData extends BaseData
{
	protected Map<String,String> fieldParameters = new HashMap();
	
	public Map<String,String> getSiteParameters()
	{
		return fieldParameters;
	}

	public void setSiteParameter(String inKey, String inValue)
	{
		getSiteParameters().put(inKey, inValue);
	}
	
	public String findAppPath(String inRequestedPath)
	{
		if( inRequestedPath.startsWith("/manager"))
		{
			return inRequestedPath;
		}
		String apppath = get("rootpath");
		return apppath + inRequestedPath;
	}

	public String getSiteParameter(String inName)
	{
		if( inName.equals("siteid"))
		{
			return getId();
		}
		return getSiteParameters().get(inName);
	}
}
