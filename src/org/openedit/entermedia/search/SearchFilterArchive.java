package org.openedit.entermedia.search;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.openedit.xml.XmlArchive;
import org.openedit.xml.XmlFile;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.page.Page;
import com.openedit.page.Permission;
import com.openedit.page.manage.PageManager;
import com.openedit.users.BaseUser;
import com.openedit.users.User;
import com.openedit.util.PathUtilities;

public class SearchFilterArchive
{
	protected XmlArchive fieldXmlArchive;
	protected Map fieldCache;
	protected PageManager fieldPageManager;

	public void clearUserSearchFilter(User inUser, String inCatalogId) throws OpenEditException
	{
		XmlFile settings = getXmlArchive().loadXmlFile(inCatalogId + inUser.getUserName());

		if (settings != null && settings.isExist())
		{
			getXmlArchive().deleteXmlFile(settings);
		}
	}

	// public void savePropertiesXXX(String inName, Types inTypes, User inUser)
	// throws OpenEditException {
	// String path = "/" + getCatalogId() + "/configuration/lists/properties"
	// + inName + ".xml";
	//
	// if (inTypes == null)
	// {
	// inTypes = getXmlArchive().createXmlFile(inName, path);
	// }
	// getXmlArchive().saveXml((XmlFile) inTypes, inUser);
	//
	// }

	public SearchFilter saveUserCategoryFilter(WebPageRequest inReq, List inCategories, User inUser, String inCatalogId) throws Exception
	{
		SearchFilter filter = getSearchFilter(inReq, false, false, inCatalogId);
		filter.clearCategoryFilters();
		filter.addSavedCategoryFilter(inCategories);
		getXmlArchive().saveXml(filter.getSavedCategoryFilters(), inUser);
		return filter;
	}

	public XmlArchive getXmlArchive()
	{
		return fieldXmlArchive;
	}

	public void setXmlArchive(XmlArchive inXmlArchive)
	{
		fieldXmlArchive = inXmlArchive;
	}

	public void saveUserSearchFilter(SearchFilter inFilter) throws Exception
	{
		getXmlArchive().saveXml(inFilter.getSavedFilters(), inFilter.getUser());
	}

	// public void savePropertiesXXX(String inName, Types inTypes, User inUser)
	// throws OpenEditException {
	// String path = "/" + getCatalogId() + "/configuration/lists/properties"
	// + inName + ".xml";
	//
	// if (inTypes == null)
	// {
	// inTypes = getXmlArchive().createXmlFile(inName, path);
	// }
	// getXmlArchive().saveXml((XmlFile) inTypes, inUser);
	//
	// }

	public org.openedit.entermedia.search.SearchFilter getSearchFilter(WebPageRequest inReq, boolean inIncludeSavedFilter, boolean inSelected, String inCatalogId) throws OpenEditException
	{
		
		User inUser = inReq.getUser();

		if (inUser == null)
		{
			inUser = new BaseUser();
			inUser.setUserName("anonymous");
			inUser.setVirtual(true);
		}

		SearchFilter details = (SearchFilter) getCache().get(inUser.getUserName());
		if (details == null)
		{
			details = new SearchFilter();
			details.setUser(inUser);
			getCache().put(inUser.getUserName(), details);
		}
		details.setCategorySelected(inSelected);

		// user specific filters
		// These are the temporarily hidden categories?
		String catpath = "/" + inCatalogId + "/data/userfilters/" + inUser.getUserName() + "categories.xml";
		XmlFile settings = getXmlArchive().getXml(catpath, "filter");
		if (!settings.isExist())
		{
			String altpath = "/" + inCatalogId + "/data/userfilters/defaultcategories.xml";
			XmlFile altsettings = getXmlArchive().getXml(altpath, "filter");
			settings.setRoot(altsettings.getRoot());
		}
		if (settings != null)
		{
			details.setSavedCategoryFilters(settings);
		}
		details.setIncludeUserFilter(inIncludeSavedFilter);

		// saved filters this should happen every time?
		// if (inIncludeSavedFilter)
		// {
		String userfilterpath = "/" + inCatalogId + "/data/userfilters/" + inUser.getUserName() + ".xml";
		settings = getXmlArchive().getXml(userfilterpath, "filter");
		details.setSavedFilters(settings);
		// }

		// /Loop over all the xconfs in categories
		details.getExcludeCategories().clear();
		List xconfs = getPageManager().getChildrenPaths("/" + inCatalogId + "/categories/");
		for (Iterator iterator = xconfs.iterator(); iterator.hasNext();)
		{
			String path = (String) iterator.next();
			if (path.endsWith(".xconf"))
			{
				Page page = getPageManager().getPage(path, true);
				WebPageRequest req = inReq.copy(page);
				Permission filter = page.getPermission("view");
				if (filter != null)
				{
					if (!filter.passes(req))
					{
						details.addCategoryExclude(PathUtilities.extractPageName(path));
					}
				}
			}
		}

		return details;
	}

	public Map getCache()
	{
		if (fieldCache == null)
		{
			fieldCache = new WeakHashMap();
		}
		return fieldCache;
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

	// public XmlFile loadSystemWideFilters(String level) throws
	// OpenEditException {
	// XmlFile settings = null;
	// if (level != null) {
	// settings = loadXml("systemfilters", "searchfilter" + level,
	// "filter");
	// }
	//
	// return settings;
	// }

}
