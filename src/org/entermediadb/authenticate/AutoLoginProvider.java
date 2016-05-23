package org.entermediadb.authenticate;

import org.openedit.WebPageRequest;

public interface AutoLoginProvider
{

	public AutoLoginResult autoLogin(WebPageRequest inReq);
	
}
