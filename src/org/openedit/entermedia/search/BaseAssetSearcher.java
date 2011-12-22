package org.openedit.entermedia.search;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.data.BaseSearcher;
import org.openedit.data.CompositeData;
import org.openedit.data.PropertyDetailsArchive;
import org.openedit.data.SearcherManager;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.AssetArchive;
import org.openedit.entermedia.AssetPathFinder;
import org.openedit.entermedia.Category;
import org.openedit.entermedia.CategoryArchive;
import org.openedit.entermedia.MediaArchive;
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
	protected CategoryArchive fieldCategoryArchive;
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
			search.setResultType("category");
			
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

	public HitTracker getAllHits(WebPageRequest inReq)
	{
		Category root = null;
		try
		{
			root = getCategoryArchive().getRootCategory();
		}
		catch (OpenEditException e)
		{
			throw new OpenEditRuntimeException(e);
		}
		SearchQuery query = createSearchQuery();
		if (root != null)
		{
			query.addMatches("category", root.getId());
		}
		else
		{
			query.addMatches("category", "index");
		}
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
		if (inData instanceof Asset)
		{
			Asset asset = (Asset) inData;
			getDataConnector().saveData(asset,inUser);
		}
		else if (inData instanceof CompositeData)
		{
			saveCompositeData((CompositeData) inData, inUser);
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

	public CategoryArchive getCategoryArchive()
	{
		if (fieldCategoryArchive == null)
		{
			fieldCategoryArchive = (CategoryArchive)getModuleManager().getBean(getCatalogId(),"categoryArchive");
		}
		return fieldCategoryArchive;
	}

	public void setCategoryArchive(CategoryArchive inCategoryArchive)
	{
		fieldCategoryArchive = inCategoryArchive;
	}

	public AssetArchive getAssetArchive()
	{
		return getMediaArchive().getAssetArchive();
	}
	
	public HitTracker cachedSearch(WebPageRequest inPageRequest, SearchQuery inSearch) throws OpenEditException
	{
		//modify in query if we are using search security
		addShowOnly(inPageRequest, inSearch);
		if(doesIndexSecurely() && !inSearch.isSecurityAttached())
		{
			//viewasset = "admin adminstrators guest designers"
			//goal: current query && (viewasset.contains(username) || viewasset.contains(group0) || ... || viewasset.contains(groupN))
			User currentUser = inPageRequest.getUser();
			StringBuffer buffer = new StringBuffer("true "); //true is for wide open searches
			if (currentUser != null)
			{
				UserProfile profile = inPageRequest.getUserProfile();
				if( profile != null)
				{
					if(profile.getSettingsGroup() != null)
					{
						buffer.append( " sgroup" + profile.getSettingsGroup().getId() );
					}
					String value = profile.getValue("assetadmin");
					if( Boolean.parseBoolean(value) )
					{
						buffer.append(" profileassetadmin");
					}
					value = profile.getValue("viewassets");
					if( Boolean.parseBoolean(value) )
					{
						buffer.append(" profileviewassets");
					}
				}
				for (Iterator iterator = currentUser.getGroups().iterator(); iterator.hasNext();)
				{
					String allow = ((Group)iterator.next()).getId();
					buffer.append(" " + allow);
				}
				buffer.append(" " + currentUser.getUserName());
			}
			inSearch.addOrsGroup("viewasset", buffer.toString().toLowerCase());
			inSearch.setSecurityAttached(true);
		}
		String filter = inPageRequest.findValue("enableprofilefilters");
		if( Boolean.parseBoolean(filter))
		{
			if( inSearch.getTermByDetailId("album") == null )
			{
				//addUserProfileSearchFilters( inPageRequest,inSearch);
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

	public String nextAssetNumber()
	{
		return getDataConnector().nextId();
	}
}
