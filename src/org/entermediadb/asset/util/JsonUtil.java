package org.entermediadb.asset.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import org.json.simple.JSONObject;
import org.openedit.util.DateStorageUtil;

public class JsonUtil
{
	public String formatDateObj(Date inDate)
	{
		if( inDate == null)
		{
			return "";
		}
		String json = DateStorageUtil.getStorageUtil().formatDateObj(inDate, "yyyy-MM-dd'T'HH:mm:ss");
		return json;
	}
	public String formatDate(String inDate)
	{
		if( inDate == null)
		{
			return "";
		}
		String json = DateStorageUtil.getStorageUtil().formatDate(inDate, "yyyy-MM-dd'T'HH:mm:ss");
		return json;
	}
	public String escape(String inVal){
		String escape = JSONObject.escape(inVal);
		return escape;
	}
	
	public Collection parseArray(String inName, String inJsonArray)
	{
		Collection all = new ArrayList();
		int name = inJsonArray.indexOf(inName);
		if( name == -1)
		{
			return all;
		}
		int startarray = inJsonArray.indexOf("[",name);
		//int arrayopen = inJsonArray.indexOf("]",startarray);
		String arraydata = inJsonArray;//inJsonArray.substring(startarray,endarray);
		int objectindex = arraydata.indexOf("{",startarray);
		while(objectindex > -1)
		{
				//Count up the { and }
				int deep = 0;
				int start = objectindex;
				int end = 0;
				for (int i = objectindex; i < arraydata.length(); i++)
				{
					char c = arraydata.charAt(i);
					if(c == '{' )
					{
						deep++;
					}
					if(c == '}' )
					{
						deep--;
					}
					if( deep == 0)
					{
						end = i + 1;
						objectindex = arraydata.indexOf("{",end);
						if( objectindex != -1)
						{
							int endarray = arraydata.indexOf("]",end);
							if( endarray < objectindex) //the array ended and this is an invalid object so exit
							{
								objectindex = -1;
							}
						}
						break;
					}
				}
				String json = arraydata.substring(start, end);
				all.add(json);
		}
		return all;
		
	}
	public String findObject(String inName, String inJsonArray)
	{
		int name = inJsonArray.indexOf(inName);
		if( name == -1)
		{
			return null;
		}
		
		int objectindex = inJsonArray.indexOf("{",name);
		if(objectindex > -1)
		{
			//Count up the { and }
			int deep = 0;
			int start = objectindex;
			int end = 0;
			for (int i = objectindex; i < inJsonArray.length(); i++)
			{
				char c = inJsonArray.charAt(i);
				if(c == '{' )
				{
					deep++;
				}
				if(c == '}' )
				{
					deep--;
				}
				if( deep == 0)
				{
					end = i + 1;
					objectindex = inJsonArray.indexOf("{",end);
					break;
				}
			}
			String json = inJsonArray.substring(start, end);
			return json;
		}
		return null;
	}
	
	
}
