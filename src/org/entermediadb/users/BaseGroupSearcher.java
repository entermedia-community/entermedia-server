/*
 * Created on Oct 19, 2004
 */
package org.entermediadb.users;

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.BaseSearcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.ListHitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.users.Group;
import org.openedit.users.GroupSearcher;
import org.openedit.users.User;
import org.openedit.users.UserManager;
import org.openedit.util.PathUtilities;

/**
 * @author cburkey
 *
 */
public class BaseGroupSearcher extends BaseSearcher implements GroupSearcher
{
	private static final Log log = LogFactory.getLog(BaseGroupSearcher.class);
	protected UserManager fieldUserManager;

	public HitTracker getAllHits(WebPageRequest inReq)
	{
		return getUserManager().getGroups();
		//return new ListHitTracker().setList(getCustomerArchive().)
	}
	public HitTracker search(SearchQuery inQ)
	{
		String inQuery = inQ.toQuery();
		int maxNum = 1000;
		ListHitTracker tracker = new ListHitTracker();
		SearchQuery q = createSearchQuery();
		q.addMatches(inQuery);
		tracker.setSearchQuery(q);
		
		  if ( inQuery == null || inQuery.equalsIgnoreCase("all")  || inQuery.length() == 0)
		  {
			  return getAllHits();
		  }
		  inQuery = inQuery.toLowerCase();
		  for (Iterator iter = getUserManager().getGroups().iterator(); iter.hasNext();)
		  {
			Group group = (Group) iter.next();
			if( matches(group.getName(),inQuery) ) 
			{
				tracker.add(group);
			}
			if ( tracker.getTotal() >= maxNum)
			{
				break;
			}
		  }
		return tracker;
	}
	protected boolean matches(String inText, String inQuery)
	{
		if ( inText != null)
		{
			if (PathUtilities.match(inText, inQuery) )
			{
				return true;		
			}				
		}
		return false;
	}

	public UserManager getUserManager()
	{
		return fieldUserManager;
	}

	public void setUserManager(UserManager inUserManager)
	{
		fieldUserManager = inUserManager;
	}

	public void reIndexAll() throws OpenEditException
	{
	}
	public void clearIndex()
	{
		// TODO Auto-generated method stub
	}		
	public SearchQuery createSearchQuery()
	{
		return new SearchQuery();
	}
	public String getIndexId()
	{
		// TODO Auto-generated method stub
		return null;
	}
	public Object searchById(String inId)
	{
		return getUserManager().getGroup(inId);
	}
	public void saveData(Data inData, User inUser)
	{
		getUserManager().saveGroup((Group)inData);
	}
	/* (non-Javadoc)
	 * @see org.openedit.users.GroupSearche#getGroup(java.lang.String)
	 */
	public Group getGroup(String inGroupId)
	{
		return getUserManager().getGroup(inGroupId);
	}
	

	public void saveAllData(Collection inList, User inUser)
	{
	}
	
	public void delete(Data inData, User inUser)
	{
		
	}
	
	public void deleteAll(User inUser)
	{
		
	}
	
	public void setCatalogId(String inCatalogId)
	{
		if( !"system".equals(inCatalogId) )
		{
			throw new OpenEditException("group searcher should only be a system level not " + inCatalogId );
		}
		super.setCatalogId(inCatalogId);
	}
}
