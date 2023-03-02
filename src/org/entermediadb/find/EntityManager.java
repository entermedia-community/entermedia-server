package org.entermediadb.find;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.collections.map.HashedMap;
import org.entermediadb.asset.MediaArchive;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.cache.CacheManager;
import org.openedit.profile.UserProfile;
import org.openedit.users.User;

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

	public Collection loadCategories(String inEntityType, String inEntityId)
	{
		//TODO: Caching ability
		Collection categories = (Collection)getCacheManager().get("entitymanager", inEntityType + "/" + inEntityId);
		//if( categories == null || categories.size() < 1)
		{
			categories = getMediaArchive().query("category").exact(inEntityType, inEntityId).sort("categorypath").search();
			getCacheManager().put("entitymanager", inEntityType + "/" + inEntityId,categories);
			
		}
		return categories;
	}

	protected MediaArchive getMediaArchive()
	{
		return (MediaArchive)getModuleManager().getBean(getCatalogId(), "mediaArchive", true);
	}
	
	public String loadUploadSourcepath(Data module, Data entity, User inUser)
	{
		String mask = (String) module.getValue("uploadsourcepath");
		String sourcepath = "";
		if(mask != null)
		{
			Map values = new HashedMap();
			
			values.put("module", module);
			values.put(module.getId(), entity);
			
			sourcepath = getMediaArchive().getAssetImporter().getAssetUtilities().createSourcePathFromMask( getMediaArchive(), inUser, "", mask, values);
		}
		
		return sourcepath;
	}	
	
}
