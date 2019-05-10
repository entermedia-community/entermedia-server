package org.entermediadb.projects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.data.BaseData;
import org.openedit.data.SaveableData;
import org.openedit.util.strainer.FilterReader;

public class LibraryCollection extends BaseData implements SaveableData, CatalogEnabled
{
	protected Data fieldLibrary;
	protected Category fieldCategoryRoot;
	protected int fieldAssetCount;
	protected String fieldCatalogId;
	protected ModuleManager fieldModuleManager;
	protected FilterReader fieldFilterReader;
	protected List fieldPermissions;

	
	public void setPermissions(List inPermissions)
	{
		fieldPermissions = inPermissions;
	}
	public String getCatalogId()
	{
		return fieldCatalogId;
	}
	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}
	
	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}
	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}
	protected MediaArchive getMediaArchive()
	{
		MediaArchive archive = (MediaArchive)getModuleManager().getBean(getCatalogId(),"mediaArchive");
		return archive;
	}
	public Data getLibrary()
	{
		if( fieldLibrary == null)
		{
			Collection ids = getValues("library");
			if( ids == null || !ids.isEmpty() )
			{
				fieldLibrary = getMediaArchive().getData("library",(String)ids.iterator().next());
			}
		}
		return fieldLibrary;
	}
	public void setLibrary(Data inLibrary)
	{
		fieldLibrary = inLibrary;
	}

	public int getAssetCount()
	{
		return fieldAssetCount;
	}
	public void setAssetCount(int inCount)
	{
		fieldAssetCount = inCount;
	}

	public long getCurentRevision()
	{
		Object obj = getValue("revisions");
		if( obj == null)
		{
			obj = 0;
		}

		Long revisionnumber = null;
		if( obj instanceof Integer)
		{
			revisionnumber = (long)(Integer)obj;
		}
		else
		{
			revisionnumber = (Long)obj;
		}

		return revisionnumber;
	}
	public boolean hasRootCategory()
	{
		
		return getRootCategoryId() != null;
	}
	public String getRootCategoryId()
	{
		String catid = get("rootcategory");
		if(catid == null || catid.isEmpty()) {
			return null;

		}
		 
return catid;
	}

	@Override
	public Object getValue(String inKey)
	{
		if( inKey.equals("parentcategories"))
		{
			if(getRootCategoryId() == null){
				return null;
			}
			Category root = getCategory();
			if( root == null)
			{
				return null;
			}
			return root.getParentCategories();
		}
		// TODO Auto-generated method stub
		return super.getValue(inKey);
	}
	
	public Category getCategory()
	{
		if(getRootCategoryId() == null){
			return null;
		}
		return getMediaArchive().getCategory(getRootCategoryId());
	}
	public boolean isVisibility(String inCode)
	{
		String code = get("visibility");
		if( code == null)
		{
			return false;
		}
		return code.equals(inCode);
	}	
	public boolean hasPendingAssets()
	{
		Object found = getMediaArchive().getAssetSearcher().query().orgroup("editstatus", "1|rejected").exact("category", getRootCategoryId()).searchOne();
		return found != null;
	}
	
	
//	public Permission getPermission(String inPermission) {
//		Data target = getMediaArchive().getData("datapermissions", "librarycollection-" + getId() + "-"+ inPermission);
//		if(target == null) {
//			return null;
//		}
//		String xml = target.get("value");
//		if(xml == null) {
//			return null;
//		}
//		return getMediaArchive().getPermission(inPermission, xml);	
//	
//	}
	
	public List getPermissions() {
		if (fieldPermissions == null)
		{
			fieldPermissions = new ArrayList();
			
		}

		return fieldPermissions;
	}
	
	
	

}