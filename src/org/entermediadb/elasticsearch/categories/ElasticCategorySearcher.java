package org.entermediadb.elasticsearch.categories;

import java.util.ArrayList;
import java.util.Collection;
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
import org.openedit.data.PropertyDetails;
import org.openedit.users.User;
import org.openedit.util.PathUtilities;

public class ElasticCategorySearcher extends BaseElasticSearcher implements CategorySearcher//, Reloadable
{
	private static final Log log = LogFactory.getLog(ElasticCategorySearcher.class);
	protected XmlCategoryArchive fieldXmlCategoryArchive;
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

	protected Category fieldRootCategory;
	
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
		Collection hits = query().exact("parentid", inParent.getId()).sort("name").search();
		List children = new ArrayList(hits.size());
		for (Iterator iterator = hits.iterator(); iterator.hasNext();) {
			Data data = (Data) iterator.next();
			ElasticCategory category = (ElasticCategory)createNewData();
			category.setId(data.getId());
			category.setProperties(data.getProperties());
			category.setParentCategory(inParent);
			children.add(category);
		}
		return children;
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
			
			//deleteAll(null); //This only deleted the index
			//This is the one time we load up the categories from the XML file
			getXmlCategoryArchive().clearCategories();
			fieldRootCategory = null;
			Category parent = getRootCategory();
			List tosave = new ArrayList();
			updateChildren(parent,tosave);
			updateIndex(tosave,null);
			
			//resave all the paths?
//			List tosave = new ArrayList(1000);
//			
//			for (Iterator iterator = all.iterator(); iterator.hasNext();)
//			{
//				Data row = (Data) iterator.next();
//				Category parent = (Category)loadData(row);
//				tosave.add(parent);
//				if(tosave.size() == 1000)
//				{
//					updateIndex(tosave, null);
//					tosave.clear();
//				}
//			}
//			updateIndex(tosave, null);
			
			fieldRootCategory = null;
			getRootCategory();
			
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
		if( inTosave.size() == 100)
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
	@Override
	protected void updateIndex(XContentBuilder inContent, Data inData, PropertyDetails inDetails)
	{
		super.updateIndex(inContent,inData,inDetails);
		Category category = (Category)inData;
		Category parent  = category.getParentCategory();
		if( parent != null)
		{
			try
			{
				inContent.field("parentid", parent.getId());
				String categorypath = category.getCategoryPath();
				inContent.field("categorypath", categorypath );
			}
			catch (Exception ex)
			{
				throw new OpenEditException(ex);
			}
		}
	}
	@Override
	public Category getRootCategory()
	{
		if( fieldRootCategory == null)
		{
			fieldRootCategory = (Category)searchById("index");
			if( fieldRootCategory == null)
			{
				fieldRootCategory = getXmlCategoryArchive().getRootCategory();
				fieldXmlCategoryArchive = null;
				if( fieldRootCategory == null)
				{
						fieldRootCategory = (Category)createNewData();
						fieldRootCategory.setId("index");
						fieldRootCategory.setName("Index");
				}
				fieldRootCategory.setValue("dirty", true);
				saveCategoryTree(fieldRootCategory);
				//We are going to create a database tool to import categories.xml
			}
		}	
		return fieldRootCategory;
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
	public Category getCategory(String inCatalog)
	{
		return (Category)searchById(inCatalog);
	}
	
	public Object searchByField(String inField, String inValue)
	{
		if( inField.equals("id") || inField.equals("_id"))
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
		return super.searchByField(inField, inValue);
	}

	@Override
	public Data loadData(Data inHit)
	{
		if( inHit instanceof Category)
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
	}
	
	public void saveData(Data inData, User inUser)
	{
		super.saveData(inData, inUser);
		getRootCategory().setValue("dirty", true);
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
		getRootCategory().setValue("dirty", true);

		getRootCategory().refresh();
	}

	@Override
	public Category createCategoryPath(String inPath)
	{
		if( inPath.length() == 0 || inPath.equals("Index") )
		{		
			Category found = (Category)query().exact("categorypath.sort", inPath).searchOne();
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
		return null;
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
