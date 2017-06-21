package org.entermediadb.asset.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.Category;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.SearchQueryFilter;
import org.openedit.data.Searcher;
import org.openedit.hittracker.SearchQuery;
import org.openedit.hittracker.Term;
import org.openedit.profile.UserProfile;
import org.openedit.users.User;

public class assetSearchQueryFilter implements SearchQueryFilter 
{
	private static final Log log = LogFactory.getLog(assetSearchQueryFilter.class);

	protected ModuleManager fieldModuleManager;
	
	
	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	
	/**
	 * 
	 * 
OR {
  Any files owned by them 
AND(
 any Approved Assets
 OR ( 
   unless they are explicidly on that collection
   NOT in Collections marked private
 )  
) 
	 * 
	 */
	public SearchQuery attachFilter(WebPageRequest inPageRequest, Searcher inSearcher, SearchQuery inQuery) 
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
			Collection<String> ids = new ArrayList<String>();
			
			User user = inPageRequest.getUser();
			if (user == null || !user.isInGroup("administrators"))
			{
				SearchQuery orchild = inSearcher.createSearchQuery();
				orchild.setAndTogether(false);
				
				UserProfile profile = inPageRequest.getUserProfile();
				if( profile != null && profile.getViewCategories() != null)
				{
					//Get the libraries
					ids.addAll(profile.getViewCategories());
				}

				if( ids.isEmpty() )
				{
					ids.add("none");
				}
				if( user != null)
				{
					orchild.addExact("owner",user.getId());
				}
				//Have clients use the category tree to give permissions as they do now on categories for visibility
				orchild.addOrsGroup(inSearcher.getDetail("category"), ids); //Only shows what people have asked for
				
				
				//Also add to this list public collections
				Collection<Category> privatecats = getMediaArchive(inSearcher.getCatalogId()).listHiddenCategories();
				Collection notshown = new ArrayList();
				for (Iterator iterator = privatecats.iterator(); iterator.hasNext();)
				{
					Category category = (Category)iterator.next();
					if( !ids.contains(category.getId()) )
					{
						notshown.add(category.getId()); //This is a hidden one that is not in the view list
					}
				}				
				SearchQuery child = inSearcher.createSearchQuery();
				child.addChildQuery(orchild);
				//child.addMatches("id", "*");
				if( !notshown.isEmpty() )
				{
					child.addNots("category", notshown );  //Hidden categories that Im not part of
				}
				//log.info( child.toQuery() );
//				Term term = inQuery.getTermByDetailId("editstatus");
//				if( term == null) //TODO: Enforce??
//				{
//					inQuery.addExact("editstatus","6");
//				}
				
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

	protected MediaArchive getMediaArchive(String inCatalogId)
	{
		MediaArchive archive = (MediaArchive)getModuleManager().getBean(inCatalogId,"mediaArchive");
		return archive;
	}
}
