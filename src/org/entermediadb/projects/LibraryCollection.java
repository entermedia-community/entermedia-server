package org.entermediadb.projects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.modules.BaseDataEntity;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.MultiValued;
import org.openedit.data.BaseData;
import org.openedit.data.SaveableData;
import org.openedit.util.PathUtilities;
import org.openedit.util.strainer.FilterReader;

public class LibraryCollection extends BaseDataEntity implements SaveableData, CatalogEnabled
{
	protected Collection fieldLibraries;
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
		Collection all = getLibraries();
		if(all == null ||  all.isEmpty() )
		{
			return null;
		}
		return (Data)all.iterator().next();
	}
	public Collection getLibraries()
	{
		if( fieldLibraries == null)
		{
			if( getMediaArchive() != null )
			{
				Collection ids = getValues("library");
				if( ids != null && !ids.isEmpty() )
				{
					fieldLibraries = getMediaArchive().query("library").ids(ids).search();
				}
			}
		}
		return fieldLibraries;
	}
	public void setLibrary(Data inLibrary)
	{
		fieldLibraries = Arrays.asList(inLibrary);
		setValue("library", fieldLibraries);
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

	@Override
	public Object getValue(String inKey)
	{
		if( inKey.equals("urlname") && super.getValue("urlname") == null)
		{
			if(getName() != null)
			{
			String url = PathUtilities.dash(String.valueOf( getName() ) );
			return url;
			}
		}

		if( inKey.equals("parentcategories"))
		{
			if(getRootCategoryId() == null){
				return null;
			}
			Object values = super.getValues(inKey);
//			if( values == null) //Must not be in the index yet
//			{
				Category root = getCategory();
				if( root == null)
				{
					return null;
				}
				values = root.getParentCategories();
//			}
			return values;
		}
		return super.getValue(inKey);
	}
	
	public Category getCategory()
	{
		if(getRootCategoryId() == null){
			return null;
		}
		Category cat = getMediaArchive().getCategory(getRootCategoryId());
		if( cat!= null &&  !cat.containsValue("librarycollection",getId()))
		{
			cat.addValue("librarycollection", getId());
			getMediaArchive().saveData("category", cat);
		}
		return cat;
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
	
	public String pickLabel(MultiValued inTopic)
	{
		TopicLabelPicker labels = new TopicLabelPicker();
		labels.setArchive(getMediaArchive());
		labels.setLibraryCollection(this);
		return labels.showLabel(inTopic);
	}

	
	public String getWebName()
	{
		if( getName() ==  null)
		{
			return "null";
		}
		String label = getName().replaceAll(" ", "-");
		return label;
	}
	

}