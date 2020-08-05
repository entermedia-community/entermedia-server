package org.entermediadb.asset.links;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.xmldb.CategorySearcher;
import org.entermediadb.elasticsearch.categories.ElasticCategorySearcher;
import org.entermediadb.webui.tree.BaseTreeModel;
import org.openedit.CatalogEnabled;
import org.openedit.page.manage.PageManager;
import org.openedit.profile.UserProfile;
import org.openedit.util.RequestUtils;

public class CategoryWebTreeModel extends BaseTreeModel implements CatalogEnabled
{
	private static final Log log = LogFactory.getLog(CategoryWebTreeModel.class);

	protected UserProfile fieldUserProfile;
	protected Set fieldHiddenCatalogs;
	protected Set fieldLimitToCatalogs;
	protected CategorySearcher fieldCategorySearcher;
	protected PageManager fieldPageManager;
	protected String fieldCatalogId;
	protected RequestUtils fieldRequestUtils;
	protected String fieldRootId;
	protected MediaArchive fieldMediaArchive;
	
	public MediaArchive getMediaArchive()
	{
		return fieldMediaArchive;
	}

	public void setMediaArchive(MediaArchive inMediaArchive)
	{
		fieldMediaArchive = inMediaArchive;
	}

	public UserProfile getUserProfile()
	{
		return fieldUserProfile;
	}

	public void setUserProfile(UserProfile inUserProfile)
	{
		fieldUserProfile = inUserProfile;
	}


	public CategorySearcher getCategorySearcher()
	{
		return fieldCategorySearcher;
	}

	public void setCategorySearcher(CategorySearcher inCategorySearcher)
	{
		fieldCategorySearcher = inCategorySearcher;
	}

	public RequestUtils getRequestUtils()
	{
		return fieldRequestUtils;
	}

	public void setRequestUtils(RequestUtils inRequestUtils)
	{
		fieldRequestUtils = inRequestUtils;
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

	public CategoryWebTreeModel()
	{
		setRoot(null);
	}

	/**
	 * @deprecated Use the list children method since it is faster
	 */
	public Object getChild(Object inParent, int index)
	{
		return listChildren(inParent).get(index);
	}

	public List listChildren(Object inParent)
	{
		if (inParent == null)
		{
			return Collections.EMPTY_LIST;
		}
		Category parent = (Category) inParent;
		List ok = new ArrayList(parent.getChildren().size());
		for (Iterator iter = parent.getChildren().iterator(); iter.hasNext();)
		{
			// If this is slow then we might consider only checking the top
			// cache the results in a cache map
			Category cat = (Category) iter.next();
			if (okToAdd(cat))
			{
				ok.add(cat);
			}
		}
		return ok;
	}

	protected boolean okToAdd(Category inCat)
	{
		if (inCat.getParentCategory() == null)
		{
			return true;
		}
		if (getHiddenCatalogs().contains(inCat.getId()))
		{
			return false;
		}

/*
		if (getLimitToCatalogs().size() > 0)
		{
			// Only worry about including these catalogs
			for (Iterator iterator = getLimitToCatalogs().iterator(); iterator.hasNext();)
			{
				Category okid = (Category) iterator.next();
				if (inCat.getId().equals(okid.getId()) || okid.hasParent(inCat.getId()))
				{
					return true;
				}
			}
			// This could be slow
			for (Iterator iterator = getLimitToCatalogs().iterator(); iterator.hasNext();)
			{
				Category okid = (Category) iterator.next();
				if (inCat.hasParent(okid.getId()))
				{
					return true;
				}
			}

			// None found so cancel if at same level as included one
			for (Iterator iterator = getLimitToCatalogs().iterator(); iterator.hasNext();)
			{
				Category okid = (Category) iterator.next();
				if (inCat.getLevel() == okid.getLevel())
				{
					return false;
				}
			}
			// index/photo2/stuff1 nostuff
			return true;
		}
		*/
		if( getUserProfile() != null && getUserProfile().getSettingsGroup().getId().equals("administrator"))
		{
			return true;
		}
		 
		Collection<Category> viewableparents = getUserProfile().getViewCategories();

		/*
		 * Collection<Category> privatecats = getMediaArchive().listHiddenCategories();
		 
		for (Iterator iterator = privatecats.iterator(); iterator.hasNext();)
		{
			Category hiddencategory = (Category)iterator.next();
			for (Iterator iterator2 = viewableparents.iterator(); iterator2.hasNext();)
			{
				Category specificchild = (Category) iterator2.next();
				if( inCat.hasParent(specificchild.getId()))
				{
					return true;
				}
			}
			if( inCat.hasParent(hiddencategory.getId()) )  //This cat is within this hidden so check it well
			{
				return false;
			}
		}
		*/
		
//		//In case it's new system TODO: Is this needed?
//		if(inCat.findValue("viewusers") == null && inCat.findValue("viewgroups") == null && inCat.findValue("viewroles") == null )
//		{
//			return true;
//		}
		Set allowed = new HashSet(getMediaArchive().listPublicCategories() );  //deprate this
		allowed.addAll(viewableparents);
		
		for (Iterator iterator = allowed.iterator(); iterator.hasNext();)
		{
			Category viewable = (Category) iterator.next();
			if( viewable.hasParentCategory(inCat) || inCat.hasParentCategory(viewable))
			{
				return true;
			}
		}
		
		return false;
	}

	public Set getHiddenCatalogs()
	{
		if (fieldHiddenCatalogs == null)
		{
			limitList();
		}
		return fieldHiddenCatalogs;
	}

	public Set getLimitToCatalogs()
	{
		if (fieldLimitToCatalogs == null)
		{
			limitList();
		}
		return fieldLimitToCatalogs;
	}

	protected void limitList()
	{
		// look over this users permissions and see if there is a limit
		fieldHiddenCatalogs = new HashSet();
		fieldLimitToCatalogs = new HashSet();
//		for (Iterator iterator = getSearchFilter().listAllFilters().iterator(); iterator.hasNext();)
//		{
//			String perm = (String) iterator.next();
//			if (perm.startsWith("limittocategory:"))
//			{
//				String catid = perm.substring("limittocategory:".length());
//				Category cat = getCategorySearcher().getCategory(catid);
//				if (cat != null)
//				{
//					fieldLimitToCatalogs.add(cat);
//				}
//			}
//			// This is old way to do it
//			else if (perm.startsWith("hidecategory:")) // Aways exclude it
//			{
//				String catid = perm.substring("hidecategory:".length());
//				fieldHiddenCatalogs.add(catid);
//			}
//			else if (perm.startsWith("hidecatalog:")) // Aways exclude it
//			{
//				String catid = perm.substring("hidecatalog:".length());
//				fieldHiddenCatalogs.add(catid);
//			}
//			else if (perm.startsWith("backgroundcatalog:")) // Aways exclude it
//			{
//				String catid = perm.substring("backgroundcatalog:".length());
//				fieldHiddenCatalogs.add(catid);
//			}
//		}
	}

	public List getChildren(Object inParent)
	{
		return listChildren(inParent);
	}

	public List getChildrenInRows(Object inParent, int inColCount)
	{
		// Now break up the page into rows by dividing the count they wanted
		List children = getChildren(inParent);
		double rowscount = (double) children.size() / (double) inColCount;

		List rows = new ArrayList();
		for (int i = 0; i < rowscount; i++)
		{
			int start = i * inColCount;
			int end = i * inColCount + inColCount;
			List sublist = children.subList(start, Math.min(children.size(), end));
			rows.add(sublist);
		}
		return rows;
	}

	public int getChildCount(Object inParent)
	{
		return listChildren(inParent).size();
	}

	public int getIndexOfChild(Object inParent, Object inChild)
	{
		return listChildren(inParent).indexOf(inChild);
	}

	public boolean isLeaf(Object inNode)
	{
		return !((Category) inNode).hasChildren();
	}

	public void setRoot(Category inCategory)
	{
		if( inCategory != null)
		{
			fieldRootId = inCategory.getId();
		}
	}

	public Object getRoot()
	{
		if (fieldRootId == null)
		{
			return getCategorySearcher().getRootCategory();
		}	
//		else
//		{
//			return getCategorySearcher().getCategory(fieldRoot.getId());
//		}
		
		//The children will be refreshed if needed based on the isDirty state
		Category cat = getCategorySearcher().getCategory(fieldRootId);
		//log.info("returning" + cat.hashCode() + " " + cat.getName());

		return cat;
	}

	public String getId(Object inNode)
	{
		if (inNode == null)
		{
			return null;
		}
		return ((Category) inNode).getId();
	}

	public Object getParent(Object inNode)
	{
		Category child = (Category) inNode;
		return child.getParentCategory();
	}


	public Category getRootCatalog()
	{
		return getCategorySearcher().getRootCategory();
	}

	public Object getChildById(String inId)
	{
		return findNodeById(getRoot(), inId);
	}

	public Object findNodeById(Object inRoot, String inId)
	{
		//String test = getId(inRoot);
//		if (test.equals(inId))
//		{
//			return inRoot;
//		}
//		//check one level deep
//		for (Iterator iterator = getChildren(inRoot).iterator(); iterator.hasNext();)
//		{
//			Object child = iterator.next();
//			String id = getId(child);
//			if (id.equals(inId))
//			{
//				return child;
//			}
//		}
		return getCategorySearcher().getCategory(inId);
	}

	public String getCatalogId()
	{
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}

}
