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
			//viewasset = "admin adminstrators guest designers"
			//goal: current query && (viewasset.contains(username) || viewasset.contains(group0) || ... || viewasset.contains(groupN))
			User currentUser = inPageRequest.getUser();
			Collection<String> ids = null;//new ArrayList<String>();
			
			User user = inPageRequest.getUser();
			if (user == null || !user.isInGroup("administrators"))
			{
				SearchQuery child = inSearcher.createSearchQuery();
				child.setAndTogether(false);
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
				String type = term.getDetail().getSearchType();
				if(type == null )
				{
					continue;
				}
				if( !type.equals("library") && !type.equals("librarycollection"))
				{
					continue;
				}
				if( filterchild == null)
				{
					filterchild = inSearcher.createSearchQuery();
				}
				Searcher othersearcher = inSearcher.getSearcherManager().getSearcher(inSearcher.getCatalogId(), type);
				
				SearchQuery othersearch = othersearcher.createSearchQuery();
				//fix the detail id?
				othersearch.addTerm(term);

				//First find any matching libraries or collections
				Collection<Data> parenthits = othersearcher.search(othersearch);
				Collection<Data> libraryhits = null;
				Collection<String> categoryids = new ArrayList();
				
				if( type.equals("library"))
				{
					for(Data data : parenthits)
					{
						categoryids.add(data.get("categoryid"));
					}	
				}
				else if( type.equals("librarycollection") )
				{
					//Since we found collections, find the correct 
					for(Data data : parenthits)
					{
						categoryids.add(data.get("rootcategory"));
					}	
				}
				else
				{
					throw new OpenEditException("Asset searches only support Library and Collection joins not: " + type);
				}
				if( categoryids.isEmpty())
				{
					categoryids.add("nocategoryhits");
				}
				filterchild.addOrsGroup(inSearcher.getDetail("category"), categoryids);  //This will filter in specific assets
			}
			if( filterchild != null )
			{
				inQuery.addChildQuery(filterchild);
			}
		}

		
		return inQuery;
	}
}
