package org.entermediadb.find;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.cache.CacheManager;
import org.openedit.data.DataWithSearcher;
import org.openedit.data.PropertyDetail;
import org.openedit.data.Searcher;
import org.openedit.hittracker.FilterNode;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.ListHitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.users.User;
import org.openedit.util.DateStorageUtil;

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
		Category cat = loadDefaultFolder(module, entity,inUser,false);
		return cat;
		
	}
	public Category loadDefaultFolder(Data module, Data entity, User inUser, boolean create)
	{
		Category cat = null;
		
		String categoryid = entity.get("rootcategory");
		if( categoryid != null)
		{
			cat = getMediaArchive().getCategory(categoryid);
		}

		if( cat == null)
		{
			String sourcepath = loadUploadSourcepath(module,entity,inUser);
			if( create )
			{
				cat = getMediaArchive().getCategorySearcher().createCategoryPath(sourcepath);
			}
			else
			{
				cat = getMediaArchive().getCategorySearcher().loadCategoryByPath(sourcepath);
			}
			if( cat != null)
			{
				entity.setValue("rootcategory",cat.getId());
				getMediaArchive().saveData(module.getId(), entity);
			}
		}
		if( cat == null)
		{
			//Cant find sourcepath
			return null;
		}
		if( cat.getValue(module.getId()) == null)
		{
			cat.setValue(module.getId(),entity.getId());
			getMediaArchive().getCategorySearcher().saveData(cat);
		}
		return cat;
	}	
	public String loadUploadSourcepath(Data module, Data entity, User inUser)
	{
		return loadUploadSourcepath(module, entity, inUser,true);
	}
	public String loadUploadSourcepath(Data module, Data entity, User inUser, boolean inSave)
	{
		if (entity == null ) {
			return null;
		}
		if( entity.getValue("uploadsourcepath") != null)
		{
			return entity.get("uploadsourcepath");
		}
		
		String categoryid = entity.get("rootcategory");
		if( categoryid != null)
		{
			Category cat = getMediaArchive().getCategory(categoryid);
			if( cat != null)
			{
				String sourcepath = cat.getCategoryPath();
				entity.setValue("uploadsourcepath",sourcepath );
				getMediaArchive().saveData(module.getId(), entity);
				return sourcepath;
			}
		}
		
		String mask = (String) module.getValue("uploadsourcepath");
		String sourcepath = "";
		if(mask != null)
		{
			Map values = new HashedMap();
			
			values.put("module", module);
			
			//sourcepath = getMediaArchive().getAssetImporter().getAssetUtilities().createSourcePathFromMask( getMediaArchive(), inUser, "", mask, values, null);
			
			DataWithSearcher smartdata = new DataWithSearcher(getMediaArchive().getSearcherManager(), getCatalogId(), module.getId(), entity);
			values.put(module.getId(), smartdata);
			values.put("data", smartdata);

			
			sourcepath = getMediaArchive().replaceFromMask( mask, entity, module.getId(), values, null);  //Static locale?

			sourcepath = sourcepath.replaceAll("////", "/");
			if( inSave)
			{
				for (int i = 0; i < 20; i++) {
					//Already exists
					Data cat = getMediaArchive().query(module.getId()).exact("uploadsourcepath",sourcepath).searchOne();
					if( cat != null)
					{
						sourcepath = sourcepath + "_" + (i+2);
					}
					else if(i == 19)
					{
						throw new OpenEditException("Too many duplicate source paths");
					}
					else
					{
						break;
					}
				}
			}
		}
		if( sourcepath.isEmpty() && entity != null)
		{
			
			if(module.getId().equals("librarycollection")) {
				LibraryCollection coll = (LibraryCollection) getMediaArchive().getData("librarycollection", entity.getId());
				if (coll != null)
				{
					Category uploadto  = null;
					uploadto = coll.getCategory();
					if(uploadto != null) 
					{
						sourcepath = uploadto.getCategoryPath(); 
						String year = getMediaArchive().getCatalogSettingValue("collectionuploadwithyear");
						if( year == null || Boolean.parseBoolean(year)) //Not reindexed yet
						{
							String thisyear = DateStorageUtil.getStorageUtil().formatDateObj(new Date(), "yyyy"); 
							sourcepath = sourcepath + "/" + thisyear;
						}
						sourcepath = sourcepath + "/";
					}
					}
			}
			if( sourcepath.isEmpty())
			{
				//long year = Calendar.getInstance().get(Calendar.YEAR);
				sourcepath = module.getName("en") + "/" + entity.getName("en") + "/";
			}
		}
		entity.setValue("uploadsourcepath",sourcepath );
		
		if( inSave)
		{
			getMediaArchive().saveData(module.getId(), entity);
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
				asset.addCategory(category);
				tosave.add(asset);
			}
			getMediaArchive().saveAssets(tosave);
		}
		//deSelect assets after copy
		hits.getSelectedHitracker().deselectAll();
		return tosave.size();
	}
	
	public Boolean addAssetToEntity(User inUser,String pickedmoduleid, String pickedentityid, Asset asset) 
	{
		Data module = getMediaArchive().getCachedData("module", pickedmoduleid);
		Data entity =getMediaArchive().getCachedData(pickedmoduleid,pickedentityid);
		Category category = loadDefaultFolder(module, entity, inUser, true);

		if(category != null)
		{
			asset.addCategory(category);
		}
		//getMediaArchive().saveAsset(asset);
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
		
		Category copyingcategory =  getMediaArchive().getCategory(categoryid);
		
		if(copyingcategory != null)
		{
			Searcher categorysearcher = getMediaArchive().getSearcher("category");
			Category child = (Category) categorysearcher.createNewData();
			child.setName(copyingcategory.getName());
			rootcategory.addChild(child);
			categorysearcher.saveData(child);
			
			String[] catids = new String [] {categoryid};
			getMediaArchive().getCategoryEditor().copyEverything(inUser,catids, child.getId());
		}
		
		return true;
	}
	
	public Data copyEntity(WebPageRequest inContext, String sourcemoduleid, String pickedmoduleid, Data source) 
	{
		
		Searcher entitysearcher = getMediaArchive().getSearcher(pickedmoduleid);
		Data newchild = entitysearcher.createNewData();
		

		for (Iterator iterator = source.getProperties().keySet().iterator(); iterator.hasNext();) {
			String key = (String) iterator.next();
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
	
	
	
	public Integer copyEntities(WebPageRequest inContext, String sourcemoduleid, String pickedmoduleid, HitTracker hits) 
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
				
		return tosave.size();
	}
	
	public Integer copyEntities(WebPageRequest inContext, String sourcemoduleid, String pickedmoduleid, String sourceentityid) 
	{
		Data source = getMediaArchive().getData(sourcemoduleid, sourceentityid);
		if(source != null) {
			Data newchild = copyEntity(inContext, sourcemoduleid, pickedmoduleid, source);
			if(newchild != null) {
				getMediaArchive().saveData(pickedmoduleid, newchild);
				inContext.putPageValue("newentity", newchild);
				return 1;
			}
		}
		return 0;
	}
	
	public Integer addToSearchCategory(WebPageRequest inContext, String sourcemoduleid, HitTracker hits, String id) 
	{
		List tosave = new ArrayList();
		if(hits != null && hits.getSelectedHitracker() != null) {
			for (Iterator iterator = hits.getSelectedHitracker().iterator(); iterator.hasNext();) {
				MultiValued hit = (MultiValued) iterator.next();
				hit.addValue("searchcategory", id);
				tosave.add(hit);
			}
			getMediaArchive().saveData(sourcemoduleid, tosave);
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
	
	
	public Collection<Data> getEntitiesForCategories(Collection<Category> inParentCategories)
	{
		if (inParentCategories == null) {
			return null;
		}
		Collection<Data> items = new ArrayList();
		
		for (Iterator iterator1 = inParentCategories.iterator(); iterator1.hasNext();) 
		{
			Category cat = (Category) iterator1.next();
			for (Iterator iterator = getMediaArchive().getList("module").iterator(); iterator.hasNext();)
			{
				Data module = (Data) iterator.next();
				Object value = cat.findValue(module.getId());
				if( value != null)
				{
					if( value instanceof Collection)
					{
						Collection all = (Collection) value;
						for (Iterator iterator2 = all.iterator(); iterator2.hasNext();)
						{
							String item = (String) iterator2.next();
							Data entity = getMediaArchive().getCachedData(module.getId(), item);
							if (entity != null)
							{
								//entity.setValue("moduleid", module.getId());
								items.add( entity);
							}
						}
					}
					else 
					{
						Data entity = getMediaArchive().getCachedData(module.getId(), (String) value);
						if (entity != null)
						{
							//entity.setValue("moduleid", module.getId());
							items.add(entity);
						}	
					}
					
				}
			}
		}
		return items;
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
	
	public int addToWorkflowStatus(User inUser, String inModuleid, String inEntityid, HitTracker inAssethits, String lightboxid)
	{
		if( lightboxid == null)
		{
			log.error("No box selected");
			return 0;
		}
		Searcher searcher = getMediaArchive().getSearcher("emedialightboxasset");
		
		Collection assetstoadd =  inAssethits.collectValues("id");
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
			Data asset = (Data) iterator.next();
			//Look for existing?
			if( !alreadyadded.contains(asset.getId()) )
			{
				Data event = searcher.createNewData();
				event.setProperty("lightboxid", lightboxid);
				event.setProperty("parententityid", inEntityid);
				event.setProperty("parentmoduleid", inModuleid);
				event.setValue("name", asset.getName());
				event.setValue("primarymedia", asset.getId());
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

	public HitTracker loadLightBoxesForModule(Data inModule, Data inEntity,User inUser)
	{
		if( inModule == null)
		{
			log.error("No module");
			return null;
		}
		//Search for all the boxes that match. 
		HitTracker boxes = getMediaArchive().query("emedialightbox").or().exact("showonall", true).
				exact("parentmoduleid", inModule.getId()).sort("orderingUp").search();
		//Then each box has a child record with an assetid and comments/statuses
		//TODO: Search for each box for total assets using facets?
		return boxes;
	}
	
	public HitTracker loadLightBoxeAssetsForModule(Collection inBoxes, Data inModule, Data inEntity,User inUser)
	{
	
		//Search for all the boxes that match. 
		HitTracker assets = getMediaArchive().query("emedialightboxasset").orgroup("lightboxid", inBoxes).
				exact("parentmoduleid", inModule.getId()).
				exact("parententityid",inEntity).facet("lightboxid").sort("ordering").search();
		//Then each box has a child record with an assetid and comments/statuses
		//TODO: Search for each box for total assets using facets?
		return assets;
	}
	
	public HitTracker loadLightBoxAssets(String inModule, String inEntity, String inLightBoxId, User inUser)
	{
		//Search for all the boxes that match. 
		HitTracker assets = getMediaArchive().query("emedialightboxasset").named("lightboxassets").exact("lightboxid", inLightBoxId).
				exact("parentmoduleid", inModule).
				exact("parententityid",inEntity).facet("lightboxid").sort("ordering").search();
		//Then each box has a child record with an assetid and comments/statuses
		//TODO: Search for each box for total assets using facets?
		return assets;
	}

	public Data findFirstSelectedLightBox(HitTracker boxes, HitTracker assets)
	{
		for (Iterator iterator = boxes.iterator(); iterator.hasNext();) {
			Data lightbox = (Data) iterator.next();
			FilterNode node = assets.findFilterChildValue("lightboxid", lightbox.getId());
			if( node != null)
			{
				return lightbox;
			}
		}
		return null;
	}

	public void updateLightBoxAssetOrderings(String inLightBox, String[] inBoxAssets, String[] inNewOrderings) 
	{
		Collection ids = Arrays.asList(inBoxAssets);
		Collection exiting = getMediaArchive().query("emedialightboxasset").ids(ids).search();
		Map<String,String> ordering = new HashMap();
		for (int i = 0; i < inBoxAssets.length; i++) {
			ordering.put(inBoxAssets[i],inNewOrderings[i]);
		}
		Collection tosave = new ArrayList();
		for (Iterator iterator = exiting.iterator(); iterator.hasNext();) {
			Data brick = (Data) iterator.next();
			String order = ordering.get(brick.getId());
			brick.setValue("ordering",order);
			tosave.add(brick);
		}
		
		getMediaArchive().saveData("emedialightboxasset", tosave);
//		tosave = getMediaArchive().query("emedialightboxasset").ids(ids).search();
//		for (Iterator iterator = tosave.iterator(); iterator.hasNext();) {
//			Data brick = (Data) iterator.next();
//			String order = brick.get("ordering");
//			log.info(order);
//		}
	}
	
	public Map loadLightBoxResults(User inUser, String inModuleid, String inEntityid, String inLightboxid)
	{
		HitTracker lightboxassets = loadLightBoxAssets(inModuleid,inEntityid,inLightboxid, inUser);
		Map<String,Data> assetidlookup = new HashMap();
		Collection assetids = lightboxassets.collectValues("primarymedia");
		
		//TODO: only support up to 1000 assets. Break down into chunks?
		Collection hits = getMediaArchive().query("asset").ids(assetids).search();
		for (Iterator iterator = hits.iterator(); iterator.hasNext();) {
			Data asset = (Data) iterator.next();
			assetidlookup.put(asset.getId(),asset);
		}
		Map<String,Object> hitassetlookup = new HashMap();
		for (Iterator iterator = lightboxassets.iterator(); iterator.hasNext();) {
			Data lightboxhit = (Data) iterator.next();
			Data asset = assetidlookup.get(lightboxhit.get("primarymedia")); 
			hitassetlookup.put(lightboxhit.getId(),asset);
		}
		hitassetlookup.put("all", lightboxassets);
		return hitassetlookup;
	
	}

	
	public void lightBoxRemoveAssets(User inUser, String inLightBoxId, HitTracker inAssethits)
	{
		Collection assetids = inAssethits.collectValues("id");
		Collection boxassets  = getMediaArchive().query("emedialightboxasset").exact("lightboxid",inLightBoxId).orgroup("primarymedia", assetids).search();
		
		getMediaArchive().getSearcher("emedialightboxasset").deleteAll(boxassets, inUser);
	
	}
	
}
