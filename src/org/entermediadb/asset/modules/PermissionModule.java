package org.entermediadb.asset.modules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.config.Configuration;
import org.openedit.config.XMLConfiguration;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.page.Page;
import org.openedit.page.PageProperty;
import org.openedit.page.PageSettings;
import org.openedit.page.Permission;
import org.openedit.users.Group;
import org.openedit.util.strainer.BooleanFilter;
import org.openedit.util.strainer.Filter;
import org.openedit.util.strainer.FilterReader;
import org.openedit.util.strainer.GroupFilter;
import org.openedit.util.strainer.OrFilter;
import org.openedit.util.strainer.SettingsGroupFilter;

public class PermissionModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(PermissionModule.class);
	
	public Permission loadPermission(WebPageRequest inReq) throws Exception
	{
		String path = inReq.getRequestParameter("editPath");
		if(path == null){
		
		}
		String name = inReq.getRequestParameter("id");
		if( name == null)
		{
			name = inReq.getRequestParameter("name");
		}
		if( name != null)
		{
			PageSettings settings = getPageManager().getPageSettingsManager().getPageSettings(path);
			Permission permission = loadOrCreatePermission(settings,path,name); 
			inReq.putPageValue("editPath", path);			
			inReq.putPageValue("permission", permission);
			return permission;
		}
		return null;
	}
	
	private Permission loadOrCreatePermission(PageSettings inSettings, String path, String inName)
	{
		Permission permission = inSettings.getPermission(inName, true);	
		if( permission == null || !permission.getPath().equals(path))
		{
			Permission per = new Permission();
			per.setName(inName);
			if( permission != null && permission.getRootFilter() != null)
			{
				FilterReader reader = (FilterReader) getModuleManager().getBean("filterReader");
				per.setRootFilter(permission.getRootFilter().copy(reader, inName));
			}
			else
			{
				
			}
			per.setPath(path);
			permission = per;
		}
		return permission;
	}

	public void loadPermissions(WebPageRequest inReq) throws Exception
	{
		String path = inReq.getRequestParameter("editPath");
		if( path == null )
		{
			path = inReq.findValue("editPath");
		}
		//System.out.println(path);

		PageSettings settings = getPageManager().getPageSettingsManager().getPageSettings(path);
		List localPermissions = settings.getFieldPermissions();
		List parentPermissions = settings.getPermissions(false);

		List combined = new ArrayList();
		if( localPermissions != null)
		{
			
			Collections.sort(localPermissions);
			combined.addAll(localPermissions);
		}
		Collections.sort(parentPermissions);
		combined.addAll(parentPermissions);
		//inReq.putPageValue("permissions", localPermissions);
		inReq.putPageValue("permissions", combined);
		inReq.putPageValue("settings", settings);
		inReq.putPageValue("editPath", path);
	}
	
	private void resetValues(Filter inFilter)
	{
		if (inFilter instanceof BooleanFilter)
		{
			inFilter.setValue("false");
		}
		
		if (inFilter.getFilters() != null)
		{
			for (int i = 0; i < inFilter.getFilters().length; i++)
			{
				resetValues(inFilter.getFilters()[i]);
			}
		}
	}
	/**
	 * This saves a bunch of stuff all at once
	 * @param inReq
	 * @throws Exception
	 */
	public void savePermissions(WebPageRequest inReq) throws Exception
	{
		Permission permission = loadPermission(inReq);
		if( permission == null)
		{
			return;
		}
		resetValues(permission.getRootFilter());
		
		for (Iterator iterator = inReq.getParameterMap().keySet().iterator(); iterator.hasNext();)
		{
			String  key = (String ) iterator.next();
			if( key.startsWith("condition"))
			{
				int start = "condition.".length();
				String traverse = key.substring(start,key.indexOf('.', start+1));
				String[] tree = traverse.split("/");
				int[] list = makeInts(tree);
				Filter target = permission.findCondition(list);
				String value = inReq.getRequestParameter(key);
				if( key.endsWith(".value"))
				{
					target.setValue(value);
				}
				else if( key.endsWith(".name"))
				{
					target.setProperty("name", value);
				}
				else if( key.endsWith(".property"))
				{
					target.setProperty("property",value);
				}
				String fieldroot = "condition." + traverse + ".field";
				String[] fields = inReq.getRequestParameters(fieldroot);
				if(fields != null){
					for (String string : fields) {
						String extra = inReq.getRequestParameter("condition." + traverse + "." + string + ".value");
						if(extra != null){
							target.setProperty(string, extra);
						}
					}
				}
				
				//String fields = inReq.getRequestParameter(condtion.)
				//TODO: Handle special filters
			}
		}
		savePermission(permission);
		
	}

	protected void savePermission(Permission permission) throws OpenEditException
	{
		String path = permission.getPath();
		Page page = getPageManager().getPage(path,true);
		page.getPageSettings().addPermission(permission);
		getPageManager().saveSettings(page);
		getPageManager().clearCache();
	}
	
	public void removeCondition(WebPageRequest inReq) throws Exception
	{
		String traverse = inReq.getRequestParameter("traverse");
		if( traverse != null)
		{
			Permission permission = loadPermission(inReq);
			if( permission != null)
			{
				String[] tree = traverse.split("/");
				if (tree.length > 1)
				{
					int[] list = makeInts(tree);
					Filter parent = permission.findConditionParent(list);
					if( parent == null)
					{
						return;
					}
					int target = list[list.length-1];
					if( target > parent.getFilters().length)
					{
						return;
					}
					Filter node = parent.getFilters()[target];
					parent.removeFilter(node);
				}
				else
				{
					permission.setRootFilter(null);
				}
			}
			savePermission(permission);
		}
	}

	public void addPermission(WebPageRequest inReq) throws Exception
	{
		String path = inReq.getRequestParameter("editPath");
		String name = inReq.getRequestParameter("name");
		Permission permission = new Permission();
		permission.setName(name);
		if (path != null)
		{
			permission.setPath(path);
			savePermission(permission);
		}
		inReq.putPageValue("selectedpermission", permission);
		inReq.putPageValue("permission", permission);
		loadPermissions(inReq);
	}
	public void removePermission(WebPageRequest inReq) throws Exception
	{
		//String path = inReq.getRequestParameter("editPath");
		Permission permission = loadPermission(inReq);

		if ( permission != null)
		{
			Page page = getPageManager().getPage(permission.getPath(),true);
			PageSettings settings = page.getPageSettings();
			settings.removePermission(permission);
			getPageManager().saveSettings(page);
			getPageManager().clearCache(page);
		}
	}
	
	public void resetPermission(WebPageRequest inReq) throws Exception
	{
		String path = inReq.getRequestParameter("editPath");
		Permission permission = loadPermission(inReq);
		Page page = getPageManager().getPage(path,true);
		page.getPageSettings().removePermission(permission);//may not be in here			
		getPageManager().saveSettings(page);
		getPageManager().clearCache(page);
	}
	public void addGroup(WebPageRequest inReq) throws Exception
	{
		String type = inReq.getRequestParameter("addgroup");
		if( type != null)
		{
			Permission permission = loadPermission(inReq);
			if( type.equals("false"))
			{
				BooleanFilter nope = new BooleanFilter();
				nope.setTrue(false);
				permission.setRootFilter(nope);
			}
			else if( type.equals("true"))
			{
				BooleanFilter yup = new BooleanFilter();
				yup.setTrue(true);
				permission.setRootFilter(yup);			
			}
			else if( type.equals("xml"))
			{
				return;
			}
			else if ( type.startsWith("group."))
			{
				Filter root = permission.getRootFilter();
				if(!(root instanceof OrFilter))
				{
					root = new OrFilter();
					permission.setRootFilter(root);
				}
				GroupFilter gf = new GroupFilter();
				String groupid = type.substring("group.".length());
				gf.setGroupId(groupid);
				root.addFilter(gf);
			}
			else if ( type.startsWith("settingsgroup."))
			{
				Filter root = permission.getRootFilter();
				if(!(root instanceof OrFilter))
				{
					root = new OrFilter();
					permission.setRootFilter(root);
				}
				SettingsGroupFilter gf = new SettingsGroupFilter();
				String groupid = type.substring("settingsgroup.".length());
				gf.setGroupId(groupid);
				root.addFilter(gf);
			}
			savePermission(permission);
		}
	}	
	public void addCondition(WebPageRequest inReq) throws Exception
	{
		String path = inReq.getRequestParameter("editPath");
		String name = inReq.getRequestParameter("name");
		String traverse = inReq.getRequestParameter("traverse");
		String type = inReq.getRequestParameter("conditiontype");
		String id = null;
		
		Data conditiondetail =  getMediaArchive(inReq).getData("conditiontypes" , type);
		 if(conditiondetail !=  null && conditiondetail.get("type") != null){
			 id=type;
			 type = conditiondetail.get("type");
			 
		 }
		
		FilterReader reader = (FilterReader)getModuleManager().getBean("filterReader");
		
		if( name != null)
		{
			Permission permission = loadPermission(inReq);
			if( permission != null)
			{
				Configuration config = new XMLConfiguration();
				config.addChild(new XMLConfiguration(type));
				
				Filter newFilter = reader.readFilterCollection(config, name);
				if (permission.getRootFilter() == null)
				{
					permission.setRootFilter(newFilter);
				}
				else if( traverse != null && traverse.length() > 0)
				{
					String[] tree = traverse.split("/");
					int[] list = makeInts(tree);
					Filter parent = permission.findCondition(list);
					parent.addFilter(newFilter);
				}
				else
				{
					permission.getRootFilter().addFilter(newFilter);
				}
				if("action".equals(type)){
					String action =  conditiondetail.get("method");
					newFilter.setProperty("name", action);
					newFilter.setProperty("conditiontype",id );
				}
				Page page = getPageManager().getPage(path,true);
				page.getPageSettings().setProperty("encoding","UTF-8");
				page.getPageSettings().addPermission(permission);
				
				getPageManager().saveSettings(page);
				getPageManager().clearCache(page);
			}
		}
	}
	
	private int[] makeInts(String[] tree)
	{
		int[] list = new int[tree.length];
		for (int i = 0; i < list.length; i++)
		{
			list[i] = Integer.parseInt(tree[i]);
		}
		return list;
	}
	
	public void loadPageProperties(WebPageRequest inReq) throws Exception
	{
		String path = inReq.getRequestParameter("editPath");
		if( path == null)
		{
			log.error("editPath is required");
			return;
		}
		PageSettings settings = getPageManager().getPageSettingsManager().getPageSettings(path);
		List pageproperties = new ArrayList();
		List props = settings.getAllProperties();
		for (Iterator iterator = props.iterator(); iterator.hasNext();)
		{
			PageProperty property = (PageProperty) iterator.next();
			if( property.getValue() != null )
			{
				if( property.getValue().equals("true") || property.getValue().equals("false") )
				{
					pageproperties.add( property );
				}
			}
		}
		inReq.putPageValue("pageproperties", pageproperties );

	}

	public void loadPermissionsByType(WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		if( catalogid == null )
		{
			catalogid = inReq.findValue("applicationid");
		}
		String searchtype = inReq.findValue("permissiontype");
		
		//we are going to load a searcher and a list of permissions
		HitTracker hits  = getSearcherManager().getList(catalogid, searchtype);
		inReq.putPageValue("permissions", hits);
		//this will get shown with edit button

		String permissionpath = inReq.findValue("editPath");
		if( permissionpath == null)
		{
			permissionpath = "/" + catalogid + "/_site.xconf";
		}
		Page page = getPageManager().getPage(permissionpath,true);
		inReq.putPageValue("settingspage", page);
	}	
	
	public void loadPermissionForEdit(WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		if( catalogid == null )
		{
			catalogid = inReq.findValue("applicationid");
		}
		if(catalogid== null)
		{
			return;
		}
		//String group = inReq.getRequestParameter("groupid");
		//List grouppermissions = new ArrayList();
		String searchtype = inReq.findValue("permissiontype");

		Searcher permsearcher = getSearcherManager().getSearcher(catalogid, searchtype);
		
		String id = inReq.getRequestParameter("id");
		if(searchtype != null && id != null){
		Data data = (Data)permsearcher.searchById(id);
		inReq.putPageValue("permdata", data);
		}
		String permissionpath = inReq.findValue("editPath");
		if( permissionpath == null)
		{
			permissionpath = "/" + catalogid + "/_site.xconf";
		}
		Page page = getPageManager().getPage(permissionpath);
		Permission perm = loadOrCreatePermission(page.getPageSettings(),permissionpath,id);
		inReq.putPageValue("permission", perm);

		HitTracker groups  = getUserManager(inReq).getGroups();
		Boolean simple = Boolean.TRUE;
		
		String usexml = inReq.getRequestParameter("addgroup");
		if( "xml".equals(usexml))
		{
			simple = Boolean.FALSE;
		}
		else
		{
			List selgroups = new ArrayList();
			if( perm != null)
			{
				Filter top = perm.getRootFilter();
				if( top != null)
				{
					if( top instanceof OrFilter)
					{
						Filter[] filters = top.getFilters();
						if( filters != null)
						{
							for (int j = 0; j < filters.length; j++)
							{
								if( filters[j] instanceof GroupFilter)
								{
									String gid = ((GroupFilter)filters[j]).getGroupId();
									Data group = (Group)getUserManager(inReq).getGroup(gid);
									if( group != null)
									{
										selgroups.add(group);
									}
								}
								else
								{
									simple = Boolean.FALSE;
								}
							}
						}
					} 
					else if( !(top instanceof BooleanFilter))
					{
						simple = Boolean.FALSE;
					}
				}			
			}
			inReq.putPageValue("selgroups", selgroups );
			groups.removeAll(selgroups);
		}
		inReq.putPageValue("issimple", String.valueOf(simple ));
		inReq.putPageValue("editPath", page);
		inReq.putPageValue("allgroups", groups );
		
	}

}
