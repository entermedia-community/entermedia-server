/*
 * Created on Oct 3, 2004
 */
package org.entermediadb.asset.xmldb;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.entermediadb.asset.BaseCategory;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.CategoryArchive;
import org.entermediadb.data.BaseDataArchive;
import org.openedit.OpenEditException;
import org.openedit.OpenEditRuntimeException;
import org.openedit.cache.CacheManager;
import org.openedit.config.Configuration;
import org.openedit.config.XMLConfiguration;
import org.openedit.page.Page;
import org.openedit.page.manage.PageManager;
import org.openedit.repository.filesystem.StringItem;
import org.openedit.util.PathUtilities;
import org.openedit.util.XmlUtil;

/**
 * @author cburkey
 * 
 */
public class XmlCategoryArchive extends BaseDataArchive implements CategoryArchive
{
	private static final Log log = LogFactory.getLog(XmlCategoryArchive.class);
	protected Category fieldRootCatalog;
	protected PageManager fieldPageManager;
	protected XmlUtil fieldXmlUtil;
	protected CacheManager fieldCacheManager;
	protected Category NULL = new BaseCategory();
	
	public XmlCategoryArchive()
	{
		log.info("created");
	}
	
	public CacheManager getCacheManager()
	{
		return fieldCacheManager;
	}

	public void setCacheManager(CacheManager inCacheManager)
	{
		fieldCacheManager = inCacheManager;
	}

	public List listAllCategories()
	{
		List all = new ArrayList();
		addCategories(all, getRootCategory());
		return all;
	}

	/**
	 * @param inAll
	 * @param inRootCatalog
	 */
	private void addCategories(List inAll, Category inRootCatalog)
	{
		// TODO Auto-generated method stub
		inAll.add(inRootCatalog);
		for (Iterator iter = inRootCatalog.getChildren().iterator(); iter.hasNext();)
		{
			Category child = (Category) iter.next();
			addCategories(inAll, child);
		}
	}


	public Category getCategory(String inCategory)
	{
		try
		{
			if(inCategory == null){
				return null;
			}
			Category cat = (Category) getCacheManager().get(cacheId(),inCategory);
			if( cat == null)
			{
				//search twice. Once follow the id path
				cat = findChildByPath(getRootCategory(), inCategory );
				//second time search everyone
				if( cat == null)
				{
					cat = findChild(getRootCategory(),inCategory);
				}
				if( cat == null)
				{
					cat = NULL;
				}
				getCacheManager().put(cacheId(), inCategory, cat);
			}
			if( cat == NULL)
			{
				return null;
			}
			return cat;
		}
		catch (Exception ex)
		{
			throw new OpenEditRuntimeException(ex);
		}
	}
	protected Category findChildByPath(Category inRoot, String inId)
	{
		String test = inRoot.getId();
		if (test.equals(inId))
		{
			return inRoot;
		}
		for (Iterator iterator = inRoot.getChildren().iterator(); iterator.hasNext();)
		{
			Category child = (Category)iterator.next();
			if( child.getId().startsWith(inId))
			{
				child = findChild(child, inId);
				if (child != null)
				{
					return child;
				}
			}
		}
		return null;
	}
	public Category findChild(Category inRoot, String inId)
	{
		String test = inRoot.getId();
		if (test.equals(inId))
		{
			return inRoot;
		}
		for (Iterator iterator = inRoot.getChildren().iterator(); iterator.hasNext();)
		{
			Category child = (Category)iterator.next();
			child = findChild(child, inId);
			if (child != null)
			{
				return child;
			}
		}
		return null;
	}


	protected String cacheId()
	{
		return getCatalogId() + "categoryarchive";
	}
	public Category getCategoryByName(String inCategoryName)
	{
		List catalogs = listAllCategories();
		for (Iterator iter = catalogs.iterator(); iter.hasNext();)
		{
			Category catalog = (Category) iter.next();
			if (catalog.getName().equals(inCategoryName))
			{
				return catalog;
			}
		}
		return null;
	}

	public Category getRootCategory()
	{
		if (fieldRootCatalog == null )
		{
			synchronized (this )
			{
				if (fieldRootCatalog == null )
				{
					reloadCategories();
				}
			}
		}
		return fieldRootCatalog;
	}


	public void deleteCategory(Category inCategory)
	{
		if (getRootCategory().getId().equals(inCategory.getId()))
		{
			//setRootCategory(new BaseCategory("index", "Index"));
			for (Iterator iterator = inCategory.getChildren().iterator(); iterator.hasNext();)
			{
				Category child = (Category) iterator.next();
				deleteAll(child);
			}
		}
		else
		{
			deleteAll(inCategory);
			if (inCategory.getParentCategory() != null)
			{
				inCategory.getParentCategory().removeChild(inCategory);
			}
		}
		getCacheManager().remove(cacheId(),inCategory.getId());
		saveAll();
	}

	protected void deleteAll(Category inCategory)
	{
		for (Iterator iter = inCategory.getChildren().iterator(); iter.hasNext();)
		{
			Category child = (Category) iter.next();
			child.setParentCategory(null); // to prevent
			// ConcurrentModificationException
			deleteAll(child);
		}
	}

	public void clearCategories()
	{
		getCacheManager().clear(cacheId());
		fieldRootCatalog = null;
	}

//	public void setRootCategory(Category inRootCatalog)
//	{
//		fieldRootCatalog = inRootCatalog;
//
//		if (fieldRootCatalog != null)
//		{
//			// This is not used much anymore
//			String home = fieldRootCatalog.getProperty("categoryhome");
//			if (home == null)
//			{
//				fieldRootCatalog.setProperty("categoryhome", "/" + getCatalogId() + "/categories/");
//			}
//			cacheCategory(fieldRootCatalog);
//		}
//
//	}

	protected String listCatalogXml()
	{
		return "/WEB-INF/data/" + getCatalogId() + "/categories.xml";
	}

	/*
	 * (non-javadoc)
	 * 
	 * @see org.openedit.store.CatalogArchive#saveCatalogs()
	 */
	public synchronized void saveAll()
	{
		try
		{
			Page catalogFile = getPageManager().getPage(listCatalogXml());
			Page tempfile = getPageManager().getPage("/WEB-INF/data/" + getCatalogId() + "/categories.tmp.xml");
			Element root = createElement(getRootCategory());
			// lets write to a stream
			//TODO: Lock the path
			
			OutputStream out = getPageManager().saveToStream(tempfile);
			getXmlUtil().saveXml(root, out, catalogFile.getCharacterEncoding());
			getPageManager().movePage(tempfile, catalogFile);
		}
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}

	}

	/**
	 * @param inRootCatalog
	 * @return
	 */
	protected Element createElement(Category inRootCatalog) throws OpenEditException
	{
		Element child = DocumentHelper.createElement("catalog");
		child.addAttribute("id", inRootCatalog.getId());
		child.addAttribute("name", inRootCatalog.getName());
		if (inRootCatalog.getShortDescription() != null)
		{
			child.addElement("shortdescription").setText(inRootCatalog.getShortDescription());
		}
		if (inRootCatalog.getDescription() != null)
		{
			saveLongDescription(inRootCatalog);
		}
		// add any other attributes we might have
		for (Iterator iter = inRootCatalog.getProperties().keySet().iterator(); iter.hasNext();)
		{
			String id = (String) iter.next();
			if (id != null && !"id".equals(id) && !"name".equals(id))
			{
				Element prop = child.addElement("property");
				
				String text = (String)inRootCatalog.getValue(id);
				
				prop.addAttribute("id", id);
				if(text != null){
				prop.setText(text);
				} else{
					//prop.setText("missing");
				}
			}
		}
		saveRelatedCategories(inRootCatalog, child);

		for (Iterator iter = inRootCatalog.getChildren().iterator(); iter.hasNext();)
		{
			Category subcatalog = (Category) iter.next();
			Element newchild = createElement(subcatalog);
			child.add(newchild);
		}

		return child;
	}

	protected void saveLongDescription(Category inCategory)
	{
		try
		{
			Page fulldesc = getPageManager().getPage("/" + getCatalogId() + "/categories/" + inCategory.getId() + ".html");
			if (!fulldesc.exists())
			{
				String desc = inCategory.getDescription();
				if (desc == null || desc.trim().length() == 0)
				{
					return;
				}
				StringItem item = new StringItem(fulldesc.getPath(), desc, "UTF-8");
				fulldesc.setContentItem(item);
				getPageManager().putPage(fulldesc);
			}
		}
		catch (OpenEditException oee)
		{
			throw new OpenEditException(oee);
		}
	}

	public synchronized void reloadCategories()
	{
		getCacheManager().clear(cacheId());

		try
		{
			Page catalogFile = getPageManager().getPage(listCatalogXml());
			if (catalogFile.exists())
			{
				try
				{
					log.info("Reload categories.xml");
					Element rootE = getXmlUtil().getXml(catalogFile.getReader(), catalogFile.getCharacterEncoding());
					XMLConfiguration rootConfig = new XMLConfiguration();
					rootConfig.populate(rootE);

					Category root = createCatalog(rootConfig);
					if( fieldRootCatalog == null)
					{
						fieldRootCatalog  = root;
					}
					else					
					{
						//Just update the root object
						fieldRootCatalog.setName(root.getName());
						fieldRootCatalog.setProperties(root.getProperties());
						fieldRootCatalog.setChildren(root.getChildren());
					}
					//fieldRootCatalog.setProperty("dirty", "false");
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
					throw new OpenEditException(ex);
				}
			}
			else
			{
				log.error("No catalog file found " + catalogFile.getPath());
				Category root = new BaseCategory();
				root.setId("index");
				root.setName("Index");
				fieldRootCatalog  = root;//setRootCategory(root);
			}
		}
		catch (Exception ex)
		{
			throw new OpenEditException(ex);
		}
	}

	/**
	 * @param inRootElement
	 * @return
	 */
	protected Category createCatalog(Configuration inRootConfig) throws OpenEditException
	{
		Category cat = new BaseCategory();
		cat.setId(inRootConfig.getAttribute("id"));
		if (cat.getId() == null)
		{
			log.error("Corrupt catalog with no id");
		}
		cat.setName(inRootConfig.getAttribute("name"));
		for (Iterator iter = inRootConfig.getAttributeNames().iterator(); iter.hasNext();)
		{
			String attrName = (String) iter.next();
			if (!attrName.equals("id") && !attrName.equals("name"))
			{
				cat.setProperty(attrName, inRootConfig.getAttribute(attrName));
			}
		}
		// Also supports property
		for (Iterator iter = inRootConfig.getChildren("property").iterator(); iter.hasNext();)
		{
			Configuration config = (Configuration) iter.next();
			cat.setProperty(config.getAttribute("id"), config.getValue());
		}

		String shortdesc = inRootConfig.getChildValue("shortdescription");
		cat.setShortDescription(shortdesc);

		loadRelatedCategoryIds(cat, inRootConfig);
		for (Iterator iter = inRootConfig.getChildren("catalog").iterator(); iter.hasNext();)
		{
			Configuration config = (Configuration) iter.next();
			cat.addChild(createCatalog(config));
		}
		return cat;
	}


	protected void loadRelatedCategoryIds(Category inCategory, Configuration inAssetConfig)
	{
		inCategory.clearRelatedCategoryIds();
		Configuration relatedAssetsElem = inAssetConfig.getChild("related-categories");
		if (relatedAssetsElem != null)
		{
			for (Iterator iter = relatedAssetsElem.getChildIterator("category"); iter.hasNext();)
			{
				Configuration relatedProdConfig = (Configuration) iter.next();
				inCategory.addRelatedCategoryId(relatedProdConfig.getAttribute("id"));
			}
		}
		Configuration linkedToElem = inAssetConfig.getChild("linkedtocategory");
		if (linkedToElem != null)
		{
			inCategory.setLinkedToCategoryId(linkedToElem.getAttribute("id"));
		}
		else
		{
			inCategory.setLinkedToCategoryId(null);
		}

	}

	protected void saveRelatedCategories(Category inCategory, Element inCategoryElement)
	{
		deleteElements(inCategoryElement, "related-categories");
		if (inCategory.getRelatedCategoryIds().size() > 0)
		{
			Element relatedAssetsElem = inCategoryElement.addElement("related-categories");
			for (Iterator iter = inCategory.getRelatedCategoryIds().iterator(); iter.hasNext();)
			{
				String relatedAssetId = (String) iter.next();
				relatedAssetsElem.addElement("category").addAttribute("id", relatedAssetId);
			}
		}
		deleteElements(inCategoryElement, "linkedtocategory");
		if (inCategory.getLinkedToCategoryId() != null)
		{
			Element linkedToElem = inCategoryElement.addElement("linkedtocategory");
			if (linkedToElem != null)
			{
				linkedToElem.addAttribute("id", inCategory.getLinkedToCategoryId());
			}
		}
	}


	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

	public void saveCategory(Category inCategory)
	{
		if (inCategory.getParentCategory() == null && !inCategory.getId().equals(getRootCategory().getId()))
		// don't want to make child of the root cat if it is the root cat
		{
			addChild(inCategory);
		}
		else
		{
			cacheCategory(inCategory);
		}
		saveAll();
	}

	public Category cacheCategory(Category inCat)
	{
		getCacheManager().put(cacheId(),inCat.getId(), inCat);
		for (Iterator iterator = inCat.getChildren().iterator(); iterator.hasNext();)
		{
			Category child = (Category) iterator.next();
			cacheCategory(child);
		}
		return inCat;
	}

	/**
	 * @deprecated use saveCategory
	 */
	public void saveCatalog(Category inCategory)
	{
		saveCategory(inCategory);
	}

	public Category addChild(Category inCatalog)
	{
		getRootCategory().addChild(inCatalog);
		cacheCategory(inCatalog);
		return inCatalog;
	}

	public XmlUtil getXmlUtil()
	{
		return fieldXmlUtil;
	}

	public void setXmlUtil(XmlUtil inXmlUtil)
	{
		fieldXmlUtil = inXmlUtil;
	}

	public Category createCategoryTree(String inPath) throws OpenEditException
	{
		Category created = createCategoryTree(inPath, null);
		saveCategory(created);

		return created;
	}

	public Category createCategoryTree(String inPath, List inNames) throws OpenEditException
	{

		if (inPath.length() < 1)
		{
			return getRootCategory();
		}

		if (inPath.endsWith("/"))
		{
			inPath = inPath.substring(0, inPath.length() - 1);
		}

		String catid = createCategoryId(inPath);
		Category child = getCategory(catid);
		if (child == null)
		{
			// make sure we have a parent to put it in
			child = new BaseCategory();
			child.setId(catid);

			if (inNames != null)
			{
				child.setName((String) inNames.remove(inNames.size() - 1));
			}
			else
			{
				child.setName(PathUtilities.extractFileName(inPath));
			}
			String parentPath = PathUtilities.extractDirectoryPath(inPath);
			if (parentPath == null || parentPath == "/")
			{
				return child;
			}

			Category inParentCategory = createCategoryTree(parentPath, inNames);
			inParentCategory.addChild(child);
			
			List children = inParentCategory.getChildren();

			Collections.sort(children, new Comparator() 
			{
				public int compare(Object o1, Object o2) {
					Category c1 = (Category)o1, c2 = (Category)o2;
					return c1.getName().compareTo(c2.getName());
				}
			});
			inParentCategory.setChildren(children);
			cacheCategory(child);
		}
		return child;
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

	/**
	 * 
	 * @param inCategoryIds space separated list of category lists 
	 * @return
	 */
	public Collection getCategoriesByIds(String inCategoryIds)
	{
		if(inCategoryIds == null)
		{
			return null;
		}
		String[] categoryids = inCategoryIds.split(" ");
		List categories = new ArrayList();
		
		for (int i = 0; i < categoryids.length; i++)
		{
			Category cat = getCategory(categoryids[i]);
			if (cat != null)
			{
				categories.add(cat);
			}
		}
		return categories;
	}

	public Category createNewCategory(String inLabel)
	{
		String id = createCategoryId(inLabel);
		Category cat = new BaseCategory(id, inLabel);
		
		return cat;
	}
}
