package org.entermediadb.users;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.config.XMLConfiguration;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.HitTracker;
import org.openedit.page.Page;
import org.openedit.page.Permission;
import org.openedit.page.PermissionSorter;
import org.openedit.util.strainer.Filter;
import org.openedit.util.strainer.FilterReader;
import org.openedit.util.strainer.FilterWriter;

public class PermissionManager implements CatalogEnabled
{
	protected SearcherManager fieldSearcherManager;
	protected PermissionSorter fieldPermissionSorter;
	protected String fieldCatalogId;
	protected FilterReader fieldFilterReader;
	protected FilterWriter fieldFilterWriter;
	public FilterReader getFilterReader()
	{
		return fieldFilterReader;
	}

	public void setFilterReader(FilterReader inFilterReader)
	{
		fieldFilterReader = inFilterReader;
	}

	public String getCatalogId()
	{
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}

	
	public PermissionSorter getPermissionSorter()
	{
		if (fieldPermissionSorter == null)
		{
			fieldPermissionSorter = new PermissionSorter();
			
			Collection items = getSearcherManager().getList(getCatalogId(),"permissionsapp");
			fieldPermissionSorter.loadPermissions(items);
			
		}
		return fieldPermissionSorter;
	}

	public void setPermissionSorter(PermissionSorter inPermissionSorter)
	{
		fieldPermissionSorter = inPermissionSorter;
	}
	
	public void loadPermissions(WebPageRequest inReq, Page inPage, String limited)
	{
		List permissions = null;
		if( limited == null )
		{
			permissions = inPage.getPermissions();
		}
		else
		{
			permissions = new ArrayList();
			String[] array = limited.split("\\s+");
			for (int i = 0; i < array.length; i++)
			{
				Permission permission = inPage.getPermission( array[i] );
				if( permission != null )
				{
					permissions.add(permission);
				}
			}
		}
		if (permissions != null)
		{
			Collections.sort(permissions,getPermissionSorter() );
			
			for (Iterator iterator = permissions.iterator(); iterator.hasNext();)
			{
				Permission per = (Permission) iterator.next();
				boolean value = per.passes(inReq);
				inReq.putPageValue("can" + per.getName(), Boolean.valueOf(value));
			}
		}
	}

	public void loadModulePermissions(String inModuleid, String inParentFolderId, String inDataId, WebPageRequest inReq)
	{
		//Base module permissions. Module wide
		//TODO: Cache
		HitTracker <Data> modulepermissions = getSearcherManager().query(getCatalogId(), "datapermissions").
				exact("permissiontype", inModuleid).search();
		for (Iterator iterator = modulepermissions.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			Permission per = findPermission(inModuleid, inParentFolderId, inDataId, data.getId());
			if( per != null)
			{
				String permissionid = data.getId();
				Boolean systemwide = (Boolean)inReq.getPageValue("can" + permissionid);
				if( systemwide == null || systemwide == false)  //Option
				{
					boolean value = per.passes(inReq);
					if( value )
					{
						inReq.putPageValue("can" + permissionid, Boolean.valueOf(value));
					}
				}
			}
		}
		
		Collection custompermissions = loadCustomPermissionRules(inModuleid,inParentFolderId,inDataId);

		for (Iterator iterator = custompermissions.iterator(); iterator.hasNext();)
		{
			Permission per = (Permission) iterator.next();
			String permid = per.get("permissionid");
			Boolean systemwide = (Boolean)inReq.getPageValue("can" + permid);
			if( systemwide == null || !systemwide )
			{
				boolean value = per.passes(inReq);
				if( value )
				{
					inReq.putPageValue("can" + permid, Boolean.valueOf(value));
				}
			}	
		}
	}
	protected Collection loadCustomPermissionRules(String inDataType,String inParentFolderId, String inSpecificRow)
	{
		//Use a 5 min Cache
		Collection<Permission> rules = new ArrayList();
		HitTracker <Data> modulepermissions = getSearcherManager().query(getCatalogId(), "datapermissions").
				exact("moduleid", inDataType).search();

		for (Iterator iterator = modulepermissions.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			
			//Specific Asset specific
			Permission per = findPermission(inDataType, null, inSpecificRow, data.getId());
			if( per == null)
			{
				//CollectionID specific
				 per = findPermission(inDataType, inParentFolderId, null, data.getId());
			}
			if( per != null)
			{
				per.setValue("permissionid",data.getId()); //Needed?
				rules.add(per);
			}
		}
		return rules;
	}

	public Permission getPermission(String inId, String inPermissionXml)
	{

		try
		{
			Document doc = DocumentHelper.parseText(inPermissionXml);
			Element permissionconf = doc.getRootElement().element("permission");
			XMLConfiguration rootConfig = new XMLConfiguration();
			rootConfig.populate(permissionconf);
			Filter filter = getFilterReader().readFilterCollection(rootConfig, inId);
			Permission per = new Permission();
			per.setName(inId);
			per.setRootFilter(filter);
			per.setId(inId);
			//per.setPath(inPageConfig.getPath());
			return per;
		}
		catch (OpenEditException e)
		{
			throw (e);
		}
		catch (DocumentException e)
		{
			throw new OpenEditException(e);
		}
	}

	public Permission getPermission(String inPermission)
	{
		Data target = (Data)getSearcher("custompermissions").searchById(inPermission);
		if (target == null)
		{
			return null;
		}
		String xml = target.get("value");
		if (xml == null)
		{
			return null;
		}

		return getPermission(inPermission, xml);

	}
	
	public Permission findPermission(String inModule, String inFolder, String inData, String inPermissionId ) {
		
		
		Searcher searcher = getSearcher("custompermissions");
		
		
			Data target = (Data) searcher.query().ignoreEmpty().exact("moduleid", inModule).exact("parentfolderid", inFolder).exact("dataid", inData).exact("datapermission", inPermissionId).searchOne();
			if(target != null) {
				Permission permission = getPermission(target.getId());
				return permission;
			}
		
return null;		
		
		
	}
	
	

	public void savePermission( Permission inPermission)
	{
		Searcher custompermissions = getSearcher("custompermissions");
		Data target = (Data) custompermissions.searchById(inPermission.getId());
		if (target == null)
		{
			target = custompermissions.createNewData();
			target.setId(inPermission.getId());
		}

		for (Iterator iterator = inPermission.getMap().keySet().iterator(); iterator.hasNext();)
		{
		String key = (String) iterator.next();
			Object val = (Object) inPermission.getValue(key);
			target.setValue(key, val);
			
		}
		Document doc = DocumentHelper.createDocument();
		XMLConfiguration configuration = new XMLConfiguration("root");

		getFilterWriter().writeFilterCollection(inPermission, configuration);

		String xml = configuration.asXml().asXML();
		target.setValue("value", xml);

		custompermissions.saveData(target);
		inPermission.setId(target.getId());

	}
	private Searcher getSearcher(String inSearchType)
	{
		return getSearcherManager().getSearcher(getCatalogId(), inSearchType);
	}

	public FilterWriter getFilterWriter()
	{
		if (fieldFilterWriter == null)
		{
			fieldFilterWriter = new FilterWriter();
		}
		return fieldFilterWriter;
	}

	public void setFilterWriter(FilterWriter inFilterWriter)
	{
		fieldFilterWriter = inFilterWriter;
	}

}
