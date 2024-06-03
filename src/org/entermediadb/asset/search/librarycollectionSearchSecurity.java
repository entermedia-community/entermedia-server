package org.entermediadb.asset.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.openedit.WebPageRequest;
import org.openedit.data.QueryBuilder;
import org.openedit.data.SearchSecurity;
import org.openedit.data.Searcher;
import org.openedit.hittracker.SearchQuery;
import org.openedit.hittracker.Term;
import org.openedit.profile.UserProfile;
import org.openedit.users.Group;

public class librarycollectionSearchSecurity implements SearchSecurity
{
	private static final Log log = LogFactory.getLog(librarycollectionSearchSecurity.class);

	public SearchQuery attachSecurity(WebPageRequest inPageRequest, Searcher inSearcher, SearchQuery inQuery)
	{
		boolean enabled = inQuery.isEndUserSearch();
		//log.info( "security filer enabled "  + enabled );
		if (!enabled)
		{
			
			return inQuery;
		}
		if( inQuery.isSecurityAttached() )
		{
			
			return inQuery;
		}
        String skipfilter = inPageRequest.getContentProperty("skipallsecurity");
        if (Boolean.parseBoolean(skipfilter))
        {
            return inQuery;
        }
        Collection onlytypes = Arrays.asList("0","2","3");
	
        Term bytype = inQuery.getTermByDetailId("collectiontype");
        if( bytype != null)
        {
        	Term ids = inQuery.getTermByDetailId("id");
        	if( ids != null)
        	{
        		log.debug("Specified ids so allowing search without collectiontype");
        		onlytypes = null;
        	}
        }
        
		UserProfile profile = inPageRequest.getUserProfile();
		if (profile != null && profile.isInRole("administrator"))
		{
			SearchQuery child = inSearcher.query()
					.all()
					.notgroup("collectiontype", onlytypes)
					.getQuery();
			inQuery.addChildQuery(child);
			inQuery.setSecurityAttached(true);
			return inQuery;
		}

		MediaArchive archive = (MediaArchive) inPageRequest.getPageValue("mediaarchive");

//		Collection<Category> catshidden = archive.listHiddenCategories(profile.getViewCategories()); //The ones I cant see
//		HashSet toshow = new HashSet(profile.getCollectionIds());
//		for (Iterator iterator = catshidden.iterator(); iterator.hasNext();)
//		{
//			Category hidden = (Category) iterator.next();
//			toshow.remove(hidden.getId());
//		}
		
		Set allowedcats = new HashSet(profile.getViewCategories());

		//@deprecate this code
		for (Iterator iterator = archive.listPublicCategories().iterator(); iterator.hasNext();)
		{
			Category publiccat = (Category) iterator.next();
			allowedcats.add(publiccat);
		}
		if( allowedcats.isEmpty() )
		{
			allowedcats.add("NONE");
		}
		SearchQuery child = inSearcher.query()
				//.orgroup("parentcategories",allowedcats)
				//.notgroup("parentcategories", catshidden)
				.notgroup("collectiontype", onlytypes)
				.getQuery();
		inQuery.addChildQuery(child);

		
		
		
		Collection groupids = new ArrayList();
		UserProfile inUserprofile = inPageRequest.getUserProfile();
		
		if( inUserprofile == null || inUserprofile.getUser() == null)
		{
			groupids.add("anonymous");
		}
		else
		{
			for (Iterator iterator = inUserprofile.getUser().getGroups().iterator(); iterator.hasNext();)
			{
				Group group = (Group) iterator.next();
				groupids.add(group.getId());
			}
		}
		String roleid = null;
		if( inUserprofile != null && inUserprofile.getSettingsGroup() != null)
		{
			roleid = inUserprofile.getSettingsGroup().getId();
		}
		else
		{
			roleid = "anonymous";
		}
		
		String userid = null;
		if( inUserprofile != null)
		{
			userid = inUserprofile.getUserId();
		}
		else
		{
			userid  = "null";
		}

		QueryBuilder builder = inSearcher.query().or().
		orgroup("viewgroups", groupids).
		match("viewroles", roleid).
		match("owner", userid).
		match("viewusers", userid);
		builder.match("securityenabled", "false");
		inQuery.addChildQuery(builder.getQuery());

		//Load all categories 1000
		//Compare to the profile categories and parents
		//run a securty fileter on collectionids
		//inQuery.setSecurityIds(toshow);
		inQuery.setSecurityAttached(true);
		
		//log.info("Collection search " + inQuery.toQuery());
		return inQuery;
	}
}
