package org.entermediadb.authenticate;

import org.openedit.data.SearcherManager;
import org.openedit.users.Authenticator;
import org.openedit.users.User;
import org.openedit.users.UserManagerException;
import org.openedit.users.authenticate.AuthenticationRequest;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;

public class GoogleTwoFactorAuthenticator implements Authenticator{

	protected SearcherManager fieldSearcherManager;
	
	
	
	
	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager fieldSearcherManager)
	{
		this.fieldSearcherManager = fieldSearcherManager;
	}

	@Override
	public boolean authenticate(String inCatalogId, User inUser, String inPassword) throws UserManagerException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean authenticate(AuthenticationRequest inReq) throws UserManagerException {
		
		GoogleAuthenticator gAuth = new GoogleAuthenticator();
		String keystring =  inReq.get("googlekeystring");
		User user = inReq.getUser();

		String secret = user.get("googlesecretkey");
				
		if(user.get("googlesecretkey") == null) {
		
			return false;
			
		}
		if(keystring == null) {
			return false;
		}
	
		//https://github.com/wstrange/GoogleAuth
		Integer intval;
		try
		{
			intval = Integer.valueOf(keystring);
		}
		catch (NumberFormatException e)
		{
			return false;
		}
		boolean isCodeValid = gAuth.authorize(secret, intval);
		return isCodeValid;
		
	}

	
	
	
	
	
}
