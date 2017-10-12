package org.entermediadb.elasticsearch.categories;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

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
import org.openedit.cache.CacheManager;
import org.openedit.data.PropertyDetails;
import org.openedit.hittracker.HitTracker;
import org.openedit.users.User;
import org.openedit.util.PathUtilities;

public class ElasticCategorySearcher extends BaseElasticSearcher implements CategorySearcher//, Reloadable
{
	private static final Log log = LogFactory.getLog(ElasticCategorySearcher.class);
	protected XmlCategoryArchive fieldXmlCategoryArchive;
	//protected Category fieldRootCategory;
	protected CacheManager fieldCacheManager;
	
	public CacheManager getCacheManager()
	{
		return fieldCacheManager;
	}

	public void setCacheManager(CacheManager inCacheManager)
	{
		fieldCacheManager = inCacheManager;
	}

	public XmlCategoryArchive getXmlCategoryArchive()
	{
		if( fieldXmlCategoryArchive == null)
		{
			fieldXmlCategoryArchive = (XmlCategoryArchive)getModuleManager().getBean(getCatalogId(),"xmlCategoryArchive");
		}
		return fieldXmlCategoryArchive;
	}

	public void setXmlCategoryArchive(XmlCategoryArchive inXmlCategoryArchive)
	{
		fieldXmlCategoryArchive = inXmlCategoryArchive;
	}

	
	public Data createNewData()
	{
		return new ElasticCategory(this);
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
		Collection hits = query().exact("parentid", inParent.getId()).search();
		List children = new ArrayList(hits.size());
		for (Iterator iterator = hits.iterator(); iterator.hasNext();) {
			Data data = (Data) iterator.next();
			
			ElasticCategory category = (ElasticCategory)getCacheManager().get("category", data.getId());
			if( category == null)
			{
				category  = (ElasticCategory)createNewData();
				getCacheManager().put("category", data.getId(),category);
			}
			category.setId(data.getId());
			category.setProperties(data.getProperties());
			category.setParentCategory(inParent);
			children.add(category);
		}
		Collections.sort(children);
		return children;
	}
	
	public void reindexInternal() throws OpenEditException
	{
		getXmlCategoryArchive().clearCategories();
		getCacheManager().clear("category");
		
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
				getCacheManager().clear("category");
			}
		}
		updateIndex(tosave,null);
		
		//Keep in mind that the index is about the clear so the cache will be invalid anyways since isDirty will be called
		getCacheManager().clear("category");
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
		
			putMappings(); //We can only try to put mapping. If this failes then they will
				//need to export their data and factory reset the fields 
			
			
			
			getXmlCategoryArchive().clearCategories();
			getCacheManager().clear("category");
			
			HitTracker tracker = query().all().sort("categorypath").search();
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
					getCacheManager().clear("category");
				}
			}
			updateIndex(tosave,null);
			
			//Keep in mind that the index is about the clear so the cache will be invalid anyways since isDirty will be called
			getCacheManager().clear("category");
			
			
			
			
		}
		finally
		{
			setReIndexing(false);
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
	protected void updateElasticIndex(PropertyDetails details, Data inData)
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
		super.updateElasticIndex(details,inData);
		Collection values = (Collection)category.getMap().getValue("parents");
		boolean edited = false;
		if( values == null)
		{
			category.setValue("parents", category.getParentCategories()); //This requires the ID of the asset to be set before saving
			edited = true;
		}	
		
		String path = (String)category.getMap().getValue("categorypath");
		if( path == null)
		{
			path = category.loadCategoryPath();
			category.setValue("categorypath", path);
			edited = true;
		}	
		if( edited )
		{
			super.updateElasticIndex(details,inData);
		}
		
	}
	@Override
	public Category getRootCategory()
	{
		Category root = getCategory("index");
		
		if( root == null)
		{
			root = getXmlCategoryArchive().getRootCategory();
			fieldXmlCategoryArchive = null;
			if( root == null)
			{
				root = (Category)createNewData();
				root.setId("index");
				root.setName("Index");
			}
			else
			{
				root = (Category)loadData(root);
			}
			List tosave = new ArrayList();
			saveCategoryTree(root,tosave);
	   		saveAllData(tosave, null);
			//We are going to create a database tool to import categories.xml
		}	
		return root;
	}
	
    protected void saveCategoryTree(Category inRootCategory)
	{
		saveData(inRootCategory, null);
		for (Iterator iterator = inRootCategory.getChildren().iterator(); iterator.hasNext();)
		{
			Category child = (Category) iterator.next();
			saveCategoryTree(child);
		}
	}
    protected void saveCategoryTree(Category inRootCategory, List toSave)
   	{
   		//saveData(inRootCategory, null);
    	toSave.add(inRootCategory);
    	if( toSave.size() > 1000 )
   		{
   			saveAllData(toSave, null);
   			toSave.clear();
   		}
   		for (Iterator iterator = inRootCategory.getChildren().iterator(); iterator.hasNext();)
   		{
   			Category child = (Category) iterator.next();
   			saveCategoryTree(child, toSave);
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
		Category cat = null;
		cat = (Category)getCacheManager().get("category", inCategoryId);
		if( cat == null || cat.isDirty() )
		{
			cat = searchCategory(inCategoryId);
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
			getCacheManager().put("category", inCategoryId,cat);
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
			ElasticCategory data = (ElasticCategory) createNewData();
			data.setProperties(response.getSource());
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
		if( inHit == null || inHit instanceof Category )
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
		saveData(inCategory, null);
		getCacheManager().put("category", inCategory.getId(),inCategory);
		//log.info("saved" + inCategory.hashCode() + " " + inCategory.getName());
	}
	
	public void saveData(Data inData, User inUser)
	{
		//For the path to be saved we might need to force category?
		
		super.saveData((Category)inData, inUser);
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
		getCacheManager().remove("category", inData.getId() );
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
	public Category createCategoryPath(String inPath)
	{
		if( inPath.length() == 0 || inPath.equals("Index"))
		{		
			return getRootCategory();
		}
		//TODO: Find right way to do this not matches
		inPath = inPath.replace('\\', '/'); //Unix paths
		Data hit = (Data)query().exact("categorypath", inPath).sort("categorypathUp").searchOne();
//		if( hit == null)
//		{
//			//String id = createCategoryId(inPath);
//			//hit = (Data)searchById(id);  //May result in false positive
//		}
		Category found = (Category)loadData(hit);
		if( found == null)
		{
			found = (Category)createNewData();
			String name = PathUtilities.extractFileName(inPath);
			found.setName(name);
			//create parents and itself
			String parent = PathUtilities.extractDirectoryPath(inPath);
			Category parentcategory = createCategoryPath(parent);
			if( parentcategory != null)
			{
				parentcategory.addChild(found);
				saveData(found);
			}
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
	
	
	
}
