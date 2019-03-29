package org.entermediadb.asset.modules;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.users.PermissionManager;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.config.Configuration;
import org.openedit.config.XMLConfiguration;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.page.Permission;
import org.openedit.users.Group;
import org.openedit.util.strainer.BooleanFilter;
import org.openedit.util.strainer.Filter;
import org.openedit.util.strainer.FilterReader;
import org.openedit.util.strainer.GroupFilter;
import org.openedit.util.strainer.OrFilter;
import org.openedit.util.strainer.SettingsGroupFilter;

public class DataPermissionModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(DataPermissionModule.class);

	
	public void loadCustomModulePermissions(WebPageRequest inReq) 
	{
		String moduleid = inReq.findValue("module"); //librarycolleciton
		String catid = inReq.findValue("catalogid");
		if (catid == null)
		{
			catid = "system";
		}
		String parametername = inReq.findValue("parameterid"); //librarycolleciton
		if( parametername != null)
		{
			String id = inReq.getRequestParameter(parametername);
			
				PermissionManager manager = getPermissionManager(catid);
				String parentparameterid = inReq.findValue("parentparameterid"); //librarycolleciton
				String parentvalue = inReq.getRequestParameter(parentparameterid);
				String parantparametertype = inReq.findValue("parametertype");
				if(parantparametertype == null) {
					parantparametertype=moduleid;
				}
				
				if(parentvalue == null && parentparameterid != null) {
					Data target = getMediaArchive(catid).getData(parantparametertype, id);
					if(target != null) {
						parentvalue = target.get(parentparameterid);
					}
					
					
				}
				
				
				manager.loadModulePermissions(moduleid,parentvalue,id,inReq);
			
		}
		
	}



	protected PermissionManager getPermissionManager(String catid)
	{
		PermissionManager manager = (PermissionManager) getModuleManager().getBean(catid, "permissionManager");
		return manager;
	}
	

	
	public Permission loadPermission(WebPageRequest inReq) throws Exception
	{
		String id = inReq.getRequestParameter("currentpermission");
		String datapermission = inReq.findValue("datapermission");
		MediaArchive archive = getMediaArchive(inReq);
			
		
			Permission permission = loadOrCreatePermission(archive,id,id); 
			if(datapermission != null) {
				permission.setValue("datapermission", datapermission);
			}
			inReq.putPageValue("permission", permission);
			return permission;
		
	}
	
	private Permission loadOrCreatePermission(MediaArchive inArchive,  String id, String inName)
	{
		PermissionManager manager = getPermissionManager(inArchive.getCatalogId());

		Permission permission = manager.getPermission(id);// need stuff here...
		if( permission == null )
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
			if(id != null) {
			per.setId(id);
			}
			permission = per;
		}
		return permission;
	}

//	public void loadPermissions(WebPageRequest inReq) throws Exception
//	{
//		String path = inReq.getRequestParameter("editPath");
//		if( path == null )
//		{
//			path = inReq.findValue("editPath");
//		}
//		//System.out.println(path);
//
//		PageSettings settings = getPageManager().getPageSettingsManager().getPageSettings(path);
//		List localPermissions = settings.getFieldPermissions();
//		List parentPermissions = settings.getPermissions(false);
//
//		List combined = new ArrayList();
//		if( localPermissions != null)
//		{
//			
//			Collections.sort(localPermissions);
//			combined.addAll(localPermissions);
//		}
//		Collections.sort(parentPermissions);
//		combined.addAll(parentPermissions);
//		//inReq.putPageValue("permissions", localPermissions);
//		inReq.putPageValue("permissions", combined);
//		inReq.putPageValue("settings", settings);
//		inReq.putPageValue("editPath", path);
//	}
	
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
		MediaArchive archive = getMediaArchive(inReq);
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
		savePermission(archive, permission);
		
	}

	protected void savePermission(MediaArchive archive, Permission permission) throws OpenEditException
	{
		PermissionManager manager = getPermissionManager(archive.getCatalogId());

		manager.savePermission(permission);
	}
	
	public void removeCondition(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
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
			savePermission(archive, permission);
		}
	}

//	public void addPermission(WebPageRequest inReq) throws Exception
//	{
//		String path = inReq.getRequestParameter("editPath");
//		String name = inReq.getRequestParameter("name");
//		Permission permission = new Permission();
//		permission.setName(name);
//		if (path != null)
//		{
//			permission.setPath(path);
//			savePermission(permission);
//		}
//		inReq.putPageValue("selectedpermission", permission);
//		inReq.putPageValue("permission", permission);
//		loadPermissions(inReq);
//	}
	
//	public void removePermission(WebPageRequest inReq) throws Exception
//	{
//		//String path = inReq.getRequestParameter("editPath");
//		Permission permission = loadPermission(inReq);
//
//		if ( permission != null)
//		{
//			Page page = getPageManager().getPage(permission.getPath(),true);
//			PageSettings settings = page.getPageSettings();
//			settings.removePermission(permission);
//			getPageManager().saveSettings(page);
//			getPageManager().clearCache(page);
//		}
//	}
//	
	public void resetPermission(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);

		String id = inReq.getRequestParameter("id");
		if(id != null) {
			Searcher searcher = archive.getSearcher("custompermissions");
			Data perm = (Data) searcher.searchById(id);
			if(perm != null) {
			searcher.delete(perm, inReq.getUser());
			}
		}
		
		
	}
	public void addGroup(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);

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
			String fields[] =inReq.getRequestParameters("field");
			archive.getSearcher("custompermissions").updateData(inReq, fields, permission);
			
			savePermission(archive, permission);
			inReq.putPageValue("permission", permission);
			String id = permission.getId();
			inReq.setRequestParameter("currentpermission", id);
		}
	}	
//	public void addCondition(WebPageRequest inReq) throws Exception
//	{
//		String path = inReq.getRequestParameter("editPath");
//		String name = inReq.getRequestParameter("name");
//		String traverse = inReq.getRequestParameter("traverse");
//		String type = inReq.getRequestParameter("conditiontype");
//		String id = null;
//		
//		Data conditiondetail =  getMediaArchive(inReq).getData("conditiontypes" , type);
//		 if(conditiondetail !=  null && conditiondetail.get("type") != null){
//			 id=type;
//			 type = conditiondetail.get("type");
//			 
//		 }
//		
//		FilterReader reader = (FilterReader)getModuleManager().getBean("filterReader");
//		
//		if( name != null)
//		{
//			Permission permission = loadPermission(inReq);
//			if( permission != null)
//			{
//				Configuration config = new XMLConfiguration();
//				config.addChild(new XMLConfiguration(type));
//				
//				Filter newFilter = reader.readFilterCollection(config, name);
//				if (permission.getRootFilter() == null)
//				{
//					permission.setRootFilter(newFilter);
//				}
//				else if( traverse != null && traverse.length() > 0)
//				{
//					String[] tree = traverse.split("/");
//					int[] list = makeInts(tree);
//					Filter parent = permission.findCondition(list);
//					parent.addFilter(newFilter);
//				}
//				else
//				{
//					permission.getRootFilter().addFilter(newFilter);
//				}
//				if("action".equals(type)){
//					String action =  conditiondetail.get("method");
//					newFilter.setProperty("name", action);
//					newFilter.setProperty("conditiontype",id );
//				}
//				Page page = getPageManager().getPage(path,true);
//				page.getPageSettings().setProperty("encoding","UTF-8");
//				page.getPageSettings().addPermission(permission);
//				
//				getPageManager().saveSettings(page);
//				getPageManager().clearCache(page);
//			}
//		}
//	}
	
	private int[] makeInts(String[] tree)
	{
		int[] list = new int[tree.length];
		for (int i = 0; i < list.length; i++)
		{
			list[i] = Integer.parseInt(tree[i]);
		}
		return list;
	}
	
	

	public void loadPermissionsByType(WebPageRequest inReq)
	{
		
		String permissiontype = inReq.findValue("permissiontype");
		if(permissiontype == null) {
			permissiontype = inReq.findValue("searchtype");

		}
		if(permissiontype == null) {
			permissiontype = inReq.findValue("moduleid");

		}
		MediaArchive archive = getMediaArchive(inReq);
		
		//we are going to load a searcher and a list of permissions
		HitTracker hits  = archive.getSearcher("datapermissions").query().exact("permissiontype", permissiontype).sort("ordering").search();
		inReq.putPageValue("permissions", hits);
		//this will get shown with edit button

		
	}	
	
	public void loadPermissionForEdit(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);

		String permissiontype = inReq.findValue("permissiontype");
		if(permissiontype == null) {
			permissiontype = inReq.findValue("searchtype");

		}
		if(permissiontype == null) {
			permissiontype = inReq.findValue("moduleid");

		}

		
	
		String dataid = inReq.findValue("dataid");
		if(permissiontype != null && dataid != null) {
			Data data = archive.getData(permissiontype, dataid);
			if(data != null) {
				inReq.putPageValue("data",data);
			}
		}
		
		String permissionid = inReq.getRequestParameter("permissionid");
		String currentid = inReq.getRequestParameter("currentpermission");
		Data target =archive.getData("datapermissions",  permissionid);
		inReq.putPageValue("permdata", target);
		
		Permission perm = loadOrCreatePermission(archive,currentid,currentid);
		inReq.putPageValue("permission", perm);
		inReq.putPageValue("permissionid", permissionid);

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
			List selroles = new ArrayList();
			
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
								} else if(filters[j] instanceof SettingsGroupFilter)
								{
									String gid = ((SettingsGroupFilter)filters[j]).getGroupId();
									Data group = (Group)getUserManager(inReq).getGroup(gid);
									if( group != null)
									{
										selroles.add(group);
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
			inReq.putPageValue("selroles", selroles );

			groups.removeAll(selgroups);
		}
		inReq.putPageValue("issimple", String.valueOf(simple ));
		inReq.putPageValue("allgroups", groups );
		
	}
	
	public void addCondition(WebPageRequest inReq) throws Exception
	{
		Permission permission = loadPermission(inReq);

		String id = permission.getId();
		String traverse = inReq.getRequestParameter("traverse");
		String type = inReq.getRequestParameter("conditiontype");
		MediaArchive archive = getMediaArchive(inReq);
		
		Data conditiondetail =  getMediaArchive(inReq).getData("conditiontypes" , type);
		 if(conditiondetail !=  null && conditiondetail.get("type") != null){
			 id=type;
			 type = conditiondetail.get("type");
			 
		 }
		
		FilterReader reader = (FilterReader)getModuleManager().getBean("filterReader");
		
		if( permission != null)
		{
			if( permission != null)
			{
				Configuration config = new XMLConfiguration();
				config.addChild(new XMLConfiguration(type));
				
				Filter newFilter = reader.readFilterCollection(config, id);
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
			
			}
			PermissionManager manager = getPermissionManager(archive.getCatalogId());

			manager.savePermission(permission);
			
			
		}
	}
	
	
	public void loadPermissions(WebPageRequest inReq) {
//		String permissiontype = inReq.findValue("permissiontype");
//		MediaArchive archive = getMediaArchive(inReq);
//		HitTracker <Data> permissions = archive.query("datapermissions").exact("permissiontype", permissiontype).search();
//		for (Iterator iterator = permissions.iterator(); iterator.hasNext();)
//		{
//			Data permission = (Data) iterator.next();
//			
//			Permission per = archive.getPermission(permission.getId());
//			if(per != null) {
//			boolean value = per.passes(inReq);
//			inReq.putPageValue("can" + per.getName(), Boolean.valueOf(value));
//			}	
//			
//		}
//		
	}

}
