/*
 * Created on Mar 2, 2004
 */
package org.entermediadb.asset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.xmldb.CategorySearcher;
import org.openedit.data.BaseData;

/**
 * @author cburkey
 * 
 */
public class BaseCategory extends BaseData implements Category
{
	private static final Log log = LogFactory.getLog(Category.class);

	
	protected String fieldDescription;
	protected String fieldShortDecription;
	protected int fieldItemCount;
	protected List fieldChildren;
	protected Category fieldParentCategory;
	protected List fieldRelatedCategoryIds;
	protected String fieldLinkedToCategoryId;
	protected String fieldIndexId;
	protected CategorySearcher fieldCategorySearcher;

	public BaseCategory()
	{
	}
	

	public BaseCategory(String inName)
	{
		setName(inName);
	}

	public BaseCategory(String inId, String inName)
	{
		setId(inId);
		if (inName != null)
		{
			setName(inName.trim());
		}
	}
	
	public BaseCategory(CategorySearcher inCategorySearcher)
	{
		setCategorySearcher(inCategorySearcher);
	}
	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#getIndexId()
	 */
	@Override
	public String getIndexId()
	{
		return fieldIndexId;
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#setIndexId(java.lang.String)
	 */
	@Override
	public void setIndexId(String inIndexId)
	{
		fieldIndexId = inIndexId;
	}
	
	public CategorySearcher getCategorySearcher()
	{
		return fieldCategorySearcher;
	}

	public void setCategorySearcher(CategorySearcher inCategorySearcher)
	{
		fieldCategorySearcher = inCategorySearcher;
	}


	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#sortChildren(boolean)
	 */
	@Override
	public void sortChildren(boolean inRecursive){
		
		Collections.sort(getChildren());
		if(inRecursive)
		{
			for (Iterator iterator = getChildren().iterator(); iterator.hasNext();) 
			{
				Category child = (Category) iterator.next();
				child.sortChildren(inRecursive);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#getRelatedCategoryIds()
	 */
	@Override
	public List getRelatedCategoryIds()
	{
		if (fieldRelatedCategoryIds == null)
		{
			fieldRelatedCategoryIds = new ArrayList();
		}
		return fieldRelatedCategoryIds;
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#setRelatedCategoryIds(java.util.List)
	 */
	@Override
	public void setRelatedCategoryIds(List fieldRelatedCategoryIds)
	{
		this.fieldRelatedCategoryIds = fieldRelatedCategoryIds;
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#toString()
	 */
	@Override
	public String toString()
	{
		return getName();
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#getItemCount()
	 */
	@Override
	public int getItemCount()
	{
		return fieldItemCount;
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#setItemCount(int)
	 */
	@Override
	public void setItemCount(int inItemCount)
	{
		fieldItemCount = inItemCount;
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#isContainsItems()
	 */
	@Override
	public boolean isContainsItems()
	{
		return getItemCount() > 0;
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#getChildren()
	 */
	@Override
	public List getChildren()
	{
		if (fieldChildren == null)
		{
			fieldChildren = new ArrayList();
		}
		return fieldChildren;
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#setChildren(java.util.List)
	 */
	@Override
	public void setChildren(List inChildren)
	{
		fieldChildren = inChildren;
		for (Iterator iter = inChildren.iterator(); iter.hasNext();)
		{
			Category cat = (Category) iter.next();
			cat.setParentCategory(this);
		}
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#addChild(org.entermediadb.asset.Category)
	 */
	@Override
	public Category addChild(Category inNewChild)
	{
		inNewChild.setParentCategory(this);
		// I removed this to speed things up
		// for (int i = 0; i < getChildren().size(); i++)
		// {
		// Category element = (Category) getChildren().get(i);
		// if ( element.getId().equals(inNewChild.getId()))
		// {
		// getChildren().set(i, inNewChild);
		// return inNewChild;
		// }
		// }
		getChildren().add(inNewChild);
		return inNewChild;
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#getChild(java.lang.String)
	 */
	@Override
	public Category getChild(String inId)
	{
		for (Iterator iter = getChildren().iterator(); iter.hasNext();)
		{
			Category element = (Category) iter.next();
			if (element.getId().equals(inId))
			{
				return element;
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#removeChild(org.entermediadb.asset.Category)
	 */
	@Override
	public void removeChild(Category inChild)
	{
		Category child = getChild(inChild.getId());
		if (child != null )
		{
			getChildren().remove(child);
			child.setParentCategory(null);
			if( child != inChild)
			{
				//tell the old parent to remove itself?
				
			}
			
		}

		inChild.setParentCategory(null);
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#hasParent(java.lang.String)
	 */
	@Override
	public boolean hasParent(String inId)
	{
		Category parent = this;
		while (parent != null)
		{
			if (parent.getId().equals(inId))
			{
				return true;
			}
			parent = parent.getParentCategory();
		}
		return false;
	}

	public boolean hasParentCategory(Category inId)
	{
		Category parent = this;
		while (parent != null)
		{
			if (parent.equals(inId))
			{
				return true;
			}
			parent = parent.getParentCategory();
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#hasChildren()
	 */
	@Override
	public boolean hasChildren()
	{
		boolean has =  fieldChildren != null && fieldChildren.size() > 0;
		return has;
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#hasCatalog(java.lang.String)
	 */
	@Override
	public boolean hasCatalog(String inId)
	{
		if (getId().equals(inId))
		{
			return true;
		}
		if (hasChildren())
		{
			for (Iterator iter = getChildren().iterator(); iter.hasNext();)
			{
				Category child = (Category) iter.next();
				if (child.hasCatalog(inId))
				{
					return true;
				}
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#hasChild(java.lang.String)
	 */
	@Override
	public boolean hasChild(String inId)
	{
		if (hasChildren())
		{
			for (Iterator iter = getChildren().iterator(); iter.hasNext();)
			{
				Category child = (Category) iter.next();
				if (child.getId().equals(inId))
				{
					return true;
				}
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#isAncestorOf(org.entermediadb.asset.Category)
	 */
	@Override
	public boolean isAncestorOf(Category inCatalog)
	{
		for (Iterator children = getChildren().iterator(); children.hasNext();)
		{
			Category child = (Category) children.next();
			if (child == inCatalog)
			{
				return true;
			}
			else if (child.hasChildren() && child.isAncestorOf(inCatalog))
			{
				return true;
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#getParentCategory()
	 */
	@Override
	public Category getParentCategory()
	{
		return fieldParentCategory;
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#setParentCategory(org.entermediadb.asset.Category)
	 */
	@Override
	public void setParentCategory(Category parentCatalog)
	{
		if( parentCatalog.hasParent(getId()))
		{
			log.error("Called myself as a child");
			return;
		}
		fieldParentCategory = parentCatalog;
		if (parentCatalog != null)
		{
			setParentId(parentCatalog.getId());
		}
		else
		{
			setParentId(null);
		}
		
		//These will be set in indexing
		if( getId() != null)
		{
			setValue("categorypath", loadCategoryPath());
			setValue("parents", getParentCategories());
		}
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#listAncestorsAndSelf(int)
	 */
	@Override
	public List listAncestorsAndSelf(int inStartLevel)
	{
		LinkedList result = new LinkedList();
		Category catalog = this;
		while (catalog != null)
		{
			result.addFirst(catalog);
			catalog = catalog.getParentCategory();
		}
		return result.subList(inStartLevel, result.size());
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#getChildrenInRows(int)
	 */
	@Override
	public List getChildrenInRows(int inColCount)
	{
		// Now break up the page into rows by dividing the count they wanted
		List children = getChildren();
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

	public List getChildren(int inColCount)
	{
		// Now break up the page into rows by dividing the count they wanted
		List children = getChildren();
		List rows = new ArrayList();
		int inMax = Math.min(inColCount, children.size());
		for (int i = 0; i < inMax; i++)
		{
			rows.add(children.get(i));
		}
		return rows;
	}
	
	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#getLevel()
	 */
	@Override
	public int getLevel()
	{
		int i = 1;
		Category parent = this;
		while (parent != null)
		{
			parent = parent.getParentCategory();
			i++;
		}
		return i;
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#getDescription()
	 */
	@Override
	public String getDescription()
	{
		return fieldDescription;
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#setDescription(java.lang.String)
	 */
	@Override
	public void setDescription(String inDescription)
	{
		fieldDescription = inDescription;
	}

//	public Object getValue(String inKey)
//	{
//		Object val = super.getValue(inKey);
//		if (inKey.equals("categorypath"))
//		{
//			return getCategoryPath();
//		}
//		if (inKey.equals("parents"))
//		{
//			List paths = new ArrayList();
//			Category parent = getParentCategory();
//			while (parent != null)
//			{
//				paths.add(0, parent);
//				parent = parent.getParentCategory();
//			}
//			return paths;
//		}
//		if (val != null)
//		{
//			return val;
//		}
//		return null;
//	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#getShortDescription()
	 */
	@Override
	public String getShortDescription()
	{
		return fieldShortDecription;
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#setShortDescription(java.lang.String)
	 */
	@Override
	public void setShortDescription(String inShortDecription)
	{
		fieldShortDecription = inShortDecription;
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#clearChildren()
	 */
	@Override
	public void clearChildren()
	{
		getChildren().clear();
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#getChildByName(java.lang.String)
	 */
	@Override
	public Category getChildByName(String inCatName)
	{
		for (Iterator iter = getChildren().iterator(); iter.hasNext();)
		{
			Category cat = (Category) iter.next();
			if (cat.getName().equals(inCatName))
			{
				return cat;
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#getLink()
	 */
	@Override
	public String getLink()
	{
		String path = get("path");
		if (path != null)
		{
			return path;
		}
		String root = get("categoryhome");
		if (root == null)
		{
			root = "/store/categories/";
		}
		return root + getId() + ".html";
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#getParentCategories()
	 */
	@Override
	public List getParentCategories()
	{
		List paths = new ArrayList();
		paths.add(this);
		Category parent = getParentCategory();
		while (parent != null)
		{
			paths.add(0, parent);
			parent = parent.getParentCategory();
		}
		return paths;
	}
	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#getParentCategoriesFrom(int)
	 */
	@Override
	public List getParentCategoriesFrom(int inStartFrom)
	{
		List paths = new ArrayList();
		Category parent = getParentCategory();
		paths.add(this);
		while (parent != null)
		{
			paths.add(0, parent);
			parent = parent.getParentCategory();
		}
		if( paths.size() == 1)
		{
			return paths;
		}
		//from top to end 1, 2, 3, 4 cuts 3 till 4
		if( inStartFrom >= paths.size() )
		{
			return Collections.EMPTY_LIST;
		}
		paths = paths.subList(inStartFrom, paths.size());
		
		return paths;
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#clearRelatedCategoryIds()
	 */
	@Override
	public void clearRelatedCategoryIds()
	{
		fieldRelatedCategoryIds = new ArrayList();

	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#addRelatedCategoryId(java.lang.String)
	 */
	@Override
	public void addRelatedCategoryId(String inId)
	{
		getRelatedCategoryIds().add(inId);

	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#getLinkedToCategoryId()
	 */
	@Override
	public String getLinkedToCategoryId()
	{
		return fieldLinkedToCategoryId;
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#setLinkedToCategoryId(java.lang.String)
	 */
	@Override
	public void setLinkedToCategoryId(String inLinkedToCategoryId)
	{
		fieldLinkedToCategoryId = inLinkedToCategoryId;
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#getParentId()
	 */
	@Override
	public String getParentId()
	{
		return get("parentid");
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#setParentId(java.lang.String)
	 */
	@Override
	public void setParentId(String inParentId)
	{
		setValue("parentid", inParentId);
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#getCategoryPath()
	 */
	@Override
	public String getCategoryPath()
	{
		String path = get("categorypath");
		if( path == null)
		{
			path = loadCategoryPath();
		}
		return path;
	}
	
	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#loadCategoryPath()
	 */
	@Override
	public String loadCategoryPath()
	{
		String vale  = null;//get("sourcepath");
		StringBuffer path = new StringBuffer();
		boolean first = true;
		for (Iterator iterator = getParentCategories().iterator(); iterator.hasNext();)
		{
			if( first ) //This takes off the index category
			{
				iterator.next();
				first = false;
				continue;
			}
			Category aparent = (Category) iterator.next();
			path.append(aparent.getName());
			if( iterator.hasNext() )
			{
				path.append("/");
			}
		}
		vale = path.toString();
		if( vale.isEmpty())
		{
			return getName();
		}
		return vale;
	}
	
	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#compareTo(org.entermediadb.asset.Category)
	 */
	@Override
	public int compareTo(Object c)
	{
		Category c2 = (Category)c;
		if( getName() == null )
		{
			if( c2.getName() == null)
			{
				return 0;
			}
			return -1;
		}
		else if( c2.getName() == null)
		{
			return 1;
		}
		return getName().toLowerCase().compareTo(c2.getName().toLowerCase());
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#refresh()
	 */
	@Override
	public boolean refresh()
	{
		if( isDirty() && getCategorySearcher() != null)
		{
			fieldChildren = null;
			setIndexId(getCategorySearcher().getIndexId());
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#isDirty()
	 */
	@Override
	public boolean isDirty()
	{
		if( getCategorySearcher() != null && getCategorySearcher().getIndexId().equals(getIndexId()))
		{
			return false;
		}
		return true;
	}

	 /* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#equals(java.lang.Object)
	 */
	@Override
	 public boolean equals(Object obj)
	 {
		 if( obj == this)
		 {
			 return true;
		 }
		 if(obj instanceof Category)
		 {
			 Category c = (Category)obj;
			 String id = c.getId();
			 if( id != null && id.equals(getId()))
			 {
				 return true;
			 }
		 }
		 
		 return false;
	 }

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#hasParent(java.util.Collection)
	 */
	@Override
	public boolean hasParent(Collection<String> inCategorids)
	{
		for(String id : inCategorids)
		{
			if( hasParent(id) )
			{
				return true;
			}
		}
		return false;

	}
	public boolean hasParentCategory(Collection<Category> inCategorids)
	{
		for(Category id : inCategorids)
		{
			if( hasParentCategory(id) )
			{
				return true;
			}
		}
		return false;

	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#hasSelf(java.util.Collection)
	 */
	@Override
	public boolean hasSelf(Collection<String> inCategorids)
	{
		for(String id : inCategorids)
		{
			if( getId().equals( id) )
			{
				return true;
			}
		}
		return false;

	}
	
	public boolean hasSelfCategory(Collection<Category> inCategorids)
	{
		for(Category id : inCategorids)
		{
			if( equals( id) )
			{
				return true;
			}
		}
		return false;

	}
	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#findValue(java.lang.String)
	 */
	@Override
	public Object findValue(String inString)
	{
		Object value = getValue(inString);
		if(value != null){
			return value;
		}
		
		if(getParentCategory() != null){
			value = getParentCategory().findValue(inString);
		}
		return value;
	}


	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Category2#hasLoadedParent()
	 */
	@Override
	public boolean hasLoadedParent()
	{
		return fieldParentCategory != null;
	}
	
	public boolean hasCountData()
	{
		if( getValue("countdata") == null )
		{
			return false;
		}
		if( getValues("countdata").isEmpty() )
		{
			return false;
		}
		return true;
	}
	
	public int getCount()
	{
		Collection counted = getValues("countdata");
		return counted.size();
	}
}
