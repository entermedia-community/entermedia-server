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

public class TaskTreeModel extends BaseTreeModel implements CatalogEnabled
{
	private static final Log log = LogFactory.getLog(TaskTreeModel.class);

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

	public TaskTreeModel()
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
		return true;
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
		Category cat = getCategorySearcher().getCategory(fieldRootId);

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
		String test = getId(inRoot);
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
