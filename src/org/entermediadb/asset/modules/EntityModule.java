package org.entermediadb.asset.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.data.ValuesMap;

public class EntityModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(EntityModule.class);

	public void addEntityToAsset(WebPageRequest inPageRequest) throws Exception 
	{
		
		String entityid = inPageRequest.getRequestParameter("entityid");
		String moduleid = inPageRequest.getRequestParameter("moduleid");
		MediaArchive archive = getMediaArchive(inPageRequest);
		if (entityid == null || moduleid == null) 
		{
			return;
		}
		
		String hitssessionid = inPageRequest.getRequestParameter("hitssessionid");
		boolean moveentity = Boolean.parseBoolean( inPageRequest.getRequestParameter("moveasset") );
			
		Asset asset = getAsset(inPageRequest);
		if (asset == null) {
			log.error("No asset id passed in");
			return;
		}

		if( moveentity )
		{
			//remove from the other entity
		}
		Boolean saved = false;
		//Use standard in CategoryEditModule?
		if (moduleid.equals("librarycollection")) {
			//rootcategoryid passed
			String rootcategory = inPageRequest.getRequestParameter("rootcategory");
			Category c = archive.getCategory(rootcategory);
			if (c != null) {
				asset.addCategory(c);
				saved = true;
			}
		}
		else if(moduleid.equals("faceprofilegroup")) {
			List<ValuesMap> otherprofiles = createListMap((Collection)asset.getValue("faceprofiles"));
			Boolean alreadyinprofile = false;
			for (int i = 0; i < otherprofiles.size(); i++) {
				ValuesMap profilegroups = (ValuesMap)otherprofiles.get(i);
				if( profilegroups.containsInValues("faceprofilegroup",entityid) ) {
					alreadyinprofile = true;
				}
			}
			if (!alreadyinprofile) {
				List<Map> profilemap = new ArrayList<Map>();
				Map profile = new HashMap();
				profile.put("faceprofilegroup", entityid);
				otherprofiles.add(new ValuesMap(profile));
				asset.setValue("faceprofiles", otherprofiles);
				saved = true;
			}
		}
		else {
			//Defualt entity
			//asset.addValue(moduleid, entityid);
			String categoryid = inPageRequest.getRequestParameter("categoryid");
			Category c = archive.getCategory(categoryid);
			if (c != null) {
				asset.addCategory(c);
				saved = true;
			}
			saved = true;
		}

		if (saved) {
			archive.saveAsset(asset, inPageRequest.getUser());
			//Assign primaryimage if not exists
			Data entity = archive.getData(moduleid, entityid);
			if (entity != null) {
				if (entity.get("primaryimage") == null) {
					entity.setValue("primaryimage", asset.getId());
					Searcher searcher = archive.getSearcher(moduleid);
					searcher.saveData(entity);
				}
			}
			archive.fireMediaEvent("saved", inPageRequest.getUser(), asset);
			inPageRequest.putPageValue("added" , "1");
		}
	}
	
	private List<ValuesMap> createListMap(Collection inValues)
	{
		ArrayList copy = new ArrayList();
		if (inValues != null) 
		{
			for (Iterator iterator = inValues.iterator(); iterator.hasNext();)
			{
				Map map = (Map) iterator.next();
				copy.add(new ValuesMap(map));
			}
		}
		return copy;
	}
	
	public void makePrimaryImageEntity(WebPageRequest inPageRequest) throws Exception 
	{
		
		String entityid = inPageRequest.getRequestParameter("entityid");
		String moduleid = inPageRequest.getRequestParameter("moduleid");
		String assetid = inPageRequest.getRequestParameter("assetid");
		MediaArchive archive = getMediaArchive(inPageRequest);
		if (entityid == null || moduleid == null || assetid == null) 
		{
			return;
		}
		
		Data entity = archive.getData(moduleid, entityid);
		if (entity != null) {
				entity.setValue("primaryimage", assetid);
				Searcher searcher = archive.getSearcher(moduleid);
				searcher.saveData(entity);
		}
	}

	public Data loadSelectedEntity(WebPageRequest inPageRequest) throws Exception 
	{
		
		String entityid = inPageRequest.getRequestParameter("entityid");
		String moduleid = inPageRequest.getRequestParameter("entitytype");
		if( entityid == null || moduleid == null)
		{
			return null;
		}
		MediaArchive archive = getMediaArchive(inPageRequest);
		Data entity = archive.getCachedData(moduleid, entityid);
		
		inPageRequest.putPageValue("selectedentity", entity);
		//inPageRequest.putPageValue(entityid+"sortby", entity);
		
		
		return entity;
	}
}
