package org.openedit.entermedia.util;

import org.json.simple.JSONObject;

public class JsonUtil
{

	public String escape(String inVal){
		String escape = JSONObject.escape(inVal);
		return escape;
	}
	
	
}
