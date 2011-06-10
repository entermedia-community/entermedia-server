package org.openedit.entermedia.util;

import java.util.Map;


public class Replacer
{
	public String replace(String inCode, Map<String, String> inValues)
	{
		if( inCode == null)
		{
			return inCode;
		}
		int start = 0;
		while( (start = inCode.indexOf("${",start)) != -1)
		{
			int end = inCode.indexOf("}",start);
			if( end != -1)
			{
				String key = inCode.substring(start+2,end);
				Object variable = inValues.get(key); //check for property
				
				if( variable != null)
				{
					String sub = variable.toString();
					sub = replace(sub,inValues);
					inCode = inCode.substring(0,start) + sub + inCode.substring(end+1);
					if(sub.length() <= end){
						start = end-sub.length();
					}else{
						start =  sub.length();
					}
				}else{
					start = end;
				}
			}
		
			
		}
		return inCode;
	}
}
