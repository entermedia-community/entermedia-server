package org.entermediadb.find;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.sum.SumBuilder;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.projects.LibraryCollection;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.cache.CacheManager;
import org.openedit.data.DataWithSearcher;
import org.openedit.data.PropertyDetail;
import org.openedit.data.QueryBuilder;
import org.openedit.data.Searcher;
import org.openedit.hittracker.FilterNode;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.ListHitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.profile.UserProfile;
import org.openedit.users.User;

public class EntityManager implements CatalogEnabled
{
	private static final Log log = LogFactory.getLog(EntityManager.class);
	protected String fieldCatalogId;
	protected ModuleManager fieldModuleManager;
	protected CacheManager fieldCacheManager;
	
	public CacheManager getCacheManager()
	{
		return fieldCacheManager;
	}

	public void setCacheManager(CacheManager inCacheManager)
	{
		fieldCacheManager = inCacheManager;
	}

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	public String getCatalogId()
	{
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}

	public Collection loadCategories(String inModule,Data entity, User inUser ) {
		Data module = getMediaArchive().getCachedData("module", inModule);
		Collection categories = loadCategories(module, entity, inUser );
		return categories;
	}
	
	
	public Collection loadCategories(Data inModule,Data entity, User inUser )
	{
		String inEntityType = inModule.getId();
		
		Collection categories = (Collection)getCacheManager().get("searchercategory" , inEntityType + "/ " + entity.getId());
		if( categories == null )
		{
			loadDefaultFolder(inModule, entity, inUser);
			categories = getMediaArchive().query("category").exact(inEntityType, entity.getId()).sort("categorypath").search();
			getCacheManager().put("searchercategory", inEntityType + "/" + entity.getId(),categories);
			
		}
		return categories;
	}

	protected MediaArchive getMediaArchive()
	{
		return (MediaArchive)getModuleManager().getBean(getCatalogId(), "mediaArchive", true);
	}
	public Category loadDefaultFolderForModule(Data module, User inUser)
	{
		if( module == null)
		{
			return null;
		}
		String mask = (String) module.getValue("autocreatestartingpath");
		if(mask == null)
		{
			mask = module.getName();
		}
		Category cat = getMediaArchive().getCategorySearcher().createCategoryPath(mask);
		return cat;

	}

	public Category createDefaultFolder(Data entity,  User inUser)
	{
		if( entity == null)
		{
			return null;
		}
		String type = entity.get("entitysourcetype");
		Data module = getMediaArchive().getCachedData("module", type);
		if( module == null)
		{
			return null;
		}
		Category cat = loadDefaultFolder(module, entity, inUser, true);
		return cat;		
	}
	public Category loadDefaultFolder(Data entity, User inUser)
	{
		if( entity == null)
		{
			return null;
		}
		String type = entity.get("entitysourcetype");
		Data module = getMediaArchive().getCachedData("module", type);
		if( module == null)
		{
			return null;
		}
		Category cat = loadDefaultFolder(module, entity,inUser);
		return cat;
	}
	public Category loadDefaultFolder(Data module, Data entity, User inUser)
	{
		if( module == null || entity == null || entity.getId() == null)
		{
			return null;
		}
		
		boolean createcat = true;
		
		Object val =  module.getValue("enableuploading");
		if(val == null || !Boolean.parseBoolean(val.toString()))
		{
			createcat = false;
		}
		
		Category cat = loadDefaultFolder(module, entity,inUser,createcat);
		return cat;
		
	}
	
	
	
	
	public Category loadDefaultFolder(Data module, Data entity, User inUser, boolean create)
	{
		//Ignore create
		
		if( entity == null || module == null || entity.getName() == null)
		{
			//log.error("No entity found entity:" + entity + " in module:" + module );
			//Invalid names?
			return null;
		}
		Category cat = null;
		String existingsourcepath = entity.get("uploadsourcepath");
		if( existingsourcepath != null ) //Dont use rootcategory if source is blank
		{
			String categoryid = entity.get("rootcategory");
			if( categoryid != null)
			{
				cat = getMediaArchive().getCategory(categoryid);
				if( cat != null)
				{
					String categorypath = cat.getCategoryPath();
					if (existingsourcepath.endsWith("/"))
					{
						existingsourcepath = existingsourcepath.substring(0, existingsourcepath.length() - 1);
					}
					if( categorypath != null && !categorypath.equals( existingsourcepath ) )
					{
						throw new OpenEditException("Move Category to correct location: " + categorypath + "  " + existingsourcepath);
					}
				}
			}
		}	
		else if(entity instanceof LibraryCollection)
		{
			LibraryCollection collection = (LibraryCollection) entity;
			cat = collection.getCategory();
			if( cat != null)
			{
				existingsourcepath = cat.getCategoryPath();
				entity.setValue("uploadsourcepath",existingsourcepath);
				getMediaArchive().saveData(module.getId(), entity);
			}
			
		}
		if( cat == null)
		{
			String sourcepath = loadUploadSourcepath(module,entity,inUser,true);
			if( sourcepath == null)
			{
				return null;
			}
			cat = getMediaArchive().getCategorySearcher().createCategoryPath(sourcepath);
			if( cat != null )
			{
				boolean saveit = false;
				String existing = entity.get("rootcategory");
				if( existing == null || !existing.equals(entity.get("rootcategory") ) )
				{
					saveit = true;
				}
				entity.setValue("rootcategory",cat.getId());
				if( entity.getValue("uploadsourcepath") == null || !sourcepath.equals(entity.getValue("uploadsourcepath")) )
				{
					entity.setValue("uploadsourcepath",sourcepath);
					saveit = true;
				}
				if( saveit )
				{
					getMediaArchive().saveData(module.getId(), entity);
				}
			}

		}
		if( cat == null)
		{
			//Cant find sourcepathsaveData
			return null;
		}
		else
		{
			boolean updatename = false;
			if( !entity.getName().equals(cat.getName()))
			{
				if (module.get("uploadsourcepath") == null)
				{
					updatename = true;
				}
				else if(module.get("uploadsourcepath").contains("${data.name}"))
				{
					updatename = true;
				}
				
				
				if (updatename)
				{
					String enpath = entity.get("uploadsourcepath");
					log.info("Category was renamed " + cat.getName() + " -> " + entity.getName());
					cat.setName(entity.getName());
					cat.setValue("categorypath",null); //clear it
					//TODO: How can I move all the old content over?
					//save all the childrem
					getMediaArchive().getCategorySearcher().saveCategoryTree(cat);
					if(enpath == null || !cat.getCategoryPath().equals(enpath) )
					{
						entity.setValue("uploadsourcepath",cat.getCategoryPath());
						getMediaArchive().saveData(module.getId(), entity);
					}
				}
			}
		}
		
//		if( cat.getValue(module.getId()) == null)
//		{
//			cat.setValue(module.getId(),entity.getId()); //Is this smart?
//			getMediaArchive().getCategorySearcher().saveData(cat);
//		}
		return cat;
	}	
	public String loadUploadSourcepath(Data module, Data entity, User inUser, boolean inCreate)
	{
		return loadUploadSourcepath(module, entity, inUser);
	}
	public String loadUploadSourcepath(Data module, Data entity, User inUser)
	{
		if (entity == null ) {
			return null;
		}
		String sourcepath = entity.get("uploadsourcepath");
		if( sourcepath != null)
		{
			return sourcepath;
		}
		
		String mask = (String) module.getValue("uploadsourcepath");
		
		if(mask != null)
		{
			Map values = new HashedMap();
			
			values.put("module", module);
			
			DataWithSearcher smartdata = new DataWithSearcher(getMediaArchive().getSearcherManager(), getCatalogId(), module.getId(), entity);
			values.put(module.getId(), smartdata);
			values.put("data", smartdata);
			
			sourcepath = getMediaArchive().replaceFromMask( mask, entity, module.getId(), values, null);  //Static locale?
			sourcepath = sourcepath.replace("//", "/");
		}
		if( sourcepath == null && entity.getName("en") != null)
		{
			sourcepath = module.getName("en") + "/" + entity.getName("en") + "/";
		}
		if( sourcepath != null && !sourcepath.isEmpty() && !sourcepath.equals( entity.get("uploadsourcepath")) )
		{
			entity.setValue("uploadsourcepath",sourcepath );
			getMediaArchive().saveData(module.getId(), entity);
		}
		
		if( sourcepath != null && sourcepath.isEmpty())
		{
			return null;
		}
		return sourcepath;
	}	

	public Collection loadChildren(String inEntityParentType, String inParentEntityId, String inChildEntityType)
	{
		String cacheid = inEntityParentType + "/" + inParentEntityId + "/" + inChildEntityType;
		Collection entities = (Collection)getCacheManager().get("entitymanager", cacheid);
		if( entities == null)
		{
			entities = getMediaArchive().query(inChildEntityType).exact(inEntityParentType, inParentEntityId).sort("name").search();
			getCacheManager().put("entitymanager", cacheid,entities);
		}
		return entities;
	}

	public Map listTotalSize(String inCategoryId, WebPageRequest inContext)
	{
		SearchQuery query = getMediaArchive().query("asset").named("sizecheck").exact("category", inCategoryId).getQuery();
		AggregationBuilder b = AggregationBuilders.terms("assettype_filesize").field("assettype");
		SumBuilder sum = new SumBuilder("assettype_sum");
		sum.field("filesize");
		b.subAggregation(sum);
		query.setAggregation(b);

//		#foreach($item in
//		$breakdownhits.getAggregations().get("assettype_filesize").getBuckets())
//		#foreach($subitem in $item.getAggregations())
//		<li class="list-group-item"><span class="badge"
//			title="$item.key">$!sizer.inEnglish($subitem.getValue()) </span>
//			#set( $data = false)
//			#set( $data = $mediaarchive.getData("assettype",$item.key))
//			$context.getText($data)
//			<br /></li> 
//		#end
//		#end
//
		HitTracker hits =
				getMediaArchive().getSearcher("asset").cachedSearch(inContext,query);
		//log.info("query:" + query.hasFilters());
		hits.enableBulkOperations();
		hits.getActiveFilterValues();
			//StringTerms agginfo = hits.getAggregations().get("assettype_filesize");
			//context.putPageValue("breakdownhits", hits)

//		<li class="list-group-item"><span class="badge">$!sizer.inEnglish($breakdownhits.getSum("filesize"))
		Map values = new HashMap();
		values.put("hits", hits);
		double size= hits.getSum("assettype_filesize","assettype_sum");
		values.put("filesize", size);
		return values;
	}

	public Integer addAssetsToEntity(User inUser,String pickedmoduleid, String pickedentityid, HitTracker hits) 
	{
		Data module = getMediaArchive().getCachedData("module", pickedmoduleid);
		Data entity = getMediaArchive().getCachedData(pickedmoduleid,pickedentityid);
		if(entity == null) {
			return 0;
		}
		Category category = loadDefaultFolder(module, entity, inUser, true);
		
		List<Asset> tosave = new ArrayList();
		if(hits != null && hits.getSelectedHitracker() != null && module != null && entity != null && category != null) {
			
			for (Iterator iterator = hits.getSelectedHitracker().iterator(); iterator.hasNext();) {
				Data hit = (Data) iterator.next();
				Asset asset = (Asset)getMediaArchive().getAssetSearcher().loadData(hit);
				asset.addCategory(category);
				tosave.add(asset);
			}
			getMediaArchive().saveAssets(tosave);
		}
		//deSelect assets after copy
		hits.getSelectedHitracker().deselectAll();
		
		getMediaArchive().getAssetManager().createLinksTo(tosave,category.getCategoryPath());

		//Use Category events?
		//fireAssetsAddedToEntity(null, inUser, tosave , entity);

		return tosave.size();
	}
	
	public Boolean addAssetToEntity(User inUser,String pickedmoduleid, String pickedentityid, Asset asset)
	{
		Data module = getMediaArchive().getCachedData("module", pickedmoduleid);
		Data entity =getMediaArchive().getCachedData(pickedmoduleid,pickedentityid);
		Category category = loadDefaultFolder(module, entity, inUser, true);
		
		return addAssetToEntity(inUser, module, entity, asset, category);
	}
	
	public Boolean addAssetToEntity(User inUser,Data module, Data entity, Asset asset, Category destinationCategory) 
	{
		
		if(destinationCategory != null)
		{
			asset.addCategory(destinationCategory);
		}
		getMediaArchive().saveAsset(asset); 
		Collection tosave = new ArrayList();
		tosave.add(asset);
		getMediaArchive().getAssetManager().createLinksTo(tosave,destinationCategory.getCategoryPath());

		return true;
	}
	public Integer removeAssetsFromEntity(User inUser,String pickedmoduleid, String pickedentityid, HitTracker hits) 
	{
		Data module = getMediaArchive().getCachedData("module", pickedmoduleid);
		Data entity =getMediaArchive().getCachedData(pickedmoduleid,pickedentityid);
		if(entity == null) {
			return 0;
		}
		Category category = loadDefaultFolder(module, entity, inUser, true);
		
		List tosave = new ArrayList();
		if(hits != null && hits.getSelectedHitracker() != null && module != null && entity != null && category != null) {
			
			for (Iterator iterator = hits.getSelectedHitracker().iterator(); iterator.hasNext();) {
				Data hit = (Data) iterator.next();
				Asset asset = (Asset)getMediaArchive().getAssetSearcher().loadData(hit);
				asset.removeCategory(category);
				
				//Make sure its totally removed from any child categories
				Set tokeep = new HashSet();
				for (Iterator iterator2 = asset.getCategories().iterator(); iterator2.hasNext();) {
					Category exact = (Category) iterator2.next();
					if( !category.hasCatalog(exact.getId()))
					{
						tokeep.add(exact);
					}
				};
				asset.setCategories(tokeep);
				
				tosave.add(asset);
			}
			
			getMediaArchive().saveAssets(tosave);
		}
		
		fireAssetsRemovedFromEntity(null, inUser, tosave , entity);
		hits.deselectAll();
		return tosave.size();
		

	}
	
	public Boolean removeAssetToEntity(User inUser,String pickedmoduleid, String pickedentityid, String assetid) 
	{
		Data module = getMediaArchive().getCachedData("module", pickedmoduleid);
		Data entity =getMediaArchive().getCachedData(pickedmoduleid,pickedentityid);
		Category category = loadDefaultFolder(module, entity, inUser, true);

		Asset asset = (Asset)getMediaArchive().getAsset(assetid);
		if(category != null)
		{
			asset.removeCategory(category);
		}
		getMediaArchive().saveAsset(asset);
		
		fireAssetRemovedFromEntity(null, inUser, asset, entity);
		return true;

	}
	
	public Boolean addCategoryToEntity(User inUser,String pickedmoduleid, String pickedentityid, String categoryid) 
	{
		Data module = getMediaArchive().getCachedData("module", pickedmoduleid);
		Data entity =getMediaArchive().getCachedData(pickedmoduleid,pickedentityid);
		
		Category rootcategory = loadDefaultFolder(module, entity, inUser, true);
		
		return addCategoryToEntity(inUser,module,entity,categoryid,rootcategory);
	}
	
	public Boolean addCategoryToEntity(User inUser,Data module, Data entity, String categoryid, Category destinationCategory) 
	{
		
		Category copyingcategory =  getMediaArchive().getCategory(categoryid);
		
		if(copyingcategory != null)
		{
			Category existing =	destinationCategory.getChildByName(copyingcategory.getName());
			if( existing == null)
			{
				Searcher categorysearcher = getMediaArchive().getSearcher("category");
				existing = (Category) categorysearcher.createNewData();
				existing.setName(copyingcategory.getName());
				destinationCategory.addChild(existing);
				categorysearcher.saveData(existing);
			}
			
			String[] catids = new String [] {categoryid};
			getMediaArchive().getCategoryEditor().copyEverything(inUser,catids, existing.getId());
		}
		
		return true;
	}
	
	public Data copyEntity(WebPageRequest inContext, String sourcemoduleid, String pickedmoduleid, Data source) 
	{
		
		Searcher entitysearcher = getMediaArchive().getSearcher(pickedmoduleid);
		Data newchild = entitysearcher.createNewData();
		

		for (Iterator iterator = source.getProperties().keySet().iterator(); iterator.hasNext();) {
			String key = (String) iterator.next();
			if(key.equals("rootcategory") || key.equals("uploadsourcepath")) {
				continue;
			}
			Object val = source.getValue(key);
			newchild.setValue(key, val);
		}
		
		String customname = inContext.getRequestParameter("nameoverwrite");
		String name = customname;
		String nameoriginal = source.getName();
		if(name == null) 
		{
			name = nameoriginal;
		}
		String action = inContext.getRequestParameter("action");
		if(!"moveentity".equals(action)) {
			//It is a copy, if copying  to same Module add  (copy)
			if(pickedmoduleid.equals(sourcemoduleid)) {
				if(name.equals(nameoriginal)) {
					name = nameoriginal + " (copy)";
				}
			}
		}
		
		newchild.setId(null);
		newchild.setName(name);
		newchild.setValue("entitysourcetype", pickedmoduleid);
		
		Category targetcategory = createDefaultFolder(newchild, inContext.getUser());
		Category sourcecategory = getMediaArchive().getCategory(source.get("rootcategory"));
		if(sourcecategory == null)
		{
			log.info("No source category to copy " + source);
		}
		else
		{
			getMediaArchive().getCategoryEditor().copyEverything(inContext.getUser(), sourcecategory, targetcategory);
		}
		newchild.setValue("uploadsourcepath", targetcategory.getCategoryPath());
		
		//Make a path
		String parenttype = source.get("entitysourcetype");
		PropertyDetail detail = entitysearcher.getDetail(parenttype);
		if( detail != null)
		{
			newchild.setValue(parenttype, source.getId() );
		}
		return newchild;
	}
	
	
	
	public Collection copyEntities(WebPageRequest inContext, String sourcemoduleid, String pickedmoduleid, HitTracker hits) 
	{
		//Data module = getMediaArchive().getCachedData("module", pickedmoduleid);
		List tosave = new ArrayList();
		if(hits != null && hits.getSelectedHitracker() != null) {
			for (Iterator iterator = hits.getSelectedHitracker().iterator(); iterator.hasNext();) {
				Data hit = (Data) iterator.next();
				Data newchild = copyEntity(inContext, sourcemoduleid, pickedmoduleid, hit);
				if(newchild != null) {
					tosave.add(newchild);
				}
			}
			getMediaArchive().saveData(pickedmoduleid, tosave);
		}
				
		return tosave;
	}
	
	public Data copyEntity(WebPageRequest inContext, String sourcemoduleid, String pickedmoduleid, String sourceentityid) 
	{
		Data source = getMediaArchive().getData(sourcemoduleid, sourceentityid);
		if(source != null) {
			Data newchild = copyEntity(inContext, sourcemoduleid, pickedmoduleid, source);
			if(newchild != null) {
				getMediaArchive().saveData(pickedmoduleid, newchild);
				inContext.putPageValue("newentity", newchild);
				return newchild;
			}
		}
		return null;
	}
	
	public Integer addToSearchCategory(WebPageRequest inContext, String entitymoduleid, HitTracker hits, String searchcategoryid) 
	{
		List tosave = new ArrayList();
		if(hits != null && hits.hasSelections()) {
			for (Iterator iterator = hits.getSelectedHitracker().iterator(); iterator.hasNext();) {
				MultiValued hit = (MultiValued) iterator.next();
				hit.addValue("searchcategory", searchcategoryid);
				tosave.add(hit);
			}
			getMediaArchive().saveData(entitymoduleid, tosave);
		}
		else
		{
			//selections missing
			log.error("Sections missing");
		}
				
		return tosave.size();
	}
	
	
	public Boolean deleteEntity(WebPageRequest inContext, String moduleid, String entityid) {
		Searcher entitysearcher = getMediaArchive().getSearcher(moduleid);
		Data data = (Data) entitysearcher.searchById(entityid);
		if(data != null) {
			entitysearcher.delete(data, inContext.getUser());
			return true;
		}
		return false;
	}
	
	public Collection<Data> getEntitiesForCategories( WebPageRequest inReq, Collection<Category> inParentCategories)
	{
		if (inParentCategories == null) {
			return null;
		}
		
		Collection<Data> categories = new HashSet();
		
		
		for (Iterator iterator1 = inParentCategories.iterator(); iterator1.hasNext();) 
		{
			Category cat = (Category) iterator1.next();
			categories.addAll(cat.listAncestorsAndSelf(1));
		}
		
		User user = inReq.getUser();
		UserProfile userprofile = inReq.getUserProfile(); //empty userprofile?
		if(userprofile == null)
		{
			userprofile = getMediaArchive().getProfileManager().getUserProfile(getMediaArchive().getCatalogId(), user.getUserName());
		}
		
		Collection allowed = userprofile.getEntitiesIds();
		allowed.remove("asset");
		
//always returns emtpy
		HitTracker found =  getMediaArchive().query("modulesearch").named("modulsearchcathits").orgroup("rootcategory", categories).put("searchtypes", allowed).search(inReq);
		
		List<Data> finallist = new ArrayList();
		if (found != null)
		{
			for (Iterator iterator = found.iterator(); iterator.hasNext();) {
				Data entity = (Data)iterator.next();
				String moduleid = entity.get("entitysourcetype");
				if( moduleid == null || allowed.contains(moduleid))
				{
					finallist.add(entity);
				}
			}
		}
		return finallist;
	}	
	
	
	public Collection loadHistoryForEntity(String applicationid, User inUser, Data inModule, Data inEntity) {
		
		Collection history = getMediaArchive().query("entityactivityhistory").exact("entityid", inEntity.getId()).sort("dateDown").search();
		
		if(history.isEmpty() && inEntity.get("rootcategory") != null) {
			HitTracker hits = getMediaArchive().query("asset").named("sizecheck").exact("category", inEntity.get("rootcategory") ).search();
			if(!hits.isEmpty()) {
				saveAssetActivity(applicationid, inUser, inEntity, hits, "assetsadded"); 
				history = getMediaArchive().query("entityactivityhistory").exact("entityid", inEntity.getId()).sort("dateDown").search();
			}
		}
		return history;

	}
	
	

	public void fireAssetAddedToEntity(String applicationid, User inUser, Data inAsset, Data entity)
	{
		Collection<Data> assets = new ArrayList(1);
		assets.add(inAsset);
		saveAssetActivity(applicationid, inUser, entity, assets, "assetsadded");
	}
	
	public void fireAssetsAddedToEntity(String applicationid, User inUser, Collection<Data> inAssets, Data entity)
	{
		saveAssetActivity(applicationid, inUser, entity, inAssets, "assetsadded");
	}

	public void fireAssetRemovedFromEntity(String applicationid, User inUser, Data inAsset, Data entity)
	{
		Collection<Data> assets = new ArrayList(1);
		assets.add(inAsset);
		saveAssetActivity(applicationid, inUser, entity, assets, "assetsremoved");

	}

	public void fireAssetsRemovedFromEntity(String applicationid, User inUser, Collection<Data> inAssets, Data entity)
	{
		saveAssetActivity(applicationid, inUser, entity, inAssets, "assetsremoved");
	}
	
	
	public void saveAssetActivity(String applicationid,  User inUser, Data entity, Collection<Data> inAssets, String inOperation) {
		Searcher searcher = getMediaArchive().getSearcher("entityactivityhistory");
		Data event = searcher.createNewData();
		event.setProperty("applicationid", applicationid);
		if( inUser != null)
		{
			event.setProperty("user", inUser.getId());
		}

		event.setProperty("operation", inOperation);
		event.setProperty("moduleid", entity.get("entitysourcetype"));
		event.setProperty("entityid", entity.getId()); //data.getId() ??
		event.setValue("assetids", inAssets);
		Collection names = new ArrayList();
		for (Iterator iterator = inAssets.iterator(); iterator.hasNext();) {
			Data asset = (Data) iterator.next();
			names.add(asset.getName());
		}
		event.setValue("assetnames", names);
		event.setValue("date", new Date()); 
		
		searcher.saveData(event, null);
	}

	//Add API to restore deleted assets
	
	public Map loadWorkStatusForPage(User inUser, String inModuleid, String inEntityid, HitTracker inAssethits)
	{
		Searcher searcher = getMediaArchive().getSearcher("entityassetworkflow");
		
		List page = inAssethits.getPageOfHits();
		ListHitTracker tracker = new ListHitTracker(page);
		tracker.setHitsPerPage(inAssethits.getHitsPerPage());
		Collection ids = tracker.collectValues("id");
		
		Collection existing = searcher.query().orgroup("primarymedia", ids).exact("parentmoduleid",inModuleid).exact("parententityid",inEntityid).search();

		Map<String,Set> assetstatuses = new HashMap();
		for (Iterator iterator = existing.iterator(); iterator.hasNext();) 
		{
			Data data = (Data) iterator.next();
			String assetid = data.get("primarymedia");
			Set<String> statuses = assetstatuses.get(assetid);
			if( statuses == null)
			{
				statuses = new HashSet();
			}
			String workflowstatus = data.get("workflowstatus");
			statuses.add(workflowstatus);
			assetstatuses.put(assetid,statuses);
		}
		return assetstatuses;
	}	
	
	//Not used?
	public int addToWorkflowStatus(User inUser, String inModuleid, String inEntityid, HitTracker inAssethits, String lightboxid)
	{
		if( lightboxid == null)
		{
			log.error("No box selected");
			return 0;
		}
		Collection assetstoadd =  null;
		
		if(!inAssethits.getSearchType().equals("asset")) {
			throw new OpenEditException("Noting to add. Wrong Searchtype");
		}
		assetstoadd =  inAssethits.collectValues("id");
		if( assetstoadd.isEmpty())
		{
			throw new OpenEditException("Noting to add");
		}
		Searcher searcher = getMediaArchive().getSearcher("emedialightboxasset");
		HitTracker existing = searcher.query().orgroup("primarymedia", assetstoadd).exact("parentmoduleid",inModuleid)
				.exact("parententityid",inEntityid).exact("lightboxid", lightboxid).search();

		Set alreadyadded =  new HashSet( existing.collectValues("primarymedia"));
//		Map<String,Data> byassets = new HashMap();
//		for (Iterator iterator = existing.iterator(); iterator.hasNext();) 
//		{
//			Data data = (Data) iterator.next();
//			byassets.put(data.get("primarymedia"),data);
//		}
//		
		List tosave = new ArrayList();
		
		//TODO: Check for existing workflows
		//Add more
		long count = 0;//System.currentTimeMillis();
		
		for (Iterator iterator = inAssethits.iterator(); iterator.hasNext();) 
		{
			Data data = (Data) iterator.next();
			String assetid = null;
			if(inAssethits.getSearchType().equals("asset")) {
				assetid =  data.getId();
			}
			else if(inAssethits.getSearchType().equals("emedialightboxasset")) {
				assetid =  data.get("primarymedia");
			}
			//Look for existing?
			if( !alreadyadded.contains(assetid) )
			{
				Data event = searcher.createNewData();
				event.setProperty("lightboxid", lightboxid);
				event.setProperty("parententityid", inEntityid);
				event.setProperty("parentmoduleid", inModuleid);
				event.setValue("name", data.getName());
				event.setValue("primarymedia", assetid);
				event.setValue("owner", inUser.getName());
				event.setValue("entity_date", new Date());
				count = count + 10000;
				event.setValue("ordering", count);
				tosave.add(event);
			}
		}
		getMediaArchive().saveData("emedialightboxasset", tosave);
		
		return tosave.size();
	}
/*
	public Data createLightBoxForEntity(String lightboxtypeid, Data inModule, Data inEntity, User inUser) {
		
		Data lightboxtype = getMediaArchive().getCachedData("emedialightboxtype", lightboxtypeid);
		
		Searcher searcher = getMediaArchive().getSearcher("emediaentitylightbox");
		Data lightbox = searcher.createNewData();
		lightbox.setValue("name", lightboxtype.getName());
		lightbox.setValue("moduleid", inModule.getId());
		lightbox.setValue("entityid", inEntity.getId());
		lightbox.setValue("lightboxtype", lightboxtypeid);
		Category entityrootcategory = createDefaultFolder(inEntity, inUser) ;

		Category lightboxcategory = (Category)getMediaArchive().getCategorySearcher().createCategoryPath(entityrootcategory.getCategoryPath() + lightboxtype.getName());

		lightbox.setValue("rootcategory", lightboxcategory.getId());
		lightbox.setValue("owner", inUser.getName());
		searcher.saveData(lightbox);
		return lightbox;
	}
	*/
	
	//Not used?
	
	public Data findLightBox(Collection<LightBox> inBoxes, String boxid)
	{
		for (Iterator iterator = inBoxes.iterator(); iterator.hasNext();)
		{
			Data lightBox = (Data) iterator.next();
			if( lightBox.getId().equals(boxid) )
			{
				return lightBox;
			}
		}
		return null;
	}

	//Not used?
	public Collection<LightBox> loadBoxesForModule(String inBoxModuleType, Data inEntityModule, Data inEntity,User inUser)
	{
		if( inEntityModule == null)
		{
			log.error("No module");
			return null;
		}
		if( inEntity == null)
		{
			log.error("No entity");
			return null;
		}
		//Search for all the boxes that match.
		if( inBoxModuleType == null)
		{
			return null;
		}

		Collection<LightBox> lighboxes = null;

		if( inBoxModuleType.equals("emedialightbox") )
		{		
			QueryBuilder query = getMediaArchive().query("emedialightbox").or().exact("showonall", true).exact("parentmoduleid", inEntityModule.getId()).sort("orderingUp");
			HitTracker boxes = getMediaArchive().getCachedSearch(query);
			lighboxes = new ArrayList(boxes.size());
			Category entityrootcategory = loadDefaultFolder(inEntityModule, inEntity, inUser) ;
			if( entityrootcategory == null)
			{
				log.error("No  root cat" + inEntity);
				return lighboxes;
			}
			for (Iterator iterator = boxes.iterator(); iterator.hasNext();)
			{
				Data box = (Data) iterator.next();
				LightBox lightbox = new LightBox();
				lightbox.setData(box);
				Category rootcategory = (Category)getMediaArchive().getCategorySearcher().createCategoryPathFromParent(entityrootcategory, box.getName());
				lightbox.setRootCategory(rootcategory);
				lighboxes.add(lightbox);
			}
		}
		else
		{
			Data boxmodule = getMediaArchive().getCachedData("module", inBoxModuleType);
			QueryBuilder query = getMediaArchive().query(inBoxModuleType).exact(inEntityModule.getId(), inEntity.getId());
			HitTracker boxes = getMediaArchive().getCachedSearch(query);
			
			//TODO: Optimize this with a cache based on boxes.getSearcher().getIndexId()
			lighboxes = new ArrayList(boxes.size());
			for (Iterator iterator = boxes.iterator(); iterator.hasNext();)
			{
				Data box = (Data) iterator.next();
				LightBox lightbox = new LightBox();
				lightbox.setData(box);
				Category rootcategory = loadDefaultFolder(boxmodule,box,inUser,true); //Make em now or later?
				if(rootcategory == null)
				{
					log.error("Could not create category for entity " + box);
					continue;
				}
				lightbox.setRootCategory(rootcategory);
				lighboxes.add(lightbox);
			}
		}
		setLightBoxCounts(lighboxes);
		
		return lighboxes;
	}

 
	
	public Collection<LightBox> loadLightBoxesForModule(Data inModule, Data inEntity,User inUser)
	{
		Collection<LightBox> boxes = loadBoxesForModule("emedialightbox",inModule, inEntity, inUser);
		return boxes;
	}
//	public HitTracker loadLightBoxesForEntity(Data inModule, Data inEntity,User inUser)
//	{
//		if( inModule == null)
//		{
//			log.error("No module");
//			return null;
//		}
//		//Search for all the boxes that match. 
//		HitTracker boxes = getMediaArchive().query("emediaentitylightbox")
//		.exact("moduleid", inModule.getId())
//		.exact("entityid", inEntity.getId())
//		.search();
//		return boxes;
//	}
	
	public Category loadLightboxCategory(Data inModule, Data inEntity, String inBoxTypeId, Data inSelectedBox, User inUser) {
		
		
		Category selectedcat = null;
		if( inBoxTypeId.equals("emedialightbox") )
		{
			Category entityrootcategory = loadDefaultFolder(inModule, inEntity, inUser) ;
			if( entityrootcategory == null)
			{
				log.error("No cat" + inEntity);
				return null;
			}
			if (inSelectedBox == null) 
			{
				return entityrootcategory;
			}
			//selectedcat = entityrootcategory.getChildByName(inSelectedBox.getName());
	//		if( selectedcat == null)
	//		{
			String catpath = entityrootcategory.getCategoryPath() + "/" + inSelectedBox.getName();
			selectedcat= (Category)getMediaArchive().getCategorySearcher().createCategoryPath(catpath);
		}
		else
		{
			selectedcat = loadDefaultFolder(inSelectedBox, inUser);
		}
		
		return selectedcat;
		
		
	}
	
	protected void setLightBoxCounts(Collection<LightBox> boxes)
	{
		Collection categories = new ArrayList();
		for (Iterator iterator = boxes.iterator(); iterator.hasNext();)
		{
			LightBox box = (LightBox) iterator.next();
			Category cat = box.getRootCategory();
			categories.add(cat);
		}
		QueryBuilder query = getMediaArchive().query("asset").orgroup("category", categories).facet("category"); //Random collection of stuff
		query.getQuery().setDefaultAggregationCount(2000); //Make sure we get enought of a sample size 2000 assets max
		HitTracker found =	query.search();
		
		Map categorycounts = new HashMap();
		
		for (Iterator iterator = boxes.iterator(); iterator.hasNext();) 
		{
			LightBox box = (LightBox)iterator.next();
			FilterNode node = found.findFilterChildValue("category",box.getRootCategory().getId());
			if( node != null)
			{
				box.setAssetCount(node.getCount());
			}
		}
	}
	
	/**
	 * This is good for finding related info on assets. To be used later
	 * @param inUser
	 * @param inModuleid
	 * @param inEntityid
	 * @param inLightboxid
	 * @return
	 
	public Map loadLightBoxResults(User inUser, String inModuleid, String inEntityid, String inLightboxid)
	{
		HitTracker lightboxassets = getMediaArchive().query("emedialightboxasset").named("lightboxassets").exact("lightboxid", inLightboxid).
				exact("parentmoduleid", inModuleid).
				exact("parententityid",inEntityid).facet("lightboxid").sort("ordering").search();
		
		lightboxassets.enableBulkOperations();
		Map<String,Data> assetidlookup = new HashMap();
		Collection assetids = lightboxassets.collectValues("primarymedia");

		Map<String,Object> hitassetlookup = new HashMap();

		//TODO: only support up to 1000 assets. Break down into chunks?
		HitTracker assethits = getMediaArchive().query("asset").ids(assetids).named("assethits").search();
		assethits.enableBulkOperations();
		for (Iterator iterator = assethits.iterator(); iterator.hasNext();) {
			Data asset = (Data) iterator.next();
			assetidlookup.put(asset.getId(),asset);
		}
		hitassetlookup.put("asset", assethits);
		
		for (Iterator iterator = lightboxassets.iterator(); iterator.hasNext();) {
			Data lightboxhit = (Data) iterator.next();
			Data asset = assetidlookup.get(lightboxhit.get("primarymedia")); 
			hitassetlookup.put(lightboxhit.getId(),asset);
		}
		hitassetlookup.put("emedialightboxasset", lightboxassets);
		
		//Get the categories
		
		return hitassetlookup;
	
	}
	*/
	
	public Data findFirstSelectedLightBox(Collection<LightBox> boxes)
	{
		if( boxes == null)
		{
			log.error("No lightboxs loaded");
			return null;
		}
		
		for (Iterator iterator = boxes.iterator(); iterator.hasNext();) {
			LightBox lightbox = (LightBox) iterator.next();
			Integer val = lightbox.getAssetCount();
			
			if( val != null && val > 0)
			{
				return lightbox;
			}
		}
		return null;
	}
	public void lightBoxRemoveAssets(User inUser, String inCategoryid, HitTracker inAssethits)
	{
		Category category = getMediaArchive().getCategory(inCategoryid);
		if (category != null) {
			List tosave = new ArrayList();
			for (Iterator iterator = inAssethits.iterator(); iterator.hasNext();) {
				Data data = (Data) iterator.next();
				Asset asset = (Asset) getMediaArchive().getAssetSearcher().loadData(data);
				asset.removeCategory(category);
				tosave.add(asset);
			}
			getMediaArchive().saveAssets(tosave);
		}
	}

	/*
//	public HitTracker searchForAssetsInCategory(Data inModule, Data inEntity, Data inSelectedBox, String sortby, User inUser)
	public HitTracker searchForAssetsInCategory(Category selectedCategory, String sortby, User inUser)
	{
//		Category parent = loadLightboxCategory(inModule, inEntity,inSelectedBox, null);
//		if( parent == null)
//		{
//			return null;
//		}
		HitTracker hits = getMediaArchive().query("asset").exact("category",selectedCategory).sort(sortby).named("catsearch").search();
		return hits;
	}
	*/
	

	public Integer addAssetsToCategory(MediaArchive archive, Category category, Collection assethits)
	{
		List<Asset> tosave = new ArrayList();
		for (Iterator iterator = assethits.iterator(); iterator.hasNext();) 
		{
			Data data = (Data) iterator.next();
			Asset asset = (Asset)archive.getAssetSearcher().loadData(data); //Why reload?
			asset.addCategory(category);
			tosave.add(asset);
		}
		archive.saveAssets(tosave);
		getMediaArchive().getAssetManager().createLinksTo(tosave,category.getCategoryPath());
		return tosave.size();
	}

	public void createEntitySnapshot(User inUser, Data inEntity, String changes)
	{
	
		Searcher searcher = getMediaArchive().getSearcher("entityactivityhistory");
		
		Data event = searcher.createNewData();
		if( inUser != null)
		{
			event.setProperty("user", inUser.getId());
		}
		event.setProperty("operation", "entitysaved");
		event.setProperty("moduleid", inEntity.get("entitysourcetype"));
		event.setProperty("entityid", inEntity.getId()); //data.getId() ??
		event.setValue("date", new Date());
		event.setProperty("historydetails", changes);
		
		
		String jsonsource = inEntity.toJsonString();
		event.setValue("entitysource", jsonsource);
		
		
		searcher.saveData(event, null);
		
	}
	
	public void restoreSnapshot(User inUser, String inHistoryid)
	{
	
		Data entityhistory = (Data) getMediaArchive().query("entityactivityhistory").id(inHistoryid).search().first();
		
		String moduleid = entityhistory.get("moduleid");
		String source = entityhistory.get("entitysource");
		String entityid = entityhistory.get("entityid");
		
		Data existing = getMediaArchive().getData(moduleid, entityid);
		
		//archive.getEntityManager().createEntitySnapshot( inReq.getUser(), existing, "");
	
		JSONParser parser = new JSONParser();
		JSONObject sourceObject = null;
		try {
			sourceObject = (JSONObject) parser.parse(source);
		} catch (Throwable e) {
			throw new OpenEditException("Noting to save");
		
		}
		JSONObject sourceObj = (JSONObject) sourceObject.get("_source");
		Searcher searcher = getMediaArchive().getSearcher(moduleid);
		searcher.saveJson(entityid, sourceObj);	
	}
	
	public Data findEntityByPath(String inModuleId, String sourcepath)
	{
		Data found = getMediaArchive().query(inModuleId).exact("entitysourcetype",sourcepath).searchOne();
		return found;
	}
}
