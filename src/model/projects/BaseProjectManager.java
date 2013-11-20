package model.projects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.profile.UserProfile;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.FilterNode;
import com.openedit.hittracker.HitTracker;

public class BaseProjectManager implements ProjectManager
{
	protected SearcherManager fieldSearcherManager;
	protected String fieldCatalogId;
	/* (non-Javadoc)
	 * @see model.projects.ProjectManager#getCatalogId()
	 */
	@Override
	public String getCatalogId()
	{
		return fieldCatalogId;
	}
	
	/* (non-Javadoc)
	 * @see model.projects.ProjectManager#setCatalogId(java.lang.String)
	 */
	@Override
	public void setCatalogId(String inCatId)
	{
		fieldCatalogId = inCatId;
	}
	
	
	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	/* (non-Javadoc)
	 * @see org.openedit.entermedia.push.PushManager#setSearcherManager(org.openedit.data.SearcherManager)
	 */
	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}
	
	protected Data getCurrentLibrary( UserProfile inProfile)
	{
		if( inProfile != null)
		{
			String lastselectedid = inProfile.get("last_selected_library" );
			if( lastselectedid != null)
			{
				Data library = getSearcherManager().getData(getCatalogId(), "library", lastselectedid );
				return library;
			}
		}
		return null;
	}
	
	public Collection<UserCollection> loadCollections(WebPageRequest inReq)
	{
		//get a library
		Data library = getCurrentLibrary(inReq.getUserProfile());
		if( library != null)
		{
			inReq.putPageValue("selectedlibrary",library);
			
			Searcher assetsearcher = getSearcherManager().getSearcher(getCatalogId(),"asset");
			int assetsize = assetsearcher.query().match("libraries",library.getId()).search(inReq).size();
			inReq.putPageValue("librarysize",assetsize);
			
			Searcher searcher = getSearcherManager().getSearcher(getCatalogId(),"librarycollection");
			HitTracker allcollections = searcher.query().match("library",library.getId()).sort("name").search(inReq);
			//enable filters
			inReq.putPageValue("allcollections", allcollections);
			
			Searcher collectionassetsearcher = getSearcherManager().getSearcher(getCatalogId(),"librarycollectionasset");

			List ids = new ArrayList(allcollections.size());
			for (Iterator iterator = allcollections.iterator(); iterator.hasNext();)
			{
				Data collection = (Data) iterator.next();
				ids.add( collection.getId() );
			}
			FilterNode collectionhits = null;
			HitTracker collectionassets = collectionassetsearcher.query().orgroup("librarycollection",ids).search(inReq); //todo: Cache?
			if( collectionassets != null)
			{
				collectionhits = collectionassets.findFilterNode("librarycollection");
			}
			
			Collection<UserCollection> usercollections = loadUserCollections(allcollections, collectionhits);
			inReq.putPageValue("usercollections", usercollections);
			return usercollections;
		}
		return Collections.EMPTY_LIST;
				
		//search 
		//Searcher searcher = getSearcherManager().getSearcher(getMediaArchive().getCatalogId(),"librarycollection") )
		//HitTracker labels = searcher.query().match("library",$library.getId()).sort("name").search() )
		
	}
	protected Collection<UserCollection> loadUserCollections(Collection<Data> allcollections, FilterNode collectionhits) 
	{
		List usercollections = new ArrayList(allcollections.size());
		for (Data collection : allcollections)
		{
			UserCollection uc = new UserCollection();
			uc.setCollection(collection);
			if( collectionhits != null)
			{
				int assetcount = collectionhits.getCount(collection.getId());
				uc.setAssetCount(assetcount);
			}
			usercollections.add(uc);
		}
		 
		return usercollections;
	}
	
}

