package org.openedit.entermedia.search;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.data.BaseSearcher;
import org.openedit.data.CompositeData;
import org.openedit.data.PropertyDetailsArchive;
import org.openedit.data.SearcherManager;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.AssetArchive;
import org.openedit.entermedia.Category;
import org.openedit.entermedia.CategoryArchive;
import org.openedit.entermedia.CompositeAsset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.xmldb.CategorySearcher;
import org.openedit.profile.UserProfile;

import com.openedit.ModuleManager;
import com.openedit.OpenEditException;
import com.openedit.OpenEditRuntimeException;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.page.PageSettings;
import com.openedit.page.manage.PageManager;
import com.openedit.users.Group;
import com.openedit.users.User;


public class BaseAssetSearcher extends BaseSearcher implements AssetSearcher
{
	static final Log log = LogFactory.getLog(BaseAssetSearcher.class);
	protected static final String CATALOGIDX = "catalogid";
	protected static final String CATEGORYID = "categoryid";
	protected DataConnector fieldDataConnector;

	protected DecimalFormat fieldDecimalFormatter;
	protected PageManager fieldPageManager;
	private Boolean fieldUsesSearchSecurity;
	protected ModuleManager fieldModuleManager;
	protected CategorySearcher fieldCategorySearcher;
	protected MediaArchive fieldMediaArchive;
	
	public BaseAssetSearcher()
	{
		setFireEvents(true);
	}
	
	public Data createNewData()
	{
		return getDataConnector().createNewData();
	}

	/**
	 * @see org.openedit.entermedia.search.AssetSearcher#fieldSearch(com.openedit.WebPageRequest,
	 *      org.openedit.store.Cart)
	 */
	public HitTracker searchCategories(WebPageRequest inPageRequest, Category catalog) throws Exception
	{
		SearchQuery search = createSearchQuery();

		if (catalog != null)
		{
			inPageRequest.putPageValue("catalog", catalog); // @deprecated
			inPageRequest.putPageValue("category", catalog); // @deprecated

			String actualid = catalog.getId();
			if (catalog.getLinkedToCategoryId() != null)
			{
				actualid = catalog.getLinkedToCategoryId();
			}

			search.addMatches("category", actualid);
			//search.setResultType("category");
			
			HitTracker res = cachedSearch(inPageRequest, search);	
			return res;
		}
		return null;
	}
	

	public void searchExactCategories(WebPageRequest inPageRequest, Category catalog) throws Exception
	{
		SearchQuery search = createSearchQuery();
				
		if (catalog != null)
		{
			inPageRequest.putPageValue("catalog", catalog); // @deprecated
			inPageRequest.putPageValue("category", catalog); // @deprecated

			// to error

			// boolean includechildren = false;
			// if (catalog.getParentCatalog() == null) // this is the root level
			// {
			// includechildren = true; // since assets dont mark themself in the
			// // index catalog
			// }
			String actualid = catalog.getId();
			if (catalog.getLinkedToCategoryId() != null)
			{
				actualid = catalog.getLinkedToCategoryId();
			}

			search.addMatches("category-exact", actualid);

//			Link crumb = buildLink(catalog, inPageRequest.findValue("url-prefix"));
//			inPageRequest.putSessionValue("crumb", crumb);

			String sortBy = catalog.get("sortfield");
			search.setSortBy(sortBy);

			cachedSearch(inPageRequest, search);	
		}
	}

	public DataConnector getDataConnector()
	{
		return fieldDataConnector;
	}
	public void setDataConnector(DataConnector inDataConnector)
	{
		this.fieldDataConnector = inDataConnector;
	}
	public synchronized void updateIndex(Data inAssets)
	{
		getDataConnector().updateIndex(inAssets);
	}
	public synchronized void updateIndex(List inAssets, boolean inOptimize)
	{
		getDataConnector().updateIndex(inAssets, inOptimize);

		//TODO: Update ids path lookup? mem leak?
//		if (asset.getId() != null)
//		{
//			getAssetPaths().put(asset.getId(), asset.getSourcePath()); // This
//		}

	}

	public void reIndexAll()
	{
		getDataConnector().reIndexAll();
		fieldUsesSearchSecurity = null;
	}

	private boolean doesIndexSecurely()
	{
		if (fieldUsesSearchSecurity == null)
		{
			//PageSettings settings = getPageManager().getPageSettingsManager().getPageSettings("/" + getCatalogId() + "/assets/");
			PageSettings settings = getPageManager().getPageSettingsManager().getPageSettings("/" + getCatalogId());
			String val = settings.getPropertyValue("usessearchsecurity", null);
			if (val != null && Boolean.valueOf(val).booleanValue())
			{
				fieldUsesSearchSecurity = Boolean.TRUE;
			}
			else
			{
				fieldUsesSearchSecurity = Boolean.FALSE;
			}
		}
		return fieldUsesSearchSecurity.booleanValue();
	}

	public MediaArchive getMediaArchive()
	{
		if (fieldMediaArchive == null)
		{
			fieldMediaArchive = (MediaArchive) getModuleManager().getBean(getCatalogId(), "mediaArchive");
		}

		return fieldMediaArchive;
	}
	
	public void deleteData(Data inData)
	{
		getAssetArchive().deleteAsset((Asset)inData);
	}
	
	public void deleteFromIndex(Asset inAsset)
	{
		deleteFromIndex(inAsset.getId());
	}

	public void deleteFromIndex(String inId)
	{
		getDataConnector().deleteFromIndex(inId);
	}

	public void deleteFromIndex(HitTracker inOld)
	{
		getDataConnector().deleteFromIndex(inOld);
	}
	@Override
	public HitTracker getAllHits()
	{
//		Category root = null;
//		try
//		{
//			root = getCategorySearcher().getRootCategory();
//		}
//		catch (OpenEditException e)
//		{
//			throw new OpenEditRuntimeException(e);
//		}
		SearchQuery query = createSearchQuery();
		query.addMatches("id", "*");
//		if (root != null)
//		{
//			query.addMatches("category", root.getId());
//		}
//		else
//		{
//			query.addMatches("category", "index");
//		}
		query.addSortBy("id");
		return search(query);
	}
	public HitTracker getAllHits(WebPageRequest inReq)
	{
//		Category root = null;
//		try
//		{
//			root = getCategorySearcher().getRootCategory();
//		}
//		catch (OpenEditException e)
//		{
//			throw new OpenEditRuntimeException(e);
//		}
		SearchQuery query = createSearchQuery();
		query.addMatches("id", "*");
//		if (root != null)
//		{
//			query.addMatches("category", root.getId());
//		}
//		else
//		{
//			query.addMatches("category", "index");
//		}
		if (inReq == null)
		{
			return search(query);
		}
		else
		{
			String sort = inReq.getRequestParameter("sortby");
			if( sort != null)
			{
				query.addSortBy(sort);
			}
			return cachedSearch(inReq, query);
		}
	}

	public void saveData(Data inData, User inUser)
	{
		if (inData instanceof CompositeAsset)
		{
			CompositeAsset asset = (CompositeAsset)inData;
			asset.saveChanges();
		}
		else if (inData instanceof CompositeData)
		{
			saveCompositeData((CompositeData) inData, inUser);
		}
		else if (inData instanceof Asset)
		{
			Asset asset = (Asset) inData;
			getDataConnector().saveData(asset,inUser);
		}
		else
		{
			throw new OpenEditException("Not an asset");
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

	public Object searchByField(String inField, String inValue)
	{
		return getDataConnector().searchByField(inField,inValue);
	}
	public ModuleManager getModuleManager()
	{
		return getSearcherManager().getModuleManager();
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	public CategorySearcher getCategorySearcher()
	{
		if (fieldCategorySearcher == null)
		{
			fieldCategorySearcher = (CategorySearcher)getSearcherManager().getSearcher(getCatalogId(), "category");
		}
		return fieldCategorySearcher;
	}

	public void setCategorySearcher(CategorySearcher inCategorySearcher)
	{
		fieldCategorySearcher = inCategorySearcher;
	}

	public AssetArchive getAssetArchive()
	{
		return getMediaArchive().getAssetArchive();
	}
	
	public HitTracker cachedSearch(WebPageRequest inPageRequest, SearchQuery inSearch) throws OpenEditException
	{
		boolean filterstuff = true;
		String datamanager = inPageRequest.getContentProperty("showallfiles");
		if( Boolean.parseBoolean(datamanager) )
		{
			filterstuff = false;
		}
		//modify in query if we are using search security
		if( filterstuff )
		{
			addShowOnly(inPageRequest, inSearch);
		}
		if(filterstuff && doesIndexSecurely() && !inSearch.isSecurityAttached())
		{
			//TODO: This should be in a child query with 	child.setFilter(true);
			
			//viewasset = "admin adminstrators guest designers"
			//goal: current query && (viewasset.contains(username) || viewasset.contains(group0) || ... || viewasset.contains(groupN))
			User currentUser = inPageRequest.getUser();
			StringBuffer buffer = new StringBuffer("true "); //true is for wide open searches

			UserProfile profile = inPageRequest.getUserProfile();
			if( profile != null)
			{
				//Get the libraries
				Collection libraries = profile.getCombinedLibraries();
				if( libraries != null)
				{
					for (Iterator iterator = libraries.iterator(); iterator	.hasNext();) 
					{
						String library = (String) iterator.next();
						buffer.append( " library_" + library);
					}
				}
			}

			if (currentUser != null)
			{
				for (Iterator iterator = currentUser.getGroups().iterator(); iterator.hasNext();)
				{
					String allow = ((Group)iterator.next()).getId();
					buffer.append(" group_" + allow);
				}
				buffer.append(" user_" + currentUser.getUserName());
			}
			inSearch.addOrsGroup("viewasset", buffer.toString().toLowerCase());
			inSearch.setSecurityAttached(true);
		}
		String filter = inPageRequest.findValue("enableprofilefilters");
		if( filterstuff && Boolean.parseBoolean(filter))
		{
			if( inSearch.getTermByDetailId("album") == null )
			{
				addUserProfileSearchFilters( inPageRequest,inSearch);
			}
		}

		HitTracker hits = super.cachedSearch(inPageRequest, inSearch);

		return hits;
	}

	public SearchQuery createSearchQuery() 
	{
		return getDataConnector().createSearchQuery();
	}

	public String getIndexId() 
	{
		return getDataConnector().getIndexId();
	}

	public void clearIndex() 
	{
		getDataConnector().clearIndex();
	}

	public void deleteAll(User inUser) 
	{
		getDataConnector().deleteAll(inUser);		
	}

	public void delete(Data inData, User inUser) 
	{
		getDataConnector().delete(inData, inUser);
	}

	public void saveAllData(Collection<Data> inAll, User inUser) 
	{
		getDataConnector().saveAllData(inAll,inUser);
	}

	public HitTracker search(SearchQuery inQuery) 
	{
		return getDataConnector().search(inQuery);
	}

	public void flush() 
	{
		getDataConnector().flush();	
	}

	public void setCatalogId(String inCatalogId) 
	{
		super.setCatalogId(inCatalogId);
		getDataConnector().setCatalogId(inCatalogId);
	}
	public void setSearchType(String inSearchType) 
	{
		super.setSearchType(inSearchType);
		getDataConnector().setSearchType(inSearchType);
	}
	public void setPropertyDetailsArchive(PropertyDetailsArchive inArchive) 
	{
		super.setPropertyDetailsArchive(inArchive);
		getDataConnector().setPropertyDetailsArchive(inArchive);
	}
	public void setSearcherManager(SearcherManager inManager) 
	{
		super.setSearcherManager(inManager);
		getDataConnector().setSearcherManager(inManager);
	}
	
	public boolean hasChanged(HitTracker inTracker)
	{
		return getDataConnector().hasChanged(inTracker);
	}
	public String nextId()
	{
		//throw new IllegalAccessError("nextId Not implemented");
		return getDataConnector().nextId();
	}
	public String nextAssetNumber()
	{
		return nextId();
	}

	
	public Asset getAssetBySourcePath(String inSourcepath, boolean inAutocreate)
	{
		return (Asset) getDataConnector().getDataBySourcePath(inSourcepath, inAutocreate);
		//return getAssetArchive().getAssetBySourcePath(inSourcepath, inAutocreate);
	}

	
	public Asset getAssetBySourcePath(String inSourcepath)
	{
	return (Asset) getDataConnector().getDataBySourcePath(inSourcepath);
	}
	
//	public void updateFilters(WebPageRequest inReq) throws OpenEditException
//	{
//	updateFilters(inReq);
//	}


}
