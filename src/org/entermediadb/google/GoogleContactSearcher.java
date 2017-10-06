package org.entermediadb.google;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.BaseSearcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.ListHitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.users.User;

public class GoogleContactSearcher extends BaseSearcher{
	
	
	private static final Log log = LogFactory.getLog(GoogleContactSearcher.class);


	
	
	
	
	public GoogleManager getGoogleManager()
	{
	
		return (GoogleManager) getModuleManager().getBean(getCatalogId(), "googleManager");
	}

	

	@Override
	public void reIndexAll() throws OpenEditException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public SearchQuery createSearchQuery()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HitTracker search(SearchQuery inQuery)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getIndexId()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void clearIndex()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteAll(User inUser)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delete(Data inData, User inUser)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void saveAllData(Collection<Data> inAll, User inUser)
	{
		// TODO Auto-generated method stub
		
	}
	
	
	@Override
	public HitTracker getAllHits(WebPageRequest inReq)
	{
		ArrayList contacts = getGoogleManager().syncContacts(inReq.getUser());
		
		return new ListHitTracker(contacts);
				
	}

	
//	@Override
//	public HitTracker fieldSearch(WebPageRequest inReq) throws OpenEditException
//	{
//	ArrayList contacts = getGoogleManager().listContacts(inReq.getUser())	
//	}
//	
	@Override
	public HitTracker fieldSearch(WebPageRequest inReq) throws OpenEditException
	{
	   try
	{
		   String query = inReq.findValue("name.value");
		   log.info("Search Google Contacts for " + query);
		ArrayList contacts = getGoogleManager().listContacts(inReq.getUser(), query);
		return new ListHitTracker(contacts);
	}
	catch (Exception e)
	{
		throw new OpenEditException(e);
	}
	}
}
