package org.entermediadb.asset.search;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.SearchQueryFilter;
import org.openedit.data.Searcher;
import org.openedit.hittracker.SearchQuery;
import org.openedit.hittracker.Term;
import org.openedit.profile.UserProfile;
import org.openedit.users.User;

public class assetSearchQueryFilter implements SearchQueryFilter {
	private static final Log log = LogFactory.getLog(assetSearchQueryFilter.class);

	public SearchQuery  attachFilter(WebPageRequest inPageRequest, Searcher inSearcher, SearchQuery inQuery) 
	{
		boolean enabled = inQuery.isEndUserSearch();
		//log.info( "security filer enabled "  + enabled );
		if (!enabled)
		{
			return inQuery;
		}

		//check for category joins
		if(!inQuery.hasChildren())
		{			
			SearchQuery child = inSearcher.createSearchQuery();
			//viewasset = "admin adminstrators guest designers"
			//goal: current query && (viewasset.contains(username) || viewasset.contains(group0) || ... || viewasset.contains(groupN))
			User currentUser = inPageRequest.getUser();
			Collection<String> ids = null;//new ArrayList<String>();
			
			User user = inPageRequest.getUser();
			if (user == null || !user.isInGroup("administrators"))
			{
				UserProfile profile = inPageRequest.getUserProfile();
				if( profile != null)
				{
					//Get the libraries
					ids = profile.getViewCategories();
				}
				if( ids == null)
				{
					ids = new ArrayList<String>();
					ids.add("none");
				}
				child.addOrsGroup(inSearcher.getDetail("category"), ids);
				child.addExact("owner",user.getId());

				inQuery.setSecurityAttached(true);
				if(!child.isEmpty())
				{
					inQuery.addChildQuery(child);
				}
			}	
		
			SearchQuery filterchild = null;
			for(Term term : inQuery.getTerms() )
			{
				String id = term.getId();
				if( id.contains("."))
				{
					if( filterchild == null)
					{
						filterchild = inSearcher.createSearchQuery();
					}
					String[] typefield = id.split("\\.");
					String type = typefield[0];
					Searcher othersearcher = inSearcher.getSearcherManager().getSearcher(inSearcher.getCatalogId(), type);
					
					SearchQuery othersearch = othersearcher.createSearchQuery();
					//fix the detail id?
					othersearch.addTerm(term);
					
					Collection<Data> parenthits = othersearcher.search(othersearch);
					Collection<Data> libraryhits = null;
					
					if( type.equals("library"))
					{
						libraryhits = parenthits;
					}
					else if( type.equals("librarycollection") )
					{
						Collection<String> libraryids  = new ArrayList();
						for(Data data : parenthits)
						{
							libraryids.add(data.get("library"));
						}	
						libraryhits = inSearcher.getSearcherManager().getSearcher(inSearcher.getCatalogId(), "library").query().orgroup("id", libraryids).search();
					}
					else
					{
						throw new OpenEditException("Asset searches only support Library and Collection joins not: " + type);
					}
					Collection<String> categoryids = new ArrayList();
					for(Data data : libraryhits)
					{
						categoryids.add(data.get("categoryid"));
					}	
					filterchild.addOrsGroup(inSearcher.getDetail("category"), categoryids);
				}
			}
			if( filterchild != null )
			{
				inQuery.addChildQuery(filterchild);
			}
		}

		
		return inQuery;
	}
}
