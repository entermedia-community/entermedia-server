package org.entermediadb.asset.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.util.DateStorageUtil;

public class JsonUtil
{
	public String formatDateObj(Object inDate)
	{
		if( inDate == null)
		{
			return "";
		}
		inDate = DateStorageUtil.getStorageUtil().parseFromObject(inDate);
		//https://mincong.io/2017/02/16/convert-date-to-string-in-java/
		String json = DateStorageUtil.getStorageUtil().formatDateObj((Date)inDate, "yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
		//String json = DateStorageUtil.getStorageUtil().formatDateObj((Date)inDate, "yyyy-MM-dd'T'HH:mm:ss.SSSX");
		
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
		//setEscapeForwardSlashAlways
		escape = escape.replaceAll("\\/","/");
		
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

	public HitTracker searchByJson(Searcher inSearcher, WebPageRequest inReq)
	{
		SearchQuery squery = parseJson(inSearcher, inReq);
		
		HitTracker hits = inSearcher.cachedSearch(inReq, squery);
		
		Map request = inReq.getJsonRequest();

		String hitsperpage = (String)request.get("hitsperpage");
		
		if (hitsperpage != null)
		{
			int pagesnum = Integer.parseInt(hitsperpage);
			hits.setHitsPerPage(pagesnum);
		}
		
		String page = (String)request.get("page");
		
		if(page != null)
		{
			int pagenumb = Integer.parseInt(page);
			hits.setPage(pagenumb);
		}
		
		if( "true".equals( request.get("showfilters") ) )
		{
			Map nodes = hits.getActiveFilterValues();
			if( nodes != null)
			{
				inReq.putPageValue("filteroptions", nodes.values());
			}
		}
		
		return hits;
	}
		
	public SearchQuery parseJson(Searcher inSearcher, WebPageRequest inReq)
	{
		ArrayList <String> fields = new ArrayList();
		ArrayList <String> operations = new ArrayList();
		
		Map request = inReq.getJsonRequest();
		
		Map query = (Map)request.get("query");
		Collection terms = (Collection)query.get("terms");
		
		for (Iterator iterator = terms.iterator(); iterator.hasNext();)
		{
			Map it = (Map)iterator.next();
			fields.add((String)it.get("field"));
			String opr = (String)it.get("operation");
			if( opr == null)
			{
				opr = (String)it.get("operator");  //legacy
			}
			operations.add(opr.toLowerCase());
			Collection values = (Collection)it.get("values");
			if( values != null)
			{
				String[] svalues = (String[])values.toArray(new String[values.size()]);
				inReq.setRequestParameter(it.get("field")+ ".values", svalues);
			}
			else if( it.get("value") != null)
			{
				inReq.setRequestParameter(it.get("field") + ".value", (String)it.get("value"));
			}
			
			// handle all other options here...
			if(it.get("before") != null) {
				inReq.setRequestParameter(it.get("field") + ".before", (String)it.get("before"));

			}
			if(it.get("after") != null) {
				inReq.setRequestParameter(it.get("field") + ".after", (String)it.get("after"));

			}
			
			if(it.get("highval") != null) {
				inReq.setRequestParameter(it.get("field") + ".highval", (String)it.get("highval"));

			}
			
			if(it.get("lowval") != null) {
				inReq.setRequestParameter(it.get("field") + ".lowval", (String)it.get("lowval"));

			}
		//	String highval = inPageRequest.getRequestParameter(field.getId() + ".highval");
	//		String lowval = inPageRequest.getRequestParameter(field.getId() + ".lowval");
			
//			String[] beforeStrings = inPageRequest.getRequestParameters(field.getId() + ".before");
//			String[] afterStrings = inPageRequest.getRequestParameters(field.getId() + ".after");

		}

		String[] fieldarray = fields.toArray(new String[fields.size()]);
		String[] opsarray = operations.toArray(new String[operations.size()]);

		inReq.setRequestParameter("field", fieldarray);
		inReq.setRequestParameter("operation", opsarray);

		SearchQuery squery = inSearcher.addStandardSearchTerms(inReq);
		return squery;
	}

	public String toJson(Map inMap)
	{
		if( inMap == null )
		{
			return "{}"; 
		}
		JSONObject obj = new JSONObject(inMap);
		return obj.toJSONString();
	}
	public String toJsonArray(Collection inMap)
	{
		if( inMap == null )
		{
			return "[]"; 
		}
		JSONArray obj = new JSONArray();
		obj.addAll(inMap);
		return obj.toJSONString();
	}
	
}
