package org.entermediadb.find;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.HashedMap;
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
import org.openedit.WebPageRequest;
import org.openedit.cache.CacheManager;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.users.User;
import org.openedit.util.DateStorageUtil;

public class EntityManager implements CatalogEnabled
{
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
	public Category createDefaultFolder(Data entity, User inUser)
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
		Category cat = loadDefaultFolder(module, entity,inUser,true);
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
		String sourcepath = loadUploadSourcepath(module,entity,inUser);
		Category cat = null;
		if( create )
		{
			cat = getMediaArchive().getCategorySearcher().createCategoryPath(sourcepath);
		}
		else
		{
			cat = getMediaArchive().getCategorySearcher().loadCategoryByPath(sourcepath);

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
		if (entity == null ) {
			return null;
		}
		if( entity.getValue("uploadsourcepath") != null)
		{
			return entity.get("uploadsourcepath");
		}
		String mask = (String) module.getValue("uploadsourcepath");
		String sourcepath = "";
		if(mask != null)
		{
			Map values = new HashedMap();
			
			values.put("module", module);
			values.put(module.getId(), entity);
			
			sourcepath = getMediaArchive().getAssetImporter().getAssetUtilities().createSourcePathFromMask( getMediaArchive(), inUser, "", mask, values);
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
		getMediaArchive().saveData(module.getId(), entity);
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
		return tosave.size();
	}
	
	public Boolean addAssetToEntity(User inUser,String pickedmoduleid, String pickedentityid, String assetid) 
	{
		Data module = getMediaArchive().getCachedData("module", pickedmoduleid);
		Data entity =getMediaArchive().getCachedData(pickedmoduleid,pickedentityid);
		Category category = loadDefaultFolder(module, entity, inUser, true);

		Asset asset = (Asset)getMediaArchive().getAsset(assetid);
		asset.addCategory(category);
		getMediaArchive().saveAsset(asset);
		return true;

	}

}
