/*
 * Created on Oct 19, 2004
 */
package org.entermediadb.elasticsearch.searchers;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.profile.UserProfile;
import org.openedit.users.User;
import org.openedit.users.UserManager;

/**
 *
 */
public class ElasticUserProfileSearcher extends BaseElasticSearcher
{
	private static final Log log = LogFactory.getLog(ElasticUserProfileSearcher.class);
	protected UserManager fieldUserManager;
	
	@Override
	public Data createNewData()
	{
		UserProfile user = (UserProfile)super.createNewData();
		return user;
	}

	public void saveAllData(Collection<Data> inAll, User inUser)
	{
//		for (Iterator iterator = inAll.iterator(); iterator.hasNext();) {
//			UserProfile user = (UserProfile) iterator.next();
//			//getUserManager().saveUser(user);
//		}
		super.saveAllData(inAll, inUser);
	
	}

	public void saveData(Data inData, User inUser)
	{
//		User tosave = (User)inData;
//		if(tosave instanceof UserProfile)
//		{
//			tosave = ((UserProfile)tosave).getUser();
//		}
//		
//		if( tosave.getValue("creationdate") == null )
//		{
//			tosave.setValue("creationdate", new Date() );
//		}
//		getXmlUserArchive().saveUser(tosave);
//		getCacheManager().remove("usercache",tosave.getId());
//
		super.saveData(inData, inUser); //update the index
	}
	
}
