package org.entermediadb.asset.modules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.CompositeAsset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.find.EntityManager;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.BaseData;
import org.openedit.data.Searcher;
import org.openedit.data.ValuesMap;
import org.openedit.hittracker.HitTracker;
import org.openedit.util.PathUtilities;

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
	
	
	public void addAssetsToEntity(WebPageRequest inPageRequest) throws Exception 
	{
	
		MediaArchive archive = getMediaArchive(inPageRequest);
		EntityManager entityManager = getEntityManager(inPageRequest);
		
		String pickedmoduleid = inPageRequest.getRequestParameter("pickedmoduleid");
		String pickedentityid = inPageRequest.getRequestParameter("id");
		
		String pickedassetid = inPageRequest.getRequestParameter("pickedassetid");
		if(pickedassetid != null) {
			
			Asset asset;
			if (pickedassetid.startsWith("multiedit:"))
			{
				try
				{
					CompositeAsset assets = (CompositeAsset) inPageRequest.getSessionValue(pickedassetid);
					List tosave = new ArrayList();
					Integer count = 0;
					for (Iterator iterator = assets.iterator(); iterator.hasNext();)
					{
						asset = (Asset) iterator.next();
						if (asset == null) {
							log.error("No asset id passed in");
							return;
						}
						if(entityManager.addAssetToEntity(inPageRequest.getUser(), pickedmoduleid, pickedentityid, pickedassetid))
						{
							tosave.add(asset);
							if( tosave.size() > 100)
							{
								archive.getAssetSearcher().saveAllData(tosave, inPageRequest.getUser());
								tosave.clear();
							}
							count = count +1;
						}
					}
					if( tosave.size() > 0)
					{
						archive.getAssetSearcher().saveAllData(tosave, inPageRequest.getUser());
					}
					log.info("Added to entity: " + count + " assets.");
				}
				catch (Exception e)
				{
					//continue;
				}
			}
			else
			{
				
				asset = archive.getAsset(pickedassetid);
				if(entityManager.addAssetToEntity(inPageRequest.getUser(), pickedmoduleid, pickedentityid, asset.getId()))
				{
					archive.getAssetSearcher().saveData(asset);
					inPageRequest.putPageValue("asset", asset);
					inPageRequest.putPageValue("assets", "1");
				}
				
			}
		}
		else 
		{
			String assethitssessionid = inPageRequest.getRequestParameter("copyinghitssessionid");
			HitTracker assethits = (HitTracker) inPageRequest.getSessionValue(assethitssessionid);
			Integer added = entityManager.addAssetsToEntity(inPageRequest.getUser(), pickedmoduleid, pickedentityid, assethits);
			inPageRequest.putPageValue("assets", added);
		}
		
	}
	
	public void addtoNewEntity(WebPageRequest inPageRequest) throws Exception 
	{
		String saveaction = inPageRequest.getRequestParameter("saveaction");
		String copyingsearchtype = inPageRequest.getRequestParameter("copyingsearchtype");
		if(saveaction != null && "assets".equals(copyingsearchtype)) 
		{
			addAssetsToEntity(inPageRequest);
		}
		else if(saveaction != null && "category".equals(copyingsearchtype)) 
		{
			addCategoryToEntity(inPageRequest);
		}
	}
	
	public void addCategoryToEntity(WebPageRequest inPageRequest) throws Exception 
	{
		MediaArchive archive = getMediaArchive(inPageRequest);
		EntityManager entityManager = getEntityManager(inPageRequest);
		
		String pickedmoduleid = inPageRequest.getRequestParameter("pickedmoduleid");
		String pickedentityid = inPageRequest.getRequestParameter("id");
		
		String copyingcategoryid = inPageRequest.getRequestParameter("copyingcategoryid");
		if(copyingcategoryid != null) {
			if(entityManager.addCategoryToEntity(inPageRequest.getUser(), pickedmoduleid, pickedentityid, copyingcategoryid))
			{
				inPageRequest.putPageValue("categories", "1");
				//add assets
				
			}
		}
	}
	
	public void addToSearchCategory(WebPageRequest inPageRequest) throws Exception 
	{
	
		MediaArchive archive = getMediaArchive(inPageRequest);
		EntityManager entityManager = getEntityManager(inPageRequest);
		
		//String sourcemoduleid = inPageRequest.getRequestParameter("copyingsearchtype");
		String id = inPageRequest.getRequestParameter("id");
		String hitssessionid = inPageRequest.getRequestParameter("copyinghitssessionid");
		String sourcemoduleid = inPageRequest.getRequestParameter("copyingsearchtype");
		HitTracker hits = (HitTracker) inPageRequest.getSessionValue(hitssessionid);
		Integer added = entityManager.addToSearchCategory(inPageRequest, sourcemoduleid, hits, id);
		inPageRequest.putPageValue("saved", added);
	}
	
	public void copyEntities(WebPageRequest inPageRequest) throws Exception 
	{
		MediaArchive archive = getMediaArchive(inPageRequest);	
		EntityManager entityManager = getEntityManager(inPageRequest);
		
		String sourcemoduleid = inPageRequest.getRequestParameter("copyingsearchtype");
		String sourceentityid = inPageRequest.getRequestParameter("copyingentityid");
		String pickedmoduleid = inPageRequest.getRequestParameter("pickedmoduleid");
		String hitssessionid = inPageRequest.getRequestParameter("copyinghitssessionid");
		HitTracker hits = (HitTracker) inPageRequest.getSessionValue(hitssessionid);
		Integer added = 0;
		if(hits == null) {
			added = entityManager.copyEntities(inPageRequest, sourcemoduleid, pickedmoduleid, sourceentityid);
		}
		else {
			added = entityManager.copyEntities(inPageRequest, sourcemoduleid, pickedmoduleid, hits);
		}

		inPageRequest.putPageValue("saved", added);
		inPageRequest.putPageValue("moduleid", sourcemoduleid);
		
		
		String action = inPageRequest.getRequestParameter("action");
		
		if("moveentity".equals(action)) {
			Boolean deleted = entityManager.deleteEntity(inPageRequest, sourcemoduleid, sourceentityid);
			inPageRequest.putPageValue("moduleid", pickedmoduleid);
			inPageRequest.putPageValue("deleted", deleted);
		}
		
	}
	
	public void removeFromEntity(WebPageRequest inPageRequest) throws Exception 
	{
	
		MediaArchive archive = getMediaArchive(inPageRequest);
		EntityManager entityManager = getEntityManager(inPageRequest);
		
		String moduleid = inPageRequest.getRequestParameter("moduleid");
		String entityid = inPageRequest.getRequestParameter("entityid");
		
		String assetid = inPageRequest.getRequestParameter("assetid");
		String appid = inPageRequest.getRequestParameter("applicationid");
		Data entity = archive.getCachedData(moduleid,entityid);
		if(assetid != null) {
			if(entityManager.removeAssetToEntity(inPageRequest.getUser(), moduleid, entityid, assetid))
			{
				inPageRequest.putPageValue("assets", "1");
				archive.getEntityManager().fireAssetRemovedFromEntity(appid, inPageRequest.getUser(), assetid, entity);
			}
		}
		else 
		{
			String assethitssessionid = inPageRequest.getRequestParameter("copyinghitssessionid");
			HitTracker assethits = (HitTracker) inPageRequest.getSessionValue(assethitssessionid);
			Integer removed = entityManager.removeAssetsToEntity(inPageRequest.getUser(), moduleid, entityid, assethits);
			Collection<String> ids = assethits.getSelectedHitracker().collectValues("id");
			archive.getEntityManager().fireAssetsRemovedFromEntity(appid, inPageRequest.getUser(), ids , entity);

			inPageRequest.putPageValue("assets", removed);
		}
		inPageRequest.putPageValue("moduleid", moduleid);
		inPageRequest.putPageValue("entityid", entityid);
		
		inPageRequest.putPageValue("assetclearselection", true);
	}
	
	protected EntityManager getEntityManager(WebPageRequest inPageRequest) 
	{
		String catalogid = inPageRequest.findValue("catalogid");
		EntityManager entity = (EntityManager) getModuleManager().getBean(catalogid, "entityManager");
		return entity;
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
		if(entityid == null) {
			//get it from URL
			entityid = PathUtilities.extractDirectoryName(inPageRequest.getPath());
		}
		inPageRequest.putPageValue("selectedentityid", entityid);
		String moduleid = inPageRequest.getRequestParameter("entitytype");
		if(moduleid == null) {
			moduleid = inPageRequest.findValue("module");
		}
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
	
	
	public void handleNewAssetCreated(WebPageRequest inPageRequest) throws Exception
	{
		MediaArchive archive = getMediaArchive(inPageRequest);
		String appid = inPageRequest.findValue("applicationid");
		//Search the hits for category
		Collection<Asset> assets = (Collection<Asset>)inPageRequest.getPageValue("hits");
		
		for (Iterator iterator = assets.iterator(); iterator.hasNext();) 
		{
			Asset asset = (Asset) iterator.next();
			
			Collection<Data> entities = archive.getEntityManager().getEntitiesForCategories(asset.getCategories());
			for (Iterator iterator2 = entities.iterator(); iterator2.hasNext();) {
				//Asset asset2 = (Asset) iterator2.next();
				Data entity = (Data) iterator2.next();
				Collection currentassets = new ArrayList();
				currentassets.add(asset);
				archive.getEntityManager().fireAssetsAddedToEntity(appid, inPageRequest.getUser(), currentassets, entity);
			}
			
			
		}
		
	}
	public void handleAssetRemovedEvent(WebPageRequest inPageRequest) throws Exception
	{
		//Search the hits for category
	}
	
	public void scanForNewEntitiesNames(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);

		String moduleid = inReq.getRequestParameter("moduleid");

		String[] existingfoldernames = inReq.getRequestParameters("name");
		List all = Arrays.asList(existingfoldernames);
		HitTracker existing = archive.getSearcher(moduleid).query().orgroup("name",all).search();
		
		Set newfolders = new HashSet(all);
		for (Iterator iterator = existing.iterator(); iterator.hasNext();) {
			Data found = (Data) iterator.next();
			newfolders.remove(found.getName());
		}
		inReq.putPageValue("existingfolders",existing);
		inReq.putPageValue("newfolders",newfolders);
	}
	
	
	public void createEntitiesForFolders(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);

		String moduleid = inReq.getRequestParameter("moduleid");
		Data tmpdata = archive.getSearcher(moduleid).createNewData(); //tmp
		tmpdata.setValue("entitysourcetype", moduleid);

		String[] existingfoldernames = inReq.getRequestParameters("name");
		String[] fields = inReq.getRequestParameters("field");
		if (fields != null) {
			for (int i = 0; i < fields.length; i++) {
				String[] values = inReq.getRequestParameters(fields[i] + ".value");
				tmpdata.setValue(fields[i],values);
			}
		}
		List all = Arrays.asList(existingfoldernames);
		if(all.isEmpty()) {
			log.info("No folders selected");
			return;
		}
		HitTracker existing = archive.getSearcher(moduleid).query().orgroup("name",all).search();
		
		Set newfolders = new HashSet(all);
		for (Iterator iterator = existing.iterator(); iterator.hasNext();) {
			Data found = (Data) iterator.next();
			newfolders.remove(found.getName());
		}
		List tosave = new ArrayList();
		Searcher entitysearcher = archive.getSearcher(moduleid);
		for (Iterator iterator = newfolders.iterator(); iterator.hasNext();) {
			String name = (String) iterator.next();
			Data entity = ((BaseData)tmpdata).copy();
			entity.setName(name);
			archive.getEntityManager().createDefaultFolder(entity, inReq.getUser());
			
			tosave.add(entity);
			
		}
		entitysearcher.saveAllData(tosave, null);
		HitTracker existingconfirmed = archive.getSearcher(moduleid).query().orgroup("name",all).search();

		inReq.putPageValue("existingfolders",existingconfirmed);
	}

	
	/*
	public void createEntitiesForFolders(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);

		String moduleid = inReq.getRequestParameter("moduleid");
		Data tmpdata = archive.getSearcher(moduleid).createNewData(); //tmp
		tmpdata.setValue("entitysourcetype",moduleid);
		String[] existingfolders = inReq.getRequestParameters("name");
		String[] fields = inReq.getRequestParameters("field");
		for (int i = 0; i < fields.length; i++) {
			String[] values = inReq.getRequestParameters(fields + ".value");
			tmpdata.setValue(fields[i],values);
		}
		List tosave = new ArrayList();
		for (int i = 0; i < existingfolders.length; i++) {
			Data newfolder = ((BaseData)tmpdata).copy();
			tmpdata.setName(existingfolders[i]);
			archive.getEntityManager().loadDefaultFolder(tmpdata, inReq.getUser());  //This saves it
			tosave.add(newfolder);
			
		}
		archive.getSearcher(moduleid).saveAllData(tosave, null);
		inReq.putPageValue("existingfolders",tosave);
		
	}
	*/
}
