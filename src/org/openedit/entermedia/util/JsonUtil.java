package org.openedit.entermedia.util;

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
	
	
}
