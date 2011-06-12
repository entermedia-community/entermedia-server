package org.openedit.entermedia.search;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.FSDirectory;
import org.openedit.Data;
import org.openedit.data.CompositeData;
import org.openedit.data.PropertyDetails;
import org.openedit.data.lucene.BaseLuceneSearcher;
import org.openedit.data.lucene.CompositeAnalyzer;
import org.openedit.data.lucene.NullAnalyzer;
import org.openedit.data.lucene.RecordLookUpAnalyzer;
import org.openedit.data.lucene.StemmerAnalyzer;
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
import com.openedit.page.Page;
import com.openedit.page.PageSettings;
import com.openedit.page.manage.PageManager;
import com.openedit.users.Group;
import com.openedit.users.User;
import com.openedit.util.FileUtils;


public class AssetLuceneSearcher extends BaseLuceneSearcher implements AssetSearcher, AssetPathFinder
{
	static final Log log = LogFactory.getLog(AssetLuceneSearcher.class);
	protected static final String CATALOGIDX = "catalogid";
	protected static final String CATEGORYID = "categoryid";
	protected DecimalFormat fieldDecimalFormatter;
	protected PageManager fieldPageManager;
	protected Map fieldAssetPaths;
	private Boolean fieldUsesSearchSecurity;
	protected AssetLuceneIndexer fieldIndexer;
	protected ModuleManager fieldModuleManager;
	protected CategoryArchive fieldCategoryArchive;
	protected MediaArchive fieldMediaArchive;
	
	public AssetLuceneSearcher()
	{
		setFireEvents(true);
	}
	
	public Data createNewData()
	{
		Asset temp = new Asset();
		return temp;
		
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

	public void updateIndex(Asset inAsset)
	{
		List all = new ArrayList(1);
		all.add(inAsset);
		updateIndex(all, false);
		clearIndex(); // Does not flush because it will flush if needed
		// anyways on a search

	}

	public Analyzer getAnalyzer()
	{
		if (fieldAnalyzer == null)
		{
			CompositeAnalyzer composite = new CompositeAnalyzer();
			composite.setAnalyzer("description", new StemmerAnalyzer());
			composite.setAnalyzer("id", new NullAnalyzer());
			composite.setAnalyzer("foldersourcepath", new NullAnalyzer());
			composite.setAnalyzer("sourcepath", new NullAnalyzer());
			RecordLookUpAnalyzer record = new RecordLookUpAnalyzer();
			record.setUseTokens(false);
			composite.setAnalyzer("cumulusid", record);
			composite.setAnalyzer("name_sortable", record);
			fieldAnalyzer = composite;
		}
		return fieldAnalyzer;
	}

	protected AssetLuceneIndexer getIndexer()
	{
		if (fieldIndexer == null)
		{
			fieldIndexer = new AssetLuceneIndexer();
			fieldIndexer.setAnalyzer(getAnalyzer());
			fieldIndexer.setSearcherManager(getSearcherManager());
			fieldIndexer.setUsesSearchSecurity(doesIndexSecurely());
			fieldIndexer.setNumberUtils(getNumberUtils());
			fieldIndexer.setRootDirectory(getRootDirectory());
			fieldIndexer.setMediaArchive(getMediaArchive());
			if(getMediaArchive().getAssetSecurityArchive() == null)
			{
				log.error("Asset Security Archive Not Set");
			}
			fieldIndexer.setAssetSecurityArchive(getMediaArchive().getAssetSecurityArchive());
		}
		return fieldIndexer;
	}

	public synchronized void updateIndex(List inAssets, boolean inOptimize)
	{
		if (log.isDebugEnabled())
		{
			log.debug("update index");
		}

		try
		{
			PropertyDetails details = getPropertyDetails();

			for (Iterator iter = inAssets.iterator(); iter.hasNext();)
			{
				Asset asset = (Asset) iter.next();
				getIndexer().populateAsset(getIndexWriter(), asset, false, details);
				if (asset.getId() != null)
				{
					getAssetPaths().put(asset.getId(), asset.getSourcePath()); // This
				}
			}
			if (inOptimize)
			{
				getIndexWriter().optimize();
				log.info("Optimized");
			}

			if (inOptimize)
			{
				flush();
			}
			else if (inAssets.size() > 100)
			{
				flush();
			}
			else
			{
				clearIndex();
			}
			//else will be flushed next time someone searches. This is a key performance improvement for things like voting that need to be fast
			//BaseLuceneSearcher implements Shutdownable
		}
		catch (Exception ex)
		{
			clearIndex(); //try to recover
			if (ex instanceof OpenEditException)
			{
				throw (OpenEditException) ex;
			}
			throw new OpenEditException(ex);
		}
	}

	protected void reIndexAll(IndexWriter writer)
	{
		// http://www.onjava.com/pub/a/onjava/2003/03/05/lucene.html
		// http://www.onjava.com/pub/a/onjava/2003/03/05/lucene.html?page=2
		// writer.mergeFactor = 10;
		writer.setMergeFactor(100);
		writer.setMaxBufferedDocs(2000);

		try
		{
			getAssetPaths().clear();
			
			AssetLuceneIndexAll reindexer = new AssetLuceneIndexAll();
			reindexer.setWriter(writer);
			reindexer.setPageManager(getPageManager());
			reindexer.setIndexer(getIndexer());
			reindexer.setMediaArchive(getMediaArchive());
			
			/* Search in the new path, if it exists */
			Page root = getPageManager().getPage("/WEB-INF/data/" + getCatalogId() + "/assets/");
			if( root.exists())
			{
				reindexer.setRootPath(root.getPath());
				reindexer.process();
			}
			
			/* Search in the old place */
			reindexer.setRootPath("/" + getCatalogId() + "/assets/");
			reindexer.process();
			
			log.info("Reindex started on with " + reindexer.getExecCount() + " assets");
			writer.optimize();

		}
		catch(Exception ex)
		{
			throw new OpenEditException(ex);
		}
		// HitCollector
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
		// TODO Auto-generated method stub
		log.info("delete from index " + inId);

		try
		{
			String id = inId.toLowerCase(); // Since it is tokenized
			Term term = new Term("id", id);

			getIndexWriter().deleteDocuments(term);
			clearIndex();
		}
		catch (IOException ex)
		{
			throw new OpenEditException(ex);
		}
	}

	public void deleteFromIndex(HitTracker inOld)
	{
		if (inOld.size() == 0)
		{
			return;
		}
		Term[] all = new Term[inOld.getTotal()];
		for (int i = 0; i < all.length; i++)
		{
			Object hit = (Object) inOld.get(i);
			String id = inOld.getValue(hit, "id");
			Term term = new Term("id", id);
			all[i] = term;
		}
		try
		{
			getIndexWriter().deleteDocuments(all);
		}
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}

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

	public void saveData(Object inData, User inUser)
	{
		if (inData instanceof Asset)
		{
			Asset asset = (Asset) inData;
			getAssetArchive().saveAsset(asset);
			updateIndex(asset);
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

	public String idToPath(String inAssetId)
	{
		String path = (String) getAssetPaths().get(inAssetId);
		if (path == null && inAssetId != null)
		{
			SearchQuery query = createSearchQuery();
			query.addExact("id", inAssetId);

			HitTracker hits = search(query.toQuery(), query.getSorts());
			if (hits.size() > 0)
			{
				Data hit = hits.get(0);
				path = hit.getSourcePath();
//				path = hits.getValue(hit,"sourcepath");
				getAssetPaths().put(inAssetId, path);
			}
			else
			{
				log.info("No such asset in index: " + inAssetId);
			}
		}
		return path;
	}

	public Map getAssetPaths()
	{
		if (fieldAssetPaths == null)
		{
			fieldAssetPaths = new HashMap();
		}
		return fieldAssetPaths;
	}

	public Object searchById(String inId)
	{
		String path = idToPath(inId);
		if (path == null)
		{
			return null;
		}
		return getAssetArchive().getAssetBySourcePath(path);
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

	public boolean checkHeights(HitTracker tracker)
	{
		if( tracker != null)
		{
			for (Iterator iterator = tracker.getPageOfHits().iterator(); iterator.hasNext();)
			{
				Data data = (Data) iterator.next();
				String w = data.get("width");
				if(w != null )
				{
					String h = data.get("height");
					if( h != null && Integer.parseInt(h) > Integer.parseInt(w))
					{
						return true;
					}
				}
				else
				{
					return true;
				}
			}
		}
		return false;
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
			inSearch.addOrsGroup("viewasset", buffer.toString());
			inSearch.setSecurityAttached(true);
		}
		String filter = inPageRequest.findValue("enableprofilefilters");
		if( Boolean.parseBoolean(filter))
		{
			if( inSearch.getTermByDetailId("album") == null )
			{
				addUserProfileSearchFilters( inPageRequest,inSearch);
			}
		}

		HitTracker hits = super.cachedSearch(inPageRequest, inSearch);

		return hits;
	}

}
