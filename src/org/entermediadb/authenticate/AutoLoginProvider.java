package org.entermediadb.authenticate;

import org.openedit.WebPageRequest;

public interface AutoLoginProvider
{
	public String ENTERMEDIAKEY = "entermedia.key";

	public AutoLoginResult autoLogin(WebPageRequest inReq);
	
}
