package org.entermediadb.users;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.data.AddedPermission;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.cache.CacheManager;
import org.openedit.config.XMLConfiguration;
import org.openedit.data.BaseData;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.HitTracker;
import org.openedit.page.Page;
import org.openedit.page.Permission;
import org.openedit.page.PermissionSorter;
import org.openedit.users.Group;
import org.openedit.users.User;
import org.openedit.util.strainer.Filter;
import org.openedit.util.strainer.FilterReader;
import org.openedit.util.strainer.FilterWriter;
import org.openedit.util.strainer.GroupFilter;
import org.openedit.util.strainer.OrFilter;
import org.openedit.util.strainer.SettingsGroupFilter;

public class PermissionManager implements CatalogEnabled
{

	private static final Log log = LogFactory.getLog(PermissionManager.class);

	protected SearcherManager fieldSearcherManager;
	protected PermissionSorter fieldPermissionSorter;

	public CacheManager getCacheManager()
	{
		return fieldCacheManager;
	}

	public void setCacheManager(CacheManager inCacheManager)
	{
		fieldCacheManager = inCacheManager;
	}

	protected CacheManager fieldCacheManager;
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

			Collection items = getSearcherManager().getList(getCatalogId(), "permissionsapp");
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
		HashMap userpermissions = new HashMap();
		List permissions = null;
		if (limited == null)
		{
			permissions = inPage.getPermissions();
		}
		else
		{
			permissions = new ArrayList();
			String[] array = limited.split("\\s+");
			for (int i = 0; i < array.length; i++)
			{
				Permission permission = inPage.getPermission(array[i]);
				if (permission != null)
				{
					permissions.add(permission);
				}
			}
		}
		if (permissions != null)
		{
			Collections.sort(permissions, getPermissionSorter());

			for (Iterator iterator = permissions.iterator(); iterator.hasNext();)
			{
				Permission per = (Permission) iterator.next();
				boolean value = per.passes(inReq);
				inReq.putPageValue("can" + per.getName(), Boolean.valueOf(value));
				userpermissions.put("can" + per.getName(), Boolean.valueOf(value));
			}
		}
		inReq.putPageValue("permissionset", userpermissions);
	}

	public void loadModulePermissions(String inModuleid, String inParentFolderId, String inDataId, WebPageRequest inReq)
	{
		//Base module permissions. Module wide
		//TODO: Cache
		HitTracker<Data> modulepermissions = (HitTracker<Data>) getCacheManager().get("modulepermissions" + getCatalogId(), inModuleid);
		if (modulepermissions == null)
		{
			modulepermissions = getSearcherManager().query(getCatalogId(), "datapermissions").exact("permissiontype", inModuleid).search();
			getCacheManager().put("modulepermissions" + getCatalogId(), inModuleid, modulepermissions);
		}
		for (Iterator iterator = modulepermissions.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			Permission per = findPermission(inModuleid, inParentFolderId, inDataId, data.getId());
			if (per != null)
			{
				String permissionid = data.get("permission");
				if (permissionid == null)
				{
					permissionid = data.getId();
				}
				Boolean systemwide = (Boolean) inReq.getPageValue("can" + permissionid);
				if (systemwide == null || systemwide == false) //Option
				{
					boolean value = per.passes(inReq);
					if (value)
					{
						inReq.putPageValue("can" + permissionid, Boolean.valueOf(value));
						//log.info("added module permission: " + "can" + permissionid +  Boolean.valueOf(value));
					}
				}
			}
		}

		Collection custompermissions = loadCustomPermissionRules(inModuleid, inParentFolderId, inDataId);
		//	log.info("Checking : " + custompermissions );
		for (Iterator iterator = custompermissions.iterator(); iterator.hasNext();)
		{
			Permission per = (Permission) iterator.next();
			String permid = per.get("permissionid");
			Boolean systemwide = (Boolean) inReq.getPageValue("can" + permid);
			if (systemwide == null || !systemwide)
			{
				boolean value = per.passes(inReq);
				if (value)
				{
					inReq.putPageValue("can" + permid, Boolean.valueOf(value));
					//log.info("added custom permission: " + "can" + permid +  Boolean.valueOf(value));
				}
			}
		}
	}

	protected Collection loadCustomPermissionRules(String inDataType, String inParentFolderId, String inSpecificRow)
	{
		//Use a 5 min Cache
		Collection<Permission> rules = new ArrayList();
		HitTracker<Data> modulepermissions = (HitTracker<Data>) getCacheManager().get("moduleidpermissions" + getCatalogId(), inDataType);
		if (modulepermissions == null)
		{
			modulepermissions = getSearcherManager().query(getCatalogId(), "datapermissions").exact("moduleid", inDataType).search();
			getCacheManager().put("moduleidpermissions" + getCatalogId(), inDataType, modulepermissions);
		}

		//		HitTracker <Data> modulepermissions = getSearcherManager().query(getCatalogId(), "datapermissions").
		//				exact("moduleid", inDataType).search();
		//	log.info("searching based on " + inDataType +":"+":"+ inParentFolderId +":"+ inSpecificRow);
		//log.info(modulepermissions.getFriendlyQuery());
		//log.info(modulepermissions.getSearchQuery().toString());
		for (Iterator iterator = modulepermissions.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();

			//Specific Asset specific
			String id = data.get("permission");
			if (id == null)
			{
				id = data.getId();
			}
			Permission per = findPermission(inDataType, null, inSpecificRow, id);
			if (per == null)
			{
				//CollectionID specific
				per = findPermission(inDataType, inParentFolderId, null, id);
				if (per != null)
				{
					// log.info("WTF");
				}
			}
			if (per != null)
			{
				per.setValue("permissionid", id); //Needed?
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
		Data target = (Data) getSearcher("custompermissions").searchById(inPermission);
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

	public Permission findPermission(String inModule, String inFolder, String inData, String inPermissionId)
	{

		Searcher searcher = getSearcher("custompermissions");
		if (inFolder == null && inData == null)
		{
			return null;
		}

		String id = inModule + " " + inFolder + " " + inData + " " + inPermissionId;
		Data target = (Data) getCacheManager().get("custompermissions" + getCatalogId(), id);
		if (target == null)
		{
			//log.info("Loading custom permissions " + id);
			target = (Data) searcher.query().ignoreEmpty().exact("moduleid", inModule).exact("parentfolderid", inFolder).exact("dataid", inData).exact("datapermission", inPermissionId).searchOne();
			if (target == null)
			{
				target = BaseData.NULL;
			}
			getCacheManager().put("custompermissions" + getCatalogId(), id, target);
		}

		if (target != null && target != BaseData.NULL)
		{
			String xml = target.get("value");
			if (xml == null)
			{
				return null;
			}
			return getPermission(target.getId(), xml);
		}

		return null;

	}

	public void savePermission(Permission inPermission)
	{
		getCacheManager().clear("custompermissions");
		Searcher custompermissions = getSearcher("custompermissions");
		Data target = (Data) custompermissions.searchById(inPermission.getId());
		if (target == null)
		{
			target = custompermissions.createNewData();
			target.setId(inPermission.getId());
		}

		for (Iterator iterator = inPermission.getProperties().keySet().iterator(); iterator.hasNext();)
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

	public String toDisplay(MediaArchive inArchive, Permission inPermission)
	{
		StringBuffer buffer = new StringBuffer();
		Filter filter = inPermission.getRootFilter();
		renderFilter(inArchive, filter, buffer);
		return buffer.toString();
	}

	private void renderFilter(MediaArchive inArchive, Filter inFilter, StringBuffer inBuffer)
	{
		if (inFilter instanceof OrFilter)
		{
			OrFilter or = (OrFilter) inFilter;

			Filter[] filters = or.getFilters();
			if (filters == null || filters.length == 0)
			{
				inBuffer.append("false");
				return;
			}
			for (int i = 0; i < filters.length; i++)
			{
				if (i > 0)
				{
					inBuffer.append(" Or ");
				}
				//inBuffer.append("(");
				renderFilter(inArchive, filters[i], inBuffer);
				//inBuffer.append(")");
			}
		}
		else if (inFilter instanceof GroupFilter)
		{
			String gid = ((GroupFilter) inFilter).getGroupId();
			Group group = inArchive.getGroup(gid);
			if (group != null)
			{
				inBuffer.append(" Group = " + group.getName());
			}
		}
		else if (inFilter instanceof SettingsGroupFilter)
		{
			String gid = ((SettingsGroupFilter) inFilter).getGroupId();
			Data settings = inArchive.getData("settingsgroup", gid);
			if (settings != null)
			{
				inBuffer.append(" Role = " + settings.getName());
			}
		}
		else
		{
			inBuffer.append(inFilter.toString());
		}
	}


	public Map loadEntitySettingsGroupPermissions(String inEntityId, String inSettingsGroupId)
	{

		Map permissions = new HashMap();
		Searcher searcher = getSearcher("permissionentityassigned");
		HitTracker grouppermissions = searcher.query().exact("settingsgroup", inSettingsGroupId).exact("moduleid", inEntityId).exact("enabled", true).search();

		for (Iterator iterator = grouppermissions.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			permissions.put(data.get("permissionsentity"), data);
		}
		return permissions;
	}

	
	
	
	

	

	protected MediaArchive getMediaArchive()
	{
		return (MediaArchive) getSearcherManager().getModuleManager().getBean(getCatalogId(), "mediaArchive");
	}
	
	
//	public void updateEntityCategoryPermissions(Data inModule, MultiValued inEntity)
//	{
//		MediaArchive archive = getMediaArchive();
//		
//		Category entityCategory = archive.getEntityManager().loadDefaultFolder(inModule, inEntity, null);
//		
//		entityCategory.setValue("securityenabled", inEntity.getValue("securityenabled"));
//		entityCategory.setValue("viewusers", inEntity.getValue("customusers"));
//		entityCategory.setValue("viewgroups", inEntity.getValue("customgroups"));
//		entityCategory.setValue("viewroles", inEntity.getValue("customroles"));
//		archive.getCategorySearcher().saveCategory(entityCategory);
//		
//		
//	}

	
	public void checkEntityCategoryPermission(Data inModule, MultiValued inEntity)
	{
	    MediaArchive archive = getMediaArchive();
	    Category rootcat = archive.getEntityManager().loadDefaultFolder(inModule, inEntity, null);
	    if( rootcat == null)
	    {
	    	return;
	    }
	    boolean needsupdate = false;
	    String[] fieldsToCompare = {"users", "groups", "roles"};
	    for (String field : fieldsToCompare) {
	        // Get values from both the root category and the module
	        Collection<String> rootValues = rootcat.getValues( "viewer" + field);
	        Collection<String> combinedViewers = inEntity.getValues("viewer" + field);
	        // Normalize null values to empty collections
	        if (rootValues == null) {
	            rootValues = Collections.emptyList();
	        }
	        if (combinedViewers == null) {
	            combinedViewers = new ArrayList();
	        }
	        Collection editorValues = inEntity.getValues("editor" + field);
	        if( field.equals("users") )
	        {
		        String owner = inEntity.get("owner");
				if(owner != null)
				{
					if( !inEntity.containsValue("editor" + field, owner) )
					{
						inEntity.addValue("editor" + field, owner);
						editorValues = inEntity.getValues("editor" + field);
					}
				}
	        }

	        if( editorValues != null)
	        {
	        	combinedViewers.addAll(editorValues);
	        }
	        //Add in the owner
	        
	        // Compare values
	        if (!rootValues.containsAll(combinedViewers) || !combinedViewers.containsAll(rootValues)) {
	            log.info("Custom values found: " + rootValues + ", Module values: " + combinedViewers);
	            
	            needsupdate = true;
	        	rootcat.setValue("viewer" + field, combinedViewers);
	        }
	    }
	    if(needsupdate) {
			archive.getCategorySearcher().saveCategory(rootcat);
			archive.saveData(inModule.getId(), inEntity);
			
			//reindex all the submodules
			Collection enttiyviews = getMediaArchive().query("view").exact("moduleid", inModule.getId()).
					exact("systemdefined","false").exact("rendertype", "entitysubmodules").cachedSearch();
			
			for (Iterator iterator = enttiyviews.iterator(); iterator.hasNext();)
			{
				Data data = (Data) iterator.next();
				String searchtype = data.get("rendertable"); //remote
				
				String renderexternalid = data.get("renderexternalid");  //join
				
				String renderinternalid = data.get("renderinternalid");
				if (renderinternalid == null)
				{
					renderinternalid = inModule.getId(); //can be customized
				}
				String renderinternalvalue = inEntity.get(renderinternalid);
				if (searchtype != null && renderexternalid != null && renderinternalvalue != null)
				{
					HitTracker hits = getMediaArchive().query(searchtype).exact(renderexternalid, renderinternalvalue).search();
					for (int i = 0; i < hits.getTotalPages(); i++)
					{
						hits.setPage(i+1);
						Collection page = hits.getPageOfHits();
						getMediaArchive().saveData(searchtype, page); //Reindex
					}
				}
			}
		
			
			HitTracker assets =  archive.getAssetSearcher().query().exact("category", rootcat).search();
			assets.enableBulkOperations();
			if( assets.size() > 10000)
			{
				//Do full reindex
				log.error("Required full reindex");
			}
			else
			{
				archive.getAssetSearcher().saveAllData(assets, null);
				log.info("Reindexed " + assets.size() + " assets");
			}

	    }    
		
	}
	
	

	public void queuePermissionCheck(MultiValued inModule)
	{
	    MediaArchive archive = getMediaArchive();
	    log.info("Checkiog permissions on " + inModule);
	    
	    Category rootcat = archive.getEntityManager().loadDefaultFolderForModule(inModule, null);
	    
	    rootcat.setValue("customusers", inModule.getValue("viewusers"));
	    rootcat.setValue("customgroups", inModule.getValue("viewgroups"));
	    rootcat.setValue("customroles", inModule.getValue("viewroles"));
	    
	    archive.saveData("category", rootcat);
	    
	    boolean needsupdate = false;
	    String[] fieldsToCompare = {"users", "groups", "roles"};
	    for (String field : fieldsToCompare) {
	    	
	    	Collection<String> rootValues = rootcat.getValues( "viewer" + field);
	    	
	    	Collection combined = inModule.getValues("viewer" + field);
	    	
	        // Normalize null values to empty collections
	        if (rootValues == null) {
	            rootValues = new ArrayList();
	        }
	        if (combined == null) {
	        	combined = new HashSet();
	        }
	        else {
	        	combined = new HashSet(combined);
	        }

	    	if (inModule.getValues("editor" + field) != null)
		    {
		    	combined.addAll(inModule.getValues("editor" + field));
		    }

	        // Compare values
	        if (!rootValues.containsAll(combined) || !combined.containsAll(rootValues)) {
	            log.info("Mismatch found for field '" + field + "' in module " + inModule.getId());
	            log.info("Root Category Values: " + rootValues + ", Module Values: " + combined);
	            
	            needsupdate = true;
	     
	        }
	        
	    }
	    
	    
	    
//	    String rootSecurityEnabled = (String) rootcat.get("securityenabled");
//	    String moduleSecurityEnabled = (String) inModule.get("securityenabled");
//
//	    if ((rootSecurityEnabled == null && moduleSecurityEnabled != null) ||
//	        (rootSecurityEnabled != null && !rootSecurityEnabled.equals(moduleSecurityEnabled))) {
//	        log.info("Mismatch found for 'securityenabled' in module " + inModule.getId());
//	        log.info("Root Category Value: " + rootSecurityEnabled + ", Module Value: " + moduleSecurityEnabled);
//	        needsupdate = true;	        
//	    }
	    
	    
	    if(needsupdate) {
	    	inModule.setValue("permissionsupdateddate", new Date());
	    	archive.saveData("module", inModule);
	    	
	    }
	    archive.fireSharedMediaEvent("entities/checkpermissionhistory"); //handleModulePermissionsUpdated()
	}
	
	
	
	public void handleModulePermissionsUpdated()
	{

		MediaArchive archive = getMediaArchive();
		
		Searcher searcher = archive.getSearcher("permissionshistory");
				
		MultiValued lastruninfo = (BaseData) searcher.query().sort("dateDown").searchOne();
		Date lastrundate = null;
		if (lastruninfo == null)
		{
			lastrundate = new Date(0);
		}
		else
		{
			lastrundate = lastruninfo.getDate("date");
		}
		
		//This was saved by EntityModule when it noticed there is a change between the rootcategory persmissions and the module permission values
		HitTracker needupdate = archive.query("module").after("permissionsupdateddate", lastrundate).search();

		StringBuffer buffer = new StringBuffer();
		 
		for (Iterator iterator = needupdate.iterator(); iterator.hasNext();)
		{
			MultiValued module = (MultiValued) iterator.next();
			Date lastchangred =module.getDate("permissionsupdateddate");
			buffer.append("Module " + module.getId() + " Permissions being updated");			
			Category rootcat = archive.getEntityManager().loadDefaultFolderForModule(module, null);
			
			String[] fieldsToCompare = {"users", "groups", "roles"};
		    for (String field : fieldsToCompare) {
		    	
		    	Collection combined = module.getValues("viewer" + field);
		    	Collection editors = module.getValues("editor" + field);
		    	if (editors != null)
			    {
		    		if( combined == null)
		    		{
		    			combined = new ArrayList();
		    		}
			    	combined.addAll(editors);
			    }
		    	if (combined != null)
		    	{
		    		rootcat.setValue("viewer" + field, combined);
		    	}
		    	
		    	if (module.getValues("view" + field) != null)
			    {
		    		rootcat.setValue("custom" + field, module.getValues("view" + field) );
			    }
		    }
			
			archive.getCategorySearcher().saveCategoryTree(rootcat);
			
			Searcher modulesearcher = getSearcher(module.getId());
			
			HitTracker missingcategory = modulesearcher.query().missing("rootcategory").search();
			for (Iterator iterator2 = missingcategory.iterator(); iterator2.hasNext();)
			{
				Data data = (Data) iterator2.next();
				archive.getEntityManager().loadDefaultFolder(module, data, null);
			}
			
			modulesearcher.reIndexAll();
			
			buffer.append("Module " + module.getId() + " Permissions update completed");
			
			HitTracker assets =  archive.getAssetSearcher().query().exact("category", rootcat).search();
			assets.enableBulkOperations();
			if (assets.size() < 25000)
			{
				archive.getAssetSearcher().saveAllData(assets, null);
			}
			else
			{
				log.error("Must manually reindex asset table");
			}
			
		}
		if (!needupdate.isEmpty())
		{
			Data finishedinfo = searcher.createNewData();
			finishedinfo.setValue("date", new Date());
			finishedinfo.setValue("notes", buffer.toString());
			searcher.saveData(finishedinfo);
		}
	}

	
	public Collection<AddedPermission> loadEntityPermissions(Data inModule, Data inEntity)
	{
	    //Load all the view and edit record into a big list
	    Collection<AddedPermission> alladded = new ArrayList<AddedPermission>();
	    
	    Collection<String> editorsfound = collectUsers(inEntity,true);
	    Collection<String> viewersfound = collectUsers(inEntity,false);
	    
	    String owner = inEntity.get("owner");
	    if( owner != null)
	    {
	    	viewersfound.add(owner);
	    }
	    
	    viewersfound.removeAll(editorsfound);
	    addUsers(alladded,editorsfound,viewersfound);

	    
	    editorsfound = collectGroups(inEntity,true);
	    viewersfound = collectGroups(inEntity,false);
	    viewersfound.removeAll(editorsfound);
	    addGroups(alladded,editorsfound,viewersfound);

	    editorsfound = collectRoles(inEntity,true);
	    viewersfound = collectRoles(inEntity,false);
	    viewersfound.removeAll(editorsfound);
	    addRoles(alladded,editorsfound,viewersfound);

	    return alladded;
	    
	}
	
	public Collection<AddedPermission> loadParentPermissions(Data inModule, Data inEntity)
	{
	    //Load all the view and edit record into a big list
	    Collection<AddedPermission> alladded = new ArrayList<AddedPermission>();
	
		Category entiytycategory = getMediaArchive().getEntityManager().loadDefaultFolder(inModule, inEntity, null);
		Category category = entiytycategory.getParentCategory();
		Collection<String> empty = Collections.EMPTY_LIST;		
		
		Collection<String> viewersfound = category.collectValues("viewerusers"); //These are already combined from customusers
		
		Collection<String> more = category.collectValues("customusers"); //These are already combined from customusers
		viewersfound.addAll(more);
		
		addUsers(alladded,empty,viewersfound);
		
		Collection<String> moregroups = category.collectValues("viewergroups");
		more = category.collectValues("customgroups"); //These are already combined from customusers
		moregroups.addAll(more);
		addGroups(alladded,empty,moregroups);
		 
		Collection<String> moreroles = category.collectValues("viewerroles");
		more = category.collectValues("customroles"); 
		moreroles.addAll(more);
		addRoles(alladded,empty,moreroles);
		

	    return alladded;
	    
	}

	private void addUsers(Collection<AddedPermission> inAlladded, Collection<String> inEditorsfound, Collection<String> inViewersfound)
	{
		for (Iterator iterator = inEditorsfound.iterator(); iterator.hasNext();)
		{
			String userid = (String) iterator.next();
			User user = getMediaArchive().getUser(userid);
			AddedPermission added = new AddedPermission();
			added.setEditor(true);
			added.setData(user);
			added.setPermissionType("users");
			inAlladded.add(added);
		}
		for (Iterator iterator = inViewersfound.iterator(); iterator.hasNext();)
		{
			String userid = (String) iterator.next();
			User user = getMediaArchive().getUser(userid);
			AddedPermission added = new AddedPermission();
			added.setEditor(false);
			added.setData(user);
			added.setPermissionType("users");
			inAlladded.add(added);
		}
	}

	private void addRoles(Collection<AddedPermission> inAlladded, Collection<String> inEditorsfound, Collection<String> inViewersfound)
	{
		for (Iterator iterator = inEditorsfound.iterator(); iterator.hasNext();)
		{
			String id = (String) iterator.next();
			Data data = getMediaArchive().getCachedData("settingsgroup",id);
			AddedPermission added = new AddedPermission();
			added.setEditor(true);
			added.setData(data);
			added.setPermissionType("groups");
			inAlladded.add(added);
		}
		for (Iterator iterator = inViewersfound.iterator(); iterator.hasNext();)
		{
			String id = (String) iterator.next();
			Data data = getMediaArchive().getCachedData("settingsgroup",id);
			AddedPermission added = new AddedPermission();
			added.setEditor(false);
			added.setData(data);
			added.setPermissionType("roles");
			inAlladded.add(added);
		}
	}

	private void addGroups(Collection<AddedPermission> inAlladded, Collection<String> inEditorsfound, Collection<String> inViewersfound)
	{
		for (Iterator iterator = inEditorsfound.iterator(); iterator.hasNext();)
		{
			String id = (String) iterator.next();
			Group data = getMediaArchive().getGroup(id);
			AddedPermission added = new AddedPermission();
			added.setEditor(true);
			added.setData(data);
			added.setPermissionType("group");
			inAlladded.add(added);
		}
		for (Iterator iterator = inViewersfound.iterator(); iterator.hasNext();)
		{
			String id = (String) iterator.next();
			Group data = getMediaArchive().getGroup(id);
			AddedPermission added = new AddedPermission();
			added.setEditor(false);
			added.setData(data);
			added.setPermissionType("group");
			inAlladded.add(added);
		}	
	}

	protected Collection<String> collectUsers(Data inSource, boolean inEditors)
	{
		String fieldname = "viewerusers";
		if( inEditors )
		{
			fieldname = "editorusers";
		}
		Collection<String>  moreusers = inSource.getValues(fieldname);
		if( moreusers == null)
		{
			moreusers = new ArrayList(0);
		}
		return moreusers;
	}

	protected Collection<String> collectGroups(Data inSource, boolean inEditors)
	{
		String fieldname = "viewergroups";
		if( inEditors )
		{
			fieldname = "editorgroups";
		}
		Collection<String> 	moregroups = inSource.getValues(fieldname);
		if( moregroups == null)
		{
			moregroups = new ArrayList(0);
		}

		return moregroups;

	}

	protected Collection<String> collectRoles(Data inSource, boolean inEditors)
	{
		String fieldname = "viewerroles";
		if( inEditors )
		{
			fieldname = "editorroles";
		}
		Collection<String> more = inSource.getValues(fieldname);
		if( more == null)
		{
			more = new ArrayList(0);
		}
		return more;
	}

	public void addEntityPermissions(Data inModule, MultiValued inEntity, Map<String,String[]> inTosave)
	{
		for (Iterator iterator = inTosave.keySet().iterator(); iterator.hasNext();)
		{
			String  field = (String ) iterator.next();
			String[] values = inTosave.get(field);
			Collection all = Arrays.asList(values);
			inEntity.addValue(field,all);
		}
		
	}
	
}
