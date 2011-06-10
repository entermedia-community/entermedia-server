package org.openedit.entermedia.search;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dom4j.Element;
import org.openedit.data.lucene.LuceneSearchQuery;
import org.openedit.xml.XmlFile;

import com.openedit.hittracker.SearchQuery;
import com.openedit.users.Group;
import com.openedit.users.User;

public class SearchFilter
{

	protected boolean fieldCategorySelected;
	protected boolean fieldIncludeUserFilter;
	protected User fieldUser;
	protected XmlFile fieldSavedFilters; // people, animals etc.
	protected XmlFile fieldSavedCategoryFilters; // User specific. Temporary
	// hidden ones
	protected Set fieldExcludeCategories; // Group based was System Wide

	public SearchFilter()
	{
		// TODO Auto-generated constructor stub
	}

	public List listFilters()
	{
		List filters = new ArrayList();

		if (getSavedFilters() != null)
		{
			for (Iterator iterator = getSavedFilters().getElements().iterator(); iterator.hasNext();)
			{
				Element child = (Element) iterator.next();
				filters.add(child.getText());
			}
		}
		if (getSavedCategoryFilters() != null)
		{
			for (Iterator iterator = getSavedCategoryFilters().getElements().iterator(); iterator.hasNext();)
			{
				Element child = (Element) iterator.next();
				filters.add(child.getText());
			}
		}
		if (fieldExcludeCategories != null)
		{
			for (Iterator iterator = getExcludeCategories().iterator(); iterator.hasNext();)
			{
				String id = (String) iterator.next();
				filters.add("excluderecords:category:" + id);
			}
		}
		return filters;
	}

	public void clearSavedFilters()
	{
		if (getSavedFilters() != null)
		{
			getSavedFilters().clear();
		}
	}

	public void clearSavedFilters(String inId)
	{
		if (getSavedFilters() != null)
		{
			List toremove = new ArrayList();
			for (Iterator iter = getSavedFilters().getElements().iterator(); iter.hasNext();)
			{
				Element element = (Element) iter.next();
				String value = element.attributeValue("id");
				if (value != null)
				{
					String[] type = value.split(":");
					if (type[0].equals(inId))
					{
						toremove.add(element);
					}
				}
			}
			for (Iterator iter = toremove.iterator(); iter.hasNext();)
			{
				Element element = (Element) iter.next();
				getSavedFilters().deleteElement(element);
			}
		}
	}

	/**
	 * People, animals, etc...
	 * 
	 * @param field
	 * @param inValues
	 */
	public void addSavedExcludeFilter(String field, List inValues)
	{
		for (int i = 0; i < inValues.size(); i++)
		{
			String value = (String) inValues.get(i);
			Element child = getSavedFilters().addNewElement();
			child.addAttribute("id", field + ":" + value);
			child.setText("excluderecords:" + field + ":" + value);
		}
	}

	public void addSavedUserFilter(String field, List inValues)
	{
		for (int i = 0; i < inValues.size(); i++)
		{
			String value = (String) inValues.get(i);
			Element child = getSavedFilters().addNewElement();
			child.addAttribute("id", field + ":" + value);
			child.setText("userfilter:" + field + ":" + value);
		}
	}

	public void addSavedCategoryFilter(List inValues)
	{
		for (int i = 0; i < inValues.size(); i++)
		{
			String value = (String) inValues.get(i);
			Element child = getSavedCategoryFilters().addNewElement();
			child.addAttribute("id", "hidecategory:" + value);
			child.setText("hidecategory:" + value);
		}
	}

	public List listAllFilters()
	{
		List filters = listFilters();
		if (getUser() != null)
		{
			filters.addAll(getUser().listGroupPermissions());

			Map allsettings = getUser().listAllProperties();
			for (Iterator iterator = allsettings.keySet().iterator(); iterator.hasNext();)
			{
				String key = (String) iterator.next();
				if (key.startsWith("excluderecords:") || key.startsWith("limitrecords:"))
				{
					String value = (String) allsettings.get(key);
					if (value != null)
					{
						filters.add(key + ":" + value);
					}
				}
			}
		}
		return filters;
	}

	public String toFilter(boolean inUsesSecurity)
	{

		SearchQuery andgroup = new LuceneSearchQuery();
		// orgroup.setAndTogether(false);
		List checks = listAllFilters();

		for (Iterator iter = checks.iterator(); iter.hasNext();)
		{
			String name = (String) iter.next();
			if (name.startsWith("excluderecords:"))
			{
				// Format is exclude:Field Id:value
				String[] command = name.split(":");
				if (command.length > 2)
				{
					andgroup.addNot(command[1], command[2]);
				}
			}
			else if (name.startsWith("limitrecords:"))
			{
				// Format is exclude:Field Id:value
				String[] command = name.split(":");
				if (command.length > 2)
				{
					andgroup.addMatches(command[1], command[2]);
				}
			}
			if (isIncludeUserFilter() && name.startsWith("userfilter:"))
			{
				String[] command = name.split(":");
				andgroup.addNot(command[1], command[2]);
			}
		}
		// AND together with these OR ed things
		SearchQuery orgroup = new LuceneSearchQuery();
		orgroup.setAndTogether(false);

		// Add security filters
		if (inUsesSecurity)
		{
			orgroup.addMatches("viewasset:blank");
			if (getUser() != null)
			{
				orgroup.addMatches("viewasset:" + getUser().getUserName());

				for (Iterator iterator = getUser().getGroups().iterator(); iterator.hasNext();)
				{
					Group group = (Group) iterator.next();
					orgroup.addMatches("viewasset:" + group.getName());
				}
			}
		}

		for (Iterator iter = checks.iterator(); iter.hasNext();)
		{
			String name = (String) iter.next();
			if (name.startsWith("limittocategory:"))
			{
				String catId = name.substring("limittocategory:".length());
				orgroup.addMatches("category", catId);
			}
		}
		if (!isCategorySelected())
		{
			for (Iterator iter = checks.iterator(); iter.hasNext();)
			{
				String name = (String) iter.next();
				if (name.startsWith("hidecategory:"))
				{
					String catId = name.substring("hidecategory:".length());
					andgroup.addNot("category", catId);
				}
				else if (name.startsWith("excludecategory:"))
				{
					String catId = name.substring("excludecategory:".length());
					andgroup.addNot("category", catId);
				}
			}
			// andgroup.addMatches("category",inSelected.getId(),inSelected.
			// getName());
			// if (includechildren) //We want to include more records unless
			// {
			// for (Iterator iter = inSelected.getChildren().iterator();
			// iter.hasNext();)
			// {
			// Category childCatalog = (Category) iter.next();
			// if (filter != null && filter.indexOf(childCatalog.getId()) > -1)
			// {
			// continue; //skip this catalog
			// }
			// catalogs = catalogs + " OR " + childCatalog.getId();
			// }
			// }
			// search.addMatches("category", "(" + catalogs + ")", "Category
			// matches " + catalog.getName() );//+ ") AND " + not;
			// if( filter != null )
			// {
			// search.addFilter(filter);
			// }
		}
		if (!andgroup.isEmpty() && !orgroup.isEmpty())
		{
			return andgroup.toQuery() + " AND (" + orgroup.toQuery() + ")";
		}
		if (!andgroup.isEmpty())
		{
			return andgroup.toQuery();
		}
		if (!orgroup.isEmpty())
		{
			return "(" + orgroup.toQuery() + ")";
		}
		// if( !group.isEmpty() )
		// {
		// return group.toQuery();
		// }
		// return " NOT catalogs:( " + notfilter.substring(0, notfilter.length()
		// -
		// 2) + ")";
		return null;
	}

	public String getUserFilter(String inKey)
	{
		Element e = getSavedFilters().getElementById(inKey);
		if (e != null)
		{
			return e.getText();
		}
		return null;
	}

	public boolean hasUserFilter(String inField, String inKey)
	{
		String search = inField + ":" + inKey;

		if (getUserFilter(search) == null)
		{
			return false;
		}
		return true;

	}

	public boolean hasCategoryFilter(String inField, String inKey)
	{
		String search = inField + ":" + inKey;

		if (getUserCategoryFilter(search) == null)
		{
			return false;
		}
		return true;
	}

	public String getUserCategoryFilter(String inKey)
	{
		Element e = getSavedCategoryFilters().getElementById(inKey);
		if (e != null)
		{
			return e.getText();
		}
		return null;
	}

	public boolean isCategorySelected()
	{
		return fieldCategorySelected;
	}

	public void setCategorySelected(boolean inCategorySelected)
	{
		fieldCategorySelected = inCategorySelected;
	}

	public User getUser()
	{
		return fieldUser;
	}

	public void setUser(User inUser)
	{
		fieldUser = inUser;
	}

	public XmlFile getSavedFilters()
	{
		return fieldSavedFilters;
	}

	public void setSavedFilters(XmlFile inSavedFilters)
	{
		fieldSavedFilters = inSavedFilters;
	}

	public boolean isIncludeUserFilter()
	{
		return fieldIncludeUserFilter;
	}

	public void setIncludeUserFilter(boolean inIncludeUserFilter)
	{
		fieldIncludeUserFilter = inIncludeUserFilter;
	}

	public void setSavedCategoryFilters(XmlFile inSettings)
	{
		fieldSavedCategoryFilters = inSettings;

	}

	public XmlFile getSavedCategoryFilters()
	{
		return fieldSavedCategoryFilters;
	}

	public void clearCategoryFilters()
	{
		if (getSavedCategoryFilters() != null)
		{
			getSavedCategoryFilters().clear();
		}
	}

	public void addCategoryExclude(String inCat)
	{
		getExcludeCategories().add(inCat);
	}

	public void removeCategoryExclude(String inCat)
	{
		if (fieldExcludeCategories != null)
		{
			getExcludeCategories().remove(inCat);
		}
	}

	// List xconfs = getPageManager().getChildrenPaths("/" + inCatalogId +
	// "/categories/");

	public Set getExcludeCategories()
	{
		if (fieldExcludeCategories == null)
		{
			fieldExcludeCategories = new HashSet();
		}
		return fieldExcludeCategories;
	}

	public boolean hasExcludedCategory(String inId)
	{
		return getExcludeCategories().contains(inId);
	}
}
