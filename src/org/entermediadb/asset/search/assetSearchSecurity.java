package org.entermediadb.asset.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.SearchSecurity;
import org.openedit.data.Searcher;
import org.openedit.hittracker.SearchQuery;
import org.openedit.hittracker.Term;
import org.openedit.profile.UserProfile;
import org.openedit.users.User;

public class assetSearchSecurity implements SearchSecurity
{
	private static final Log log = LogFactory.getLog(assetSearchSecurity.class);

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
	 * OR { Any files owned by them AND( any Approved Assets OR ( unless they
	 * are explicidly on that collection NOT in Collections marked private ) )
	 * 
	 */
	public SearchQuery attachSecurity(WebPageRequest inPageRequest, Searcher inSearcher, SearchQuery inQuery)
	{
		
		if (!inQuery.isEndUserSearch())
        {
            return inQuery;
        }
        String skipfilter = inPageRequest.getContentProperty("assetskipfilter");
        if (Boolean.parseBoolean(skipfilter))
        {
            return inQuery;
        }
        
		//log.info( "security filer enabled "  + enabled );

		//check for category joins
		if (!inQuery.isSecurityAttached() )
		{
			User user = inPageRequest.getUser();
			UserProfile profile = inPageRequest.getUserProfile();
			String profilefilters = profile.get(inSearcher.getSearchType() + "showonly");
			if(profilefilters != null && profilefilters.length() != 0) 
			{
				inSearcher.addShowOnlyFilter(inPageRequest, profilefilters, inQuery); //TODO: Depregate this approach
			}
			
			SearchQuery required = null;

			if(inPageRequest.hasPermission("hidedeletedassets"))
			{
				if(inQuery.getTermByDetailId("editstatus") == null)
				{
					required = inSearcher.createSearchQuery();
					required.addNot("editstatus", "7");
					//required.addNot("deleted", "true"); //Ian?
					if (profile != null && profile.isInRole("administrator"))
					{
						inQuery.addChildQuery(required); //Short cut
						addJoins(inPageRequest,inSearcher,inQuery);
						return inQuery;
					}
				}
			}
			if (profile != null && profile.isInRole("administrator"))  //OR			//Boolean canviewallassets = (Boolean) inPageRequest.getPageValue("canviewallassets");
			{
				addJoins(inPageRequest,inSearcher,inQuery);
				inQuery.setSecurityAttached(true);
				return inQuery;					
			}
			
			
			if( required == null)
			{
				required = inSearcher.createSearchQuery();
			}
			
//			Collection allowedassetstypes = profile.getValues("hideassettype");
//			if( allowedassetstypes != null && !allowedassetstypes.isEmpty())
//			{
//				required.addNots("assettype", allowedassetstypes);
//			}
			
						
			SearchQuery orchild = inSearcher.createSearchQuery();
			orchild.setAndTogether(false);
			
			Set ids = new HashSet();

			MediaArchive mediaArchive = getMediaArchive(inSearcher.getCatalogId());
			Collection allowed = new ArrayList(mediaArchive.listPublicCategories() );
			
			
			Collection canview = profile.getViewCategories(); //This has entities folders in it
			if( canview != null )
			{
				allowed.addAll(canview);
			}
			for (Iterator iterator = allowed.iterator(); iterator.hasNext();)
			{
				Category allowedcat = (Category) iterator.next();
				ids.add(allowedcat.getId());
			}
			if (ids.isEmpty())
			{
				ids.add("none");
			}
			if (user != null)
			{
				orchild.addExact("owner", user.getId());
			}
			
			Boolean caneditdata = (Boolean) inPageRequest.getPageValue("caneditcollection");
			String editstatus = null;
			if (caneditdata == null || !caneditdata )
			{
					Boolean showpendingassets = (Boolean) inPageRequest.getPageValue("canshowpendingassets");
					if(showpendingassets == null || !showpendingassets)  //False
					{
						editstatus = "6";  //Approved only
					}
				
			}
//			boolean onlyapproved = inPageRequest.hasPermission("showonlyapprovedassets"); //Legacy emshare
//			if(editstatus == null && onlyapproved ) 
//			{
//				editstatus="6";
//			}
			
			if(editstatus != null) {
				//OWNERS can always see their assets (from orchild.addExact))
				SearchQuery hidependingchild = inSearcher.createSearchQuery();
				hidependingchild.addOrsGroup(inSearcher.getDetail("category"), ids); //Only shows what people have asked for
				hidependingchild.addExact("editstatus", editstatus);
				
				orchild.addChildQuery(hidependingchild);
			}			
		
			else
			{
				orchild.addOrsGroup(inSearcher.getDetail("category"), ids); //Only shows what people have asked for
			}
			required.addChildQuery(orchild);

			//Also add to this list public collections
//			Collection<Category> privatecats = mediaArchive.listHiddenCategories(profile.getViewCategories());  //Cant be hidden and public at the same time
//			Collection<String> notshown = new ArrayList<String>();
//			for (Iterator iterator = privatecats.iterator(); iterator.hasNext();)
//			{
//				Category cat = (Category) iterator.next();
//				notshown.add(cat.getId());
//			}
//			//child.addMatches("id", "*");
//			if (!privatecats.isEmpty())
//			{
//				required.addNots("category", privatecats); //Hidden categories that Im not part of
//			}
			
			inQuery.setSecurityAttached(true);
			if (!required.isEmpty())
			{
				inQuery.addChildQuery(required);
			}
			addJoins(inPageRequest,inSearcher,inQuery);

		}

		return inQuery;
	}
	
	protected void addJoins(WebPageRequest inPageRequest, Searcher inSearcher, SearchQuery inQuery) 
	{
		// TODO Auto-generated method stub
		SearchQuery filterchild = null;
		for (Term term : inQuery.getTerms())
		{
			String type = term.getDetail().getSearchType();
			if (type == null)
			{
				continue;
			}
			if (!type.equals("library") && !type.equals("librarycollection"))  //Join searches
			{
				continue;
			}
			if (filterchild == null)
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

			if (type.equals("library"))
			{
				for (Data data : parenthits)
				{
					categoryids.add(data.get("categoryid"));
				}
			}
			else if (type.equals("librarycollection"))
			{
				//Since we found collections, find the correct 
				for (Data data : parenthits)
				{
					categoryids.add(data.get("rootcategory"));
				}
			}
			else
			{
				throw new OpenEditException("Asset searches only support Library and Collection joins not: " + type);
			}
			if (categoryids.isEmpty())
			{
				categoryids.add("nocategoryhits");
			}
			
			filterchild.addOrsGroup(inSearcher.getDetail("category"), categoryids); //This will filter in specific assets
		}
		if (filterchild != null)
		{
			inQuery.addChildQuery(filterchild);
		}	
	}

	protected MediaArchive getMediaArchive(String inCatalogId)
	{
		MediaArchive archive = (MediaArchive) getModuleManager().getBean(inCatalogId, "mediaArchive");
		return archive;
	}
}
