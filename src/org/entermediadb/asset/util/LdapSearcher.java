package org.entermediadb.asset.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.BaseSearcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.hittracker.Term;
import org.openedit.users.User;
import org.openedit.users.UserManager;
import org.openedit.util.LDAP;
import org.openedit.xml.XmlArchive;
import org.openedit.xml.XmlFile;

public class LdapSearcher extends BaseSearcher 
{
	protected Log log = LogFactory.getLog(getClass()); 
	protected XmlArchive fieldXmlArchive;
	protected String fieldDomain;
	protected Map fieldServers;
	protected UserManager fieldUserManager;
	
	
	public UserManager getUserManager()
	{
		return fieldUserManager;
	}

	public void setUserManager(UserManager inUserManager)
	{
		fieldUserManager = inUserManager;
	}

	protected Map getServers()
	{
		if (fieldServers == null)
		{
			fieldServers = new HashMap();
		}

		return fieldServers;
	}
	
	protected LDAP getServer()
	{
		LDAP server = (LDAP)getServers().get("default"); //TODO: Support named config
		if (server == null)
		{
			server = new LDAP();
			XmlFile ldapconfig = getXmlArchive().getXml("/" + getCatalogId() + "/configuration/ldap.xml");
			server.setDomain(ldapconfig.get("domain"));
			server.setMaxLdapResults(Integer.parseInt(ldapconfig.get("maxldapresults")));
			server.setServerName(ldapconfig.get("servername"));
			
			String username = ldapconfig.get("username");
			if( username != null)
			{
				User user = getUserManager().getUser(username);
				String password = getUserManager().decryptPassword(user);
				server.authenticate(user,password);
				if( !server.connect() )
				{
					throw new OpenEditException("Could not connect as user " + username);
				}
			}			
			
			getServers().put("default", server);
		}
		return server;
	}
	
	public String getDomain()
	{
		return fieldDomain;
	}
	

	public void clearIndex()
	{
		// TODO Auto-generated method stub
		
	}

	public SearchQuery createSearchQuery()
	{
		SearchQuery query = new SearchQuery();
		return query;
	}

	public void delete(Data inData, User inUser)
	{
		// TODO Auto-generated method stub
		
	}

	public HitTracker getAllHits(WebPageRequest inReq)
	{
		// TODO Auto-generated method stub
		return null;
	}

	public String getIndexId()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public void reIndexAll() throws OpenEditException
	{
		// TODO Auto-generated method stub
		
	}

	public void saveAllData(Collection inAll, User inUser)
	{
		// TODO Auto-generated method stub
		
	}

	public void saveData(Data inData, User inUser)
	{
		// TODO Auto-generated method stub
		
	}

	public HitTracker search(SearchQuery inQuery)
	{
		LDAP ldap = getServer();
		
		Term term = (Term)inQuery.getTerms().get(0);
		String value = term.getValue();
		try
		{
			HitTracker results = ldap.search(term.getDetail().getId(), "substring", value); //TODO: lookup operation
			return results;
		}
		catch(Exception e)
		{
			log.error("Could not run LDAP search", e);
		}

		
		return null;
	}

	public XmlArchive getXmlArchive()
	{
		return fieldXmlArchive;
	}

	public void setXmlArchive(XmlArchive inXmlArchive)
	{
		fieldXmlArchive = inXmlArchive;
	}

	public void deleteAll(User inUser)
	{
		// TODO Auto-generated method stub
		
	}
}
