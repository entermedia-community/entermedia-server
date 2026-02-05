package org.entermediadb.elasticsearch.categories;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.xmldb.CategorySearcher;
import org.entermediadb.asset.xmldb.XmlCategoryArchive;
import org.entermediadb.elasticsearch.searchers.BaseElasticSearcher;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.ValuesMap;
import org.openedit.hittracker.HitTracker;
import org.openedit.users.User;
import org.openedit.util.PathUtilities;

public class ElasticCategorySearcher extends BaseElasticSearcher implements CategorySearcher//, Reloadable
{
	private static final Log log = LogFactory.getLog(ElasticCategorySearcher.class);
	protected XmlCategoryArchive fieldXmlCategoryArchive;
	//protected Category fieldRootCategory;
	protected String fieldSort = "name";
	
	
	
	
	public String getSort() {
		return fieldSort;
	}

	public void setSort(String inSort) {
		fieldSort = inSort;
	}



	
	protected String getCacheKey() {
		return getSearchType() + "category";
	}
	
	
	public Data createNewData()
	{
		
		String classname = getNewDataName();
		//elastcCategory has no empty contructor 
		if (classname == null || classname.equals("elasticCategory"))
		{
			return new ElasticCategory(this);
		}
		ElasticCategory cat =  (ElasticCategory) getModuleManager().getBean(getCatalogId(), getNewDataName(), false);
		cat.setCategorySearcher(this);
		return cat;
		

	}
//	protected Category refreshData(String inId, GetResponse response) 
//	{
//		ElasticCategory category = (ElasticCategory)createNewData();
//		category.setProperties(response.getSource());
//		
//		return category;
//	}

	public List findChildren(Category inParent) 
	{
		if(inParent== null || inParent.getId() == null) {
			return new ArrayList();
		}
		HitTracker hits = query().exact("parentid", inParent.getId()).sort(getSort()).search();
		hits.enableBulkOperations();
		List children = new ArrayList(hits.size());
		for (Iterator iterator = hits.iterator(); iterator.hasNext();) {
			Data data = (Data) iterator.next();
			
			ElasticCategory category = (ElasticCategory)getCacheManager().get(getCacheKey(), data.getId());
			if( category == null)
			{
				category  = (ElasticCategory)loadData(data);
				getCacheManager().put(getCacheKey(), data.getId(),category);
				children.add(category);
				continue;
			}
			category.setProperties(  data.getProperties() );
			category.refresh();
			children.add(category);
		}
		//Collections.sort(children);
		return children;
	}
	
	public void reindexInternal() throws OpenEditException
	{
		setReIndexing(true);
		try
		{
			//getXmlCategoryArchive().clearCategories();
			getCacheManager().clear(getCacheKey());
			
			HitTracker tracker = query().all().sort("categorypath").search();
			tracker.enableBulkOperations();
			
			List tosave = new ArrayList();
			for (Iterator iterator = tracker.iterator(); iterator.hasNext();)
			{
				Data hit = (Data) iterator.next();
				//log.info(hit.get("categorypath"));
				ElasticCategory data = (ElasticCategory)loadData(hit);
				tosave.add(data);
				if( tosave.size() > 1000)
				{
					updateIndex(tosave,null);
					tosave.clear();
					getCacheManager().clear(getCacheKey());  //TODO: Why do we do this?
				}
			}
			updateIndex(tosave,null);
			
			//Keep in mind that the index is about the clear so the cache will be invalid anyways since isDirty will be called
			getCacheManager().clear(getCacheKey());
		}
		finally
		{
			setReIndexing(false);
		}
	}

	public void reIndexAll() throws OpenEditException 
	{
		
		//there is not reindex step since it is only in memory
		if (isReIndexing())
		{
			return;
		}
		try
		{
			setReIndexing(true);
			setOptimizeReindex(false);
			putMappings(); //We can only try to put mapping. If this failes then they will
				//need to export their data and factory reset the fields 
			
			
			
			//getXmlCategoryArchive().clearCategories();
			getCacheManager().clear(getCacheKey());
			
/*			HitTracker tracker = query().all().sort("categorypath").search();
			tracker.enableBulkOperations();
			
			List tosave = new ArrayList();
			for (Iterator iterator = tracker.iterator(); iterator.hasNext();)
			{
				Data hit = (Data) iterator.next();
				//log.info(hit.get("categorypath"));
				ElasticCategory data = (ElasticCategory)loadData(hit);
				String path = data.loadCategoryPath();
				data.setValue("parents", data.getParentCategories());
				data.setValue("categorypath", path);
				
				tosave.add(data);
				if( tosave.size() > 1000)
				{
					updateIndex(tosave,null);
					tosave.clear();
					getCacheManager().clear(getCacheKey());
				}
			}
			updateIndex(tosave,null);
	*/
			

			saveCategory(getRootCategory());
	    	List tosave = new ArrayList(1000);
			saveCategoryTree(getRootCategory(),tosave, true);
			saveAllData(tosave, null);
			
			//Keep in mind that the index is about the clear so the cache will be invalid anyways since isDirty will be called
			getCacheManager().clear(getCacheKey());
			
			
			
			
		}
		finally
		{
			setReIndexing(false);
			setOptimizeReindex(true);
		}	
	}
	protected void updateChildren(Category inParent, List inTosave)
	{
		// TODO Auto-generated method stub
		inTosave.add(inParent);
		if( inTosave.size() == 1000)
		{
			updateIndex(inTosave,null);
			inTosave.clear();
		}
		for (Iterator iterator = inParent.getChildren().iterator(); iterator.hasNext();)
		{
			Category child = (Category) iterator.next();
			updateChildren(child,inTosave);
		}
	}
	
//	@Override
//	public boolean shoudSkipField(String inKey)
//	{
//		boolean skip = super.shoudSkipField(inKey);
//		if( skip || inKey.equals("parents") || inKey.equals("categorypath"))
//		{
//			return true; 
//		}
//		return false;
//	}
	
	@Override
	protected void saveToElasticSearch(PropertyDetails inDetails, Data inData, boolean delete, User inUser)
	{
		ElasticCategory category = null;
		if( inData instanceof ElasticCategory)
		{
			category = (ElasticCategory)inData;
		}
		else
		{
			category = (ElasticCategory)loadData(inData);
		}
		super.saveToElasticSearch(inDetails,inData, delete,inUser);
		Collection values = (Collection)category.getProperties().getValue("parents");
		boolean edited = false;
		if( values == null)
		{
			category.setValue("parents", category.getParentCategories()); //This requires the ID of the asset to be set before saving
			edited = true;
		}	
		
		String path = (String)category.getProperties().getValue("categorypath");
		if( path == null)
		{
			path = category.loadCategoryPath();
			category.setValue("categorypath", path);
			edited = true;
		}	
		if( edited )
		{
			super.saveToElasticSearch(inDetails,inData, delete, inUser);
		}
		
	}
	@Override
	public Category getRootCategory()
	{
		Category root = getCategory("index");
		
		if( root == null)
		{
			//root = getXmlCategoryArchive().getRootCategory();
			fieldXmlCategoryArchive = null;
			if( root == null)
			{
				root = (Category)createNewData();
				root.setId("index");
				root.setName("All");
			}
			else
			{
				root = (Category)loadData(root);
			}
			List tosave = new ArrayList();
			saveCategoryTree(root,tosave, false);
	   		saveAllData(tosave, null);
			//We are going to create a database tool to import categories.xml
		}
		
		return root;
	}
	
    public void saveCategoryTree(Category inRootCategory)
	{
    	/*
    	 * 
    	HitTracker all = query().exact("parents", inRootCategory).search();
    	all.enableBulkOperations();
		saveAllData(all, null);
    	 * */
    	saveCategory(inRootCategory);
    	List tosave = new ArrayList(1000);
		saveCategoryTree(inRootCategory,tosave, false);
		saveAllData(tosave, null);
	}
    protected void saveCategoryTree(Category inCategory, List toSave, Boolean inReloadChildren)
   	{
   		//saveData(inRootCategory, null);
    	
		String path = inCategory.loadCategoryPath();
		inCategory.setValue("parents", inCategory.getParentCategories());
		inCategory.setValue("categorypath", path);
		getCacheManager().put(getCacheKey(), inCategory.getId(),inCategory); //Is this too many?
    	toSave.add(inCategory);
    	if( toSave.size() > 1000 )
   		{
   			saveAllData(toSave, null);
   			toSave.clear();
   		}
		Collection children = inCategory.getChildren(inReloadChildren);
		if( children != null)
		{
	   		for (Iterator iterator = children.iterator(); iterator.hasNext();)
	   		{
	   			Category child = (Category) iterator.next();
	   			saveCategoryTree(child, toSave, false);
	   		}
    	}
   		
   	}
	//	public CategoryArchive getCategoryArchive()
//	{
//		if(fieldCategoryArchive != null){
//			fieldCategoryArchive.setCatalogId(getCatalogId());
//		}
//		return fieldCategoryArchive;
//	}
//	public void setCategoryArchive(CategoryArchive inCategoryArchive)
//	{
//		fieldCategoryArchive = inCategoryArchive;
//		inCategoryArchive.setCatalogId(getCatalogId());
//	}
	@Override
	public Category getCategory(String inCategoryId)
	{
		if(inCategoryId == null) {
			return null;
		}
		Category cat = null;		
		cat = (Category)getCacheManager().get(getCacheKey(), inCategoryId);
		if( cat == null || cat.isDirty() )
		{
			Category newcopy = searchCategory(inCategoryId);
			if(newcopy == null) {
				return null;
			}
			if( cat == null)
			{
				cat = newcopy;
			}
			else
			{
				cat.setProperties(  newcopy.getProperties() );
				cat.refresh();
				//cat.setParentCategory(null); cant call this
			}
//			if( cat != null)
//			{
//				log.info("loading category:"  + cat.getName() );
//			}	
		}
		//log.info("returning" + cat.hashCode() + " " + cat.getName());
		if( cat != null && !cat.hasLoadedParent())
		{
			String parentid = (String)cat.get("parentid");
			if( parentid != null && !parentid.equals(inCategoryId))
			{
				Category parent = getCategory(parentid);
				if( parent != null )
				{
					cat.setParentCategory(parent);
				}
				else
				{
					log.error("Missing parent category " + parentid + " on child " + inCategoryId);
				}
			}
		}
		if( cat != null )
		{
			getCacheManager().put(getCacheKey(), inCategoryId,cat);
		}

		return cat;
	}
	
	public Object searchByField(String inField, String inValue)
	{
		if( inField.equals("id") || inField.equals("_id"))
		{
			return getCategory(inValue);
		}
		return super.searchByField(inField, inValue);
	}

	protected Category searchCategory(String inValue)
	{
		GetResponse response = getClient().prepareGet(toId(getCatalogId()), getSearchType(), inValue).execute().actionGet();
		if( response.isExists() )
		{
			Map source = response.getSource();
			if( isDeleted(source))
			{
				return null;
			}
			ElasticCategory data = (ElasticCategory) createNewData();
			data.setProperties(new ValuesMap(source));
			//data.
			//copyData(data, typed);
			data.setId(inValue);
			if( response.getVersion() > -1)
			{
				data.setProperty(".version",String.valueOf(response.getVersion()) );
			}
			return data;
		}
		return null;
	}

	@Override
	public Data loadData(Data inHit)
	{
		if( inHit == null || inHit instanceof ElasticCategory )
		{
			return inHit;
		}
		ElasticCategory data = (ElasticCategory) createNewData();
		
		data.setProperties(inHit.getProperties());
		data.setId(inHit.getId());
		return data;
		//return super.loadData(inHit);
	}
	
	@Override
	public void saveCategory(Category inCategory)
	{
		String path = inCategory.loadCategoryPath();
		inCategory.setValue("parents", inCategory.getParentCategories());
		inCategory.setValue("categorypath", path);
		saveData(inCategory, null);
		getCacheManager().put(getCacheKey(), inCategory.getId(),inCategory); //Is this too many?
		
		//log.info("saved" + inCategory.hashCode() + " " + inCategory.getName());
	}
	
	public void saveData(Data inData, User inUser)
	{
		//For the path to be saved we might need to force category?
		super.saveData(inData, inUser);
		setIndexId(-1);
//		cat = (ElasticCategory)cat.getParentCategory();
//		if( cat == null)
//		{
//			cat.setChildren(null); //force a reload?
//		}
	}
	
	
	@Override
	public void delete(Data inData, User inUser) {
		// TODO Auto-generated method stub
		super.delete(inData, inUser);
		getCacheManager().remove(getCacheKey(), inData.getId() );
		setIndexId(-1);
	}

	protected String createCategoryId(String inPath)
	{
		// subtract the start /store/assets/stuff/more -> stuff_more
		if (inPath.length() < 0)
		{
			return "index";
		}

		if (inPath.startsWith("/"))
		{
			inPath = inPath.substring(1);
		}
		inPath = inPath.replace('/', '_');
		String id = PathUtilities.extractId(inPath, true);
		return id;
	}
	
	@Override
	public Category createCategoryPathFromParent(Category inParent, String inChildPath)
	{
		String categorypath = inParent.get("categorypath");
		if (categorypath != null && inParent.hasLoadedParent())   //Data error check
		{
			String existingcategorypath = inParent.loadCategoryPath();
			if (!categorypath.equals(existingcategorypath)) 
			{
				throw new OpenEditException("Parent category needs to be reindex"); 
			}
		}
		//Clean up
//		if (inChildPath.startsWith("/"))
//		{
//			inChildPath  = inChildPath.substring(1);
//		}
//		if (inChildPath.endsWith("/"))
//		{
//			inChildPath  = inChildPath.substring(0,inChildPath.length()-1);
//		}
		Category childcategory = null;
		String[] children = inChildPath.split("\\/");
		Category currentparent = inParent;
		for (int i = 0; i < children.length; i++)
		{
			String catname = children[i];
			childcategory = currentparent.getChildByName(catname); //speed up but children could be out of date if they renamed it
			if( childcategory == null)
			{
				//Create it
				childcategory = (Category)createNewData();
				childcategory.setName(inChildPath);
				//create parents and itself
				childcategory.setParentCategory(currentparent);
				currentparent.setIndexId("-1"); //reload sometime
				saveData(childcategory);
				getCacheManager().put(getCacheKey(), childcategory.getId(),childcategory);
			}
			currentparent = childcategory;
			
		}
		
		return childcategory;
	}
	
	@Override
	public synchronized Category createCategoryPath(String inPath)
	{
		Category found = loadCategoryByPath(inPath);
		if( found == null)
		{
			//Was not in cache so create it
			String cleanpath = null;
			if (inPath.endsWith("/"))
			{
				cleanpath  = inPath.substring(0,inPath.length()-1);
			}
			else
			{
				cleanpath  = inPath;
			}
			if( cleanpath.isEmpty())
			{
				throw new OpenEditException("Blank path");
			}
			//log.info("Category not found: "+ inPath);
			found = (Category)createNewData();
			String name = PathUtilities.extractFileName(cleanpath);
			found.setName(name);
			//create parents and itself
			String parent = PathUtilities.extractDirectoryPath(cleanpath);
			Category parentcategory = createCategoryPath(parent);
			if( parentcategory != null)
			{
				//log.info(parentcategory.isDirty());
				
				//This could be slow to load on a big catalog
				//parentcategory.addChild(found);
				found.setParentCategory(parentcategory);
				parentcategory.setIndexId("-1"); //reload sometime
				//log.info(parentcategory.isDirty());
			}
			saveData(found);
			getCacheManager().put(getCacheKey(), found.getId(),found);
		}
		return found;
	}
	
	
	public void deleteCategoryTree(Category root){
		for (Iterator iterator = root.getChildren().iterator(); iterator.hasNext();)
		{
			Category child = (Category) iterator.next();
			deleteCategoryTree(child);
			
		}
		delete(root, null);
	}

	@Override
	public void clearCategories()
	{
		clearIndex();
		getCacheManager().clear(getCacheKey());
		
	}
	public Set buildCategorySet(Category inCategory) {
		List categories = new ArrayList();
		categories.add(inCategory);
		
		return buildCategorySet(categories);
	}
	public Set buildCategorySet(List inCategories) {
		HashSet allCatalogs = new HashSet();
		// allCatalogs.addAll(catalogs);
		for (Iterator iter = inCategories.iterator(); iter.hasNext();) {
			Category catalog = (Category) iter.next();
			buildCategorySet(catalog, allCatalogs);
		}
		return allCatalogs;
	}

	protected void buildCategorySet(Category inCategory, Set inCategorySet) {
		if(inCategory==null) {
			return;
		}
		inCategorySet.add(inCategory);
		Category parent = inCategory.getParentCategory();
		if (parent != null) {
			buildCategorySet(parent, inCategorySet);
		}
	}
	public Category loadCategoryByPath(String categorypath)
	{
		if( categorypath.length() == 0 || categorypath.equals(getRootCategory().getName()))
		{		
			return getRootCategory();
		}
		//TODO: Cache these categories
		
		
		//TODO: Find right way to do this not matches
		categorypath = categorypath.replace('\\', '/'); //Unix paths
		
		categorypath = categorypath.replace("//", "/"); //Double Slash
		
		if (categorypath.endsWith("/"))
		{
			categorypath = categorypath.substring(0,categorypath.length()-1);
		}
		Data hit = (Data)query().exact("categorypath", categorypath).sort("categorypathUp").searchOne();
		if( hit == null)
		{
			return null;
		}
		Category cached = (Category)getCacheManager().get(getCacheKey(), hit.getId());
		if( cached != null)
		{
			return cached;
		}
		
		Category found = (Category)loadData(hit);
		getCacheManager().put(getCacheKey(), found.getId(),found);
		return found;
	}

	public List listAllCategories(Category inTopCategory)
	{
		if(inTopCategory == null) {
			return null;
		}
		List all = new ArrayList(300);
		addChildren(inTopCategory,all);
		return all;
	}
	
	protected void addChildren(Category parent,List all)
	{
		all.add(parent);
		if( parent.hasChildren())
		{
			for (Iterator iterator = parent.getChildren().iterator(); iterator.hasNext();) {
				Category category = (Category ) iterator.next();
				addChildren(category,all);
			}
		}
	}
	
	protected void addSecurity(XContentBuilder inContent, Data inData) throws Exception
	{
		//Check for security
		PropertyDetail detail = getDetail("securityenabled");

		if (detail == null)
		{
			return;
		}
		
		Category cat = null;
		
		if( inData instanceof Category)
		{
			cat = (Category)inData;
		}
		else
		{
			cat = (Category)loadData(inData);
		}

		Collection viewusers = cat.collectValues("viewerusers");
		Collection viewgroups = cat.collectValues("viewergroups");
		Collection viewroles = cat.collectValues("viewerroles");
		
		if( cat.getValues("customusers") != null)
		{
			viewusers.addAll( cat.getValues("customusers"));
		}
		if( cat.getValues("customgroups") != null)
		{
			viewgroups.addAll( cat.getValues("customgroups"));
		}
		if( cat.getValues("customroles") != null)
		{
			viewroles.addAll( cat.getValues("customroles"));
		}
		
		if( !viewusers.isEmpty())
		{
			inContent.field("viewusers", viewusers);
		}
		if (!viewgroups.isEmpty())
		{
			inContent.field("viewgroups", viewgroups);
		}
		if (!viewroles.isEmpty())
		{
			inContent.field("viewroles", viewroles);
		}

		
		if( !viewusers.isEmpty() || !viewgroups.isEmpty() || !viewroles.isEmpty() )
		{
			inContent.field("securityenabled", true);
		}
		else
		{
			inContent.field("securityenabled", false);
		}
		
				
		

	}
	
	
}
