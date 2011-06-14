/*
 * Created on Oct 19, 2004
 */
package org.openedit.users;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.data.BaseSearcher;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.ListHitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.users.Group;
import com.openedit.users.User;
import com.openedit.users.UserManager;
import com.openedit.util.PathUtilities;

/**
 * @author cburkey
 * @deprecated use LuceneUserSearcher
 */
public class UserManagerSearcher extends BaseSearcher implements UserSearcher
{
	private static final Log log = LogFactory.getLog(UserSearcher.class);
	protected UserManager fieldUserManager;

	public HitTracker getAllHits(WebPageRequest inReq) 
	{
		SearchQuery q = createSearchQuery();
		q.addMatches("all");
		return search(q);
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
			  for (Iterator iter = getUserManager().listUserNames().iterator(); iter.hasNext() && tracker.getTotal() < maxNum;)
			  {
				String username = (String) iter.next();
				User user = getUserManager().getUser(username);
				tracker.add(user);
			  }
			  return tracker;
		  }
		  inQuery = inQuery.toLowerCase();
		  for (Iterator iter = getUserManager().listUserNames().iterator(); iter.hasNext();)
		  {
			String username = (String) iter.next();
			if( matches(username,inQuery) ) 
			{
				User user = getUserManager().getUser(username);
				tracker.add(user);
			}
			else if( maxNum < 1001)
			{
				//check email
				User user = getUserManager().getUser(username);
				for (Iterator iterator = user.getProperties().values().iterator(); iterator.hasNext();)
				{
					String val = (String) iterator.next();
					if( matches(val,inQuery ) )
					{
						tracker.add(user);
					}
				}
				
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
		return getUserManager().getUser(inId);
	}

	public void saveData(Object inData, User inUser)
	{
		if( inData instanceof User)
		{
			getUserManager().saveUser( (User)inData );
		}
		
	}

	/* (non-Javadoc)
	 * @see org.openedit.users.UserSearcherI#getUser(java.lang.String)
	 */
	public User getUser(String inAccount)
	{
		User user = (User)searchById(inAccount);
		return user;
	}

	/* (non-Javadoc)
	 * @see org.openedit.users.UserSearcherI#getUserByEmail(java.lang.String)
	 */
	public User getUserByEmail(String inEmail)
	{
		return getUserManager().getUserByEmail(inEmail);
	}

	public HitTracker getUsersInGroup(Group inGroup)
	{
		return getUserManager().getUsersInGroup(inGroup);
	}

	public void saveUsers(List userstosave, User user) {
		// TODO Auto-generated method stub
		
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

}
