package org.openedit.entermedia.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.data.BaseSearcher;
import org.openedit.xml.XmlArchive;
import org.openedit.xml.XmlFile;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.hittracker.Term;
import com.openedit.users.User;
import com.openedit.util.LDAP;

public class LdapSearcher extends BaseSearcher 
{
	protected Log log = LogFactory.getLog(getClass()); 
	protected XmlArchive fieldXmlArchive;
	protected String fieldDomain;
	protected Map fieldServers;
	
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

	public void saveData(Object inData, User inUser)
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
