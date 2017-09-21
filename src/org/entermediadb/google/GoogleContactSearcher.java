package org.entermediadb.google;

import java.util.Collection;

import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.common.OAuthProviderType;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.data.BaseSearcher;
import org.openedit.entermedia.util.EmTokenResponse;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.users.User;

public class GoogleContactSearcher extends BaseSearcher
{

	
	protected GoogleManager fieldGoogleManager;
	
	
	

	
	
	
	
	public GoogleManager getGoogleManager()
	{
		return fieldGoogleManager;
	}

	public void setGoogleManager(GoogleManager inGoogleManager)
	{
		fieldGoogleManager = inGoogleManager;
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

}
