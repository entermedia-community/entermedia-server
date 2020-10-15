/*
 * Created on Oct 19, 2004
 */
package org.entermediadb.elasticsearch.searchers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.data.PropertyDetails;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.users.BaseUser;
import org.openedit.users.Group;
import org.openedit.users.GroupSearcher;
import org.openedit.users.User;
import org.openedit.users.UserSearcher;
import org.openedit.users.filesystem.XmlUserArchive;
import org.openedit.util.StringEncryption;

/**
 *
 */
public class ElasticUserSearcher extends BaseElasticSearcher implements UserSearcher
{
	private static final Log log = LogFactory.getLog(ElasticUserSearcher.class);
	protected XmlUserArchive fieldXmlUserArchive;
	private User NULLUSER = new BaseUser();
	
	@Override
	public Data createNewData()
	{
		BaseUser user = (BaseUser)super.createNewData();
		user.setGroupSearcher(getGroupSearcher());
		user.setEnabled(true);
		return user;
	}

	public XmlUserArchive getXmlUserArchive() {
		if (fieldXmlUserArchive == null) {
			fieldXmlUserArchive = (XmlUserArchive) getModuleManager().getBean(
					getCatalogId(), "xmlUserArchive");

		}

		return fieldXmlUserArchive;
	}
	
	public void reIndexAll() throws OpenEditException
	{
		log.info("Reindex of customer users directory");
		putMappings();
	
		Collection usernames = getXmlUserArchive().listUserNames();
		if( usernames != null)
		{
			log.info("Indexing " + usernames.size() + " users");
			List users = new ArrayList();
			for (Iterator iterator = usernames.iterator(); iterator.hasNext();)
			{
				String userid = (String) iterator.next();
				try
				{
					User data = (User)createNewData(); 
					data.setId(userid);
					data = getXmlUserArchive().loadUser(data, getGroupSearcher());
					if( data != null)
					{
						users.add(data);
						if( users.size() > 1000)//makes it bulk.
						{
							updateIndex(users, null);
							users.clear();
						}
					}	
					else
					{
						log.error("Could not load user " + userid);
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
					log.error(e);
				}
					
			}	
			updateIndex(users, null);
		}

	}
	
	@Override
	public void reindexInternal() throws OpenEditException
	{
		reIndexAll();
	}
	public void restoreSettings()
	{
		getPropertyDetailsArchive().clearCustomSettings(getSearchType());
		reIndexAll();
	}

	/* (non-Javadoc)
	 * @see org.openedit.users.UserSearcherI#getUser(java.lang.String)
	 */
	public User getUser(String inAccount)
	{
		return getUser(inAccount,false);
	}
	@Override
	public User getUser(String inAccount, boolean inCached)
	{
		if( inAccount == null)
		{
			return null;
		}
		if( inCached)
		{
			User user = (User)getCacheManager().get("usercache",inAccount);
			if( user == null)
			{
				user = (User)searchById(inAccount);
				if( user == null)
				{
					user = NULLUSER;
				}
				getCacheManager().put("usercache", inAccount, user);
			}
			if( user == NULLUSER)
			{
				return null;
			}
			return user;
		}
		else
		{
			User user = (User)searchById(inAccount);
			return user; 
		}
	}


	protected GroupSearcher getGroupSearcher()
	{
		return (GroupSearcher)getSearcherManager().getSearcher(getCatalogId(), "group");
	}
	/**
	 * @deprecate use standard field search API
	 */
	public User getUserByEmail(String inEmail)
	{
		User target = null;
		inEmail = inEmail.trim();
		Data record = (Data)query().or().startsWith("email", inEmail).startsWith("email", inEmail.toLowerCase()).searchOne();
		if(record != null){
			target = (User) loadData(record);
		}
		return target;
	}

	public HitTracker getUsersInGroup(Group inGroup)
	{
		SearchQuery query = createSearchQuery();
		if( inGroup == null)
		{
			throw new OpenEditException("No group found");
		}
		query.addExact("groups",inGroup.getId());
		//query.setSortBy("idsorted");
		HitTracker tracker = search(query);
		//log.info(tracker.size());
		return tracker;
	}

	public void saveUsers(List userstosave, User inUser) 
	{
		saveAllData(userstosave,inUser);
	}
	public void saveAllData(Collection<Data> inAll, User inUser)
	{
		for (Iterator iterator = inAll.iterator(); iterator.hasNext();) {
			User user = (User) iterator.next();
			if( user.getValue("creationdate") == null )
			{
				user.setValue("creationdate", new Date() );
			}
			getXmlUserArchive().saveUser(user);
			getCacheManager().remove("usercache",user.getId());
		}
		super.saveAllData(inAll, inUser);
	
	}

	public void saveData(Data inData, User inUser)
	{
		User tosave = (User)inData;
		if( tosave.getValue("creationdate") == null )
		{
			tosave.setValue("creationdate", new Date() );
		}
		getXmlUserArchive().saveUser(tosave);
		getCacheManager().remove("usercache",inData.getId());

		super.saveData(inData, inUser); //update the index
	}
	
	public void delete(Data inData, User inUser)
	{
		User user = null;
		if( inData instanceof User)
		{
			user = (User)inData;
		}
		else
		{
			user = (User) loadData(inData);
		}
		super.delete(user, inUser); //delete the index
		getXmlUserArchive().deleteUser(user);
		getCacheManager().remove("usercache",user.getId());

	}
	@Override
	protected void updateIndex(XContentBuilder inContent, Data inData, PropertyDetails inDetails,User inUser)
	{
		super.updateIndex(inContent, inData, inDetails,inUser);
		User user = null;
		if(!(inData instanceof User)){
			user = (User) loadData(inData);
		}
		try
		{
		//	inContent.field("enabled", user.isEnabled() ); //this causes mapping problem... will probably be in here twice.
			if(user != null &&  user.getGroups().size() > 0)
			{
				String[] groups = new String[user.getGroups().size()];
				int i = 0;
				for (Iterator iterator = user.getGroups().iterator(); iterator.hasNext();)
				{
					Group group = (Group) iterator.next();
					groups[i++] = group.getId();
					inContent.array("group", groups);
				}
			}
		}
		catch ( Exception ex)
		{
			throw new OpenEditException(ex);	
		}
	}
	
	public Data loadData(Data inHit)
	{
		if( inHit == null)
		{
			return null;
		}
		User user = null;
		if ( inHit instanceof User)
		{
			user = (User)inHit;
		}
		else
		{
			user = (User)createNewData();
			user.setProperties(inHit.getProperties());
			user.setId(inHit.getId());
			

		}
		if( user.getPassword() == null)
		{
			//Old indexes did not contain the password
			user = getXmlUserArchive().loadUser(user, getGroupSearcher());
			if(user != null &&  user.getPassword() != null){
				saveToElasticSearch(getPropertyDetails(), user, false,user);
			} else{
				if(user != null) {
				log.info("User " + user.getId() + " Had no password.  Please set one.");
				}
			}
				
			
		}
		
		
		return user;
	}

	@Override
	public boolean initialize()
	{
		if( !tableExists() || getAllHits().isEmpty())
		{
			reIndexAll();
			return true;
		}				
		return false;
	}


	@Override
	public StringEncryption getStringEncryption()
	{
		return getXmlUserArchive().getStringEncryption();
	}


	@Override
	public String encryptPassword(User inUser)
	{
		return getXmlUserArchive().encryptPassword(inUser);
	}


	@Override
	public String decryptPassword(User inUser)
	{
		return getXmlUserArchive().decryptPassword(inUser);
	}

}
