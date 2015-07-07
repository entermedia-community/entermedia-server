package org.openedit.entermedia.links;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.openedit.entermedia.Category;
import org.openedit.entermedia.CategoryArchive;
import org.openedit.entermedia.search.SearchFilter;

import com.openedit.page.manage.PageManager;
import com.openedit.users.User;
import com.openedit.util.RequestUtils;
import com.openedit.webui.tree.BaseTreeModel;

public class CatalogWebTreeModel extends BaseTreeModel
{
	protected User fieldUser;
	protected Set fieldHiddenCatalogs;
	protected Set fieldLimitToCatalogs;
	protected CategoryArchive fieldCatalogArchive;
	protected SearchFilter fieldSearchFilter;
	protected PageManager fieldPageManager;
	protected String fieldCatalogId;
	protected RequestUtils fieldRequestUtils;
	protected Category fieldRoot;

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

	public CatalogWebTreeModel()
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
		if( getSearchFilter().hasExcludedCategory(inCat.getId()) )
		{
			return false;
		}

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

		return true;
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
		for (Iterator iterator = getSearchFilter().listAllFilters().iterator(); iterator.hasNext();)
		{
			String perm = (String) iterator.next();
			if (perm.startsWith("limittocategory:"))
			{
				String catid = perm.substring("limittocategory:".length());
				Category cat = getCatalogArchive().getCategory(catid);
				if (cat != null)
				{
					fieldLimitToCatalogs.add(cat);
				}
			}
			// This is old way to do it
			else if (perm.startsWith("hidecategory:")) // Aways exclude it
			{
				String catid = perm.substring("hidecategory:".length());
				fieldHiddenCatalogs.add(catid);
			}
			else if (perm.startsWith("hidecatalog:")) // Aways exclude it
			{
				String catid = perm.substring("hidecatalog:".length());
				fieldHiddenCatalogs.add(catid);
			}
			else if (perm.startsWith("backgroundcatalog:")) // Aways exclude it
			{
				String catid = perm.substring("backgroundcatalog:".length());
				fieldHiddenCatalogs.add(catid);
			}
		}
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
		fieldRoot = inCategory;
	}

	public Object getRoot()
	{
		if (fieldRoot == null)
			return getCatalogArchive().getRootCategory();
		else
			return getCatalogArchive().getCategory(fieldRoot.getId());
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

	public User getUser()
	{
		return fieldUser;
	}

	public void setUser(User inUser)
	{
		fieldUser = inUser;
	}

	public Category getRootCatalog()
	{
		return getCatalogArchive().getRootCategory();
	}

	public Object getChildById(String inId)
	{
		return findNodeById(getRoot(), inId);
	}

	public Object findNodeById(Object inRoot, String inId)
	{
		String test = getId(inRoot);
		if (test.equals(inId))
		{
			return inRoot;
		}
		for (Iterator iterator = getChildren(inRoot).iterator(); iterator.hasNext();)
		{
			Object child = iterator.next();
			child = findNodeById(child, inId);
			if (child != null)
			{
				return child;
			}
		}
		return null;
	}

	public CategoryArchive getCatalogArchive()
	{
		return fieldCatalogArchive;
	}

	public void setCatalogArchive(CategoryArchive inCatalogArchive)
	{
		fieldCatalogArchive = inCatalogArchive;
	}

	public SearchFilter getSearchFilter()
	{
		return fieldSearchFilter;
	}

	public void setSearchFilter(SearchFilter inSearchFilter)
	{
		fieldSearchFilter = inSearchFilter;
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
